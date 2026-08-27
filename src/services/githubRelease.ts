/**
 * Netly Dynamic GitHub Releases Service
 * 
 * Automatically fetches, detects, and caches the latest stable Android APK release
 * from https://github.com/sumitkumartiwari19-sudo/Netly
 */

export interface GitHubAsset {
  id: number;
  name: string;
  size: number;
  content_type: string;
  browser_download_url: string;
  download_count?: number;
}

export interface GitHubRelease {
  id: number;
  tag_name: string;
  name: string;
  draft: boolean;
  prerelease: boolean;
  published_at: string;
  html_url: string;
  body: string;
  assets: GitHubAsset[];
}

export interface LatestReleaseState {
  isLoading: boolean;
  isError: boolean;
  errorMessage?: string;
  version: string;
  releaseName: string;
  releaseDate: string;
  formattedReleaseDate: string;
  apkDownloadUrl: string;
  apkFileName: string;
  apkSize: string;
  releaseNotes: string;
  releaseUrl: string;
  repoUrl: string;
  allReleasesUrl: string;
}

const REPO_OWNER = 'sumitkumartiwari19-sudo';
const REPO_NAME = 'Netly';
export const GITHUB_REPO_URL = `https://github.com/${REPO_OWNER}/${REPO_NAME}`;
export const GITHUB_RELEASES_URL = `https://github.com/${REPO_OWNER}/${REPO_NAME}/releases`;
const GITHUB_API_LATEST = `https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/releases/latest`;
const GITHUB_API_ALL = `https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/releases?per_page=5`;

// In-memory cache for session
let cachedReleaseState: LatestReleaseState | null = null;

/**
 * Filter and select the most appropriate Android APK asset from release assets.
 * Excludes non-APK files (.aab, .zip, .sha256, mapping, debug artifacts).
 */
export function getLatestApkAsset(assets: GitHubAsset[]): GitHubAsset | null {
  if (!Array.isArray(assets) || assets.length === 0) {
    return null;
  }

  // Filter valid .apk assets
  const apkAssets = assets.filter((asset) => {
    const name = (asset.name || '').trim().toLowerCase();
    
    // Must end with .apk
    if (!name.endsWith('.apk')) return false;

    // Exclude checksums, signatures, debug or mapping files
    if (
      name.endsWith('.sha256') ||
      name.endsWith('.md5') ||
      name.endsWith('.asc') ||
      name.includes('-debug') ||
      name.includes('.debug.') ||
      name.includes('mapping')
    ) {
      return false;
    }

    return Boolean(asset.browser_download_url);
  });

  if (apkAssets.length === 0) {
    return null;
  }

  // Selection priority:
  // 1. Universal release APK
  const universalApk = apkAssets.find((a) => a.name.toLowerCase().includes('universal'));
  if (universalApk) return universalApk;

  // 2. Named release APK or official app name (e.g. Netly.apk or Netly-release.apk)
  const releaseApk = apkAssets.find((a) => {
    const n = a.name.toLowerCase();
    return n.includes('release') || n === 'netly.apk' || n.startsWith('netly-v') || n.startsWith('netly_v');
  });
  if (releaseApk) return releaseApk;

  // 3. First valid APK
  return apkAssets[0];
}

/**
 * Format bytes to readable string (e.g., 27.8 MB)
 */
export function formatFileSize(bytes: number): string {
  if (!bytes || isNaN(bytes) || bytes <= 0) {
    return '18 MB';
  }
  const mb = bytes / (1024 * 1024);
  return `${mb.toFixed(1)} MB`;
}

/**
 * Format ISO date to user-friendly string (e.g. Aug 17, 2026)
 */
export function formatReleaseDate(isoDateString?: string): string {
  if (!isoDateString) return '';
  try {
    const date = new Date(isoDateString);
    if (isNaN(date.getTime())) return '';
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  } catch {
    return '';
  }
}

/**
 * Sanitize and clean release notes safely for display
 */
export function sanitizeReleaseNotes(notes?: string): string[] {
  if (!notes) return [];
  
  // Extract bullet points or lines
  const lines = notes
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line.length > 0);

  const cleanBullets: string[] = [];

  for (const line of lines) {
    // Skip large markdown headers or disclaimers if too verbose, but keep highlights
    if (line.startsWith('#') || line.startsWith('===')) {
      continue;
    }
    
    // Clean bullet symbols like "- ", "* ", "• "
    let clean = line.replace(/^[-*•]\s+/, '').trim();
    // Remove markdown bold/italics
    clean = clean.replace(/\*\*(.*?)\*\*/g, '$1');
    clean = clean.replace(/\*(.*?)\*/g, '$1');
    clean = clean.replace(/__(.*?)__/g, '$1');
    clean = clean.replace(/_(.*?)_/g, '$1');

    if (clean.length > 0 && cleanBullets.length < 8) {
      cleanBullets.push(clean);
    }
  }

  return cleanBullets;
}

/**
 * Fetch latest stable release from GitHub API with fallback and caching.
 */
export async function fetchLatestRelease(): Promise<LatestReleaseState> {
  // Return cached result if already fetched
  if (cachedReleaseState && !cachedReleaseState.isError) {
    return cachedReleaseState;
  }

  const defaultFallback: LatestReleaseState = {
    isLoading: false,
    isError: false,
    version: 'v1.0',
    releaseName: 'Netly Latest',
    releaseDate: '',
    formattedReleaseDate: '',
    apkDownloadUrl: GITHUB_RELEASES_URL,
    apkFileName: 'Netly.apk',
    apkSize: '18 MB',
    releaseNotes: '',
    releaseUrl: GITHUB_RELEASES_URL,
    repoUrl: GITHUB_REPO_URL,
    allReleasesUrl: GITHUB_RELEASES_URL,
  };

  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 7000);

    // 1. First attempt /releases/latest endpoint
    const response = await fetch(GITHUB_API_LATEST, {
      headers: {
        Accept: 'application/vnd.github.v3+json',
      },
      signal: controller.signal,
    });
    clearTimeout(timeoutId);

    if (!response.ok) {
      // If 404 or rate limited, try querying /releases list as fallback
      return await fetchFromReleasesListFallback(defaultFallback);
    }

    const releaseData: GitHubRelease = await response.json();

    // Ensure it's not a draft or prerelease
    if (releaseData.draft || releaseData.prerelease) {
      return await fetchFromReleasesListFallback(defaultFallback);
    }

    const apkAsset = getLatestApkAsset(releaseData.assets);

    if (!apkAsset) {
      // Release found but no APK asset found in latest, try all releases
      return await fetchFromReleasesListFallback(defaultFallback);
    }

    const state: LatestReleaseState = {
      isLoading: false,
      isError: false,
      version: releaseData.tag_name || 'v1.0',
      releaseName: releaseData.name || `Netly ${releaseData.tag_name || ''}`,
      releaseDate: releaseData.published_at || '',
      formattedReleaseDate: formatReleaseDate(releaseData.published_at),
      apkDownloadUrl: apkAsset.browser_download_url,
      apkFileName: apkAsset.name || 'Netly.apk',
      apkSize: formatFileSize(apkAsset.size),
      releaseNotes: releaseData.body || '',
      releaseUrl: releaseData.html_url || GITHUB_RELEASES_URL,
      repoUrl: GITHUB_REPO_URL,
      allReleasesUrl: GITHUB_RELEASES_URL,
    };

    cachedReleaseState = state;
    return state;
  } catch (err) {
    console.warn('Could not fetch latest release from GitHub API:', err);
    return {
      ...defaultFallback,
      isError: true,
      errorMessage: 'Latest version info unavailable',
      apkDownloadUrl: GITHUB_RELEASES_URL,
    };
  }
}

/**
 * Fallback to /releases list if /releases/latest is not available or returned a prerelease.
 */
async function fetchFromReleasesListFallback(fallback: LatestReleaseState): Promise<LatestReleaseState> {
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 6000);

    const res = await fetch(GITHUB_API_ALL, {
      headers: {
        Accept: 'application/vnd.github.v3+json',
      },
      signal: controller.signal,
    });
    clearTimeout(timeoutId);

    if (!res.ok) {
      return {
        ...fallback,
        isError: true,
        errorMessage: 'Latest version unavailable',
      };
    }

    const releases: GitHubRelease[] = await res.json();
    if (!Array.isArray(releases) || releases.length === 0) {
      return {
        ...fallback,
        isError: true,
        errorMessage: 'No published releases found',
      };
    }

    // Find the first stable release with an APK
    for (const rel of releases) {
      if (rel.draft || rel.prerelease) continue;
      const asset = getLatestApkAsset(rel.assets);
      if (asset) {
        const state: LatestReleaseState = {
          isLoading: false,
          isError: false,
          version: rel.tag_name || 'v1.0',
          releaseName: rel.name || `Netly ${rel.tag_name || ''}`,
          releaseDate: rel.published_at || '',
          formattedReleaseDate: formatReleaseDate(rel.published_at),
          apkDownloadUrl: asset.browser_download_url,
          apkFileName: asset.name || 'Netly.apk',
          apkSize: formatFileSize(asset.size),
          releaseNotes: rel.body || '',
          releaseUrl: rel.html_url || GITHUB_RELEASES_URL,
          repoUrl: GITHUB_REPO_URL,
          allReleasesUrl: GITHUB_RELEASES_URL,
        };
        cachedReleaseState = state;
        return state;
      }
    }

    return {
      ...fallback,
      isError: true,
      errorMessage: 'No APK found in releases',
    };
  } catch {
    return {
      ...fallback,
      isError: true,
      errorMessage: 'Latest version unavailable',
    };
  }
}
