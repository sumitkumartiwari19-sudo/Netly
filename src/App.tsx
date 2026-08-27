/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect, useRef } from 'react';
import {
  Download,
  Smartphone,
  Zap,
  ShieldCheck,
  Film,
  Layers,
  Lock,
  Globe,
  Copy,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  Star,
  Play,
  Menu,
  X,
  ArrowRight,
  Sparkles,
  Scissors,
  Check,
  Search,
  HardDrive,
  QrCode,
  Info,
  ChevronLeft,
  ChevronRight,
  Music,
  Share2,
  SlidersHorizontal,
  ExternalLink,
  Shield,
  Calendar,
  Tag,
  AlertCircle,
  Github
} from 'lucide-react';
import {
  fetchLatestRelease,
  sanitizeReleaseNotes,
  GITHUB_REPO_URL,
  GITHUB_RELEASES_URL,
  type LatestReleaseState
} from './services/githubRelease';

export default function App() {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [activeFaq, setActiveFaq] = useState<number | null>(0);
  const [isDownloadModalOpen, setIsDownloadModalOpen] = useState(false);
  const [downloadProgress, setDownloadProgress] = useState(0);
  const [isDownloadingState, setIsDownloadingState] = useState(false);

  // Dynamic GitHub Release State
  const [releaseState, setReleaseState] = useState<LatestReleaseState>({
    isLoading: true,
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
  });
  
  // Interactive Phone Simulator State
  const [sampleUrl, setSampleUrl] = useState('https://instagram.com/reel/C8x9_NetlyPro');
  const [selectedQuality, setSelectedQuality] = useState('1080p HD');
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [isAnalyzed, setIsAnalyzed] = useState(true);
  const [copiedToast, setCopiedToast] = useState('');

  // Screenshot Carousel Scroll Ref
  const carouselRef = useRef<HTMLDivElement>(null);

  // Fetch GitHub release once on mount
  useEffect(() => {
    let isMounted = true;
    fetchLatestRelease().then((data) => {
      if (isMounted) {
        setReleaseState({
          ...data,
          isLoading: false,
        });
      }
    });
    return () => {
      isMounted = false;
    };
  }, []);

  const scrollCarousel = (direction: 'left' | 'right') => {
    if (carouselRef.current) {
      const scrollAmount = direction === 'left' ? -320 : 320;
      carouselRef.current.scrollBy({ left: scrollAmount, behavior: 'smooth' });
    }
  };

  /**
   * Opens the download modal popup without directly navigating or downloading.
   * The user initiates the actual APK download using the button inside the popup.
   */
  const handleOpenDownloadModal = (e?: React.MouseEvent) => {
    if (e) {
      e.preventDefault();
    }
    setIsDownloadModalOpen(true);
    setIsDownloadingState(true);
    setDownloadProgress(0);
  };

  useEffect(() => {
    let interval: NodeJS.Timeout;
    if (isDownloadModalOpen && isDownloadingState) {
      interval = setInterval(() => {
        setDownloadProgress((prev) => {
          if (prev >= 100) {
            clearInterval(interval);
            setIsDownloadingState(false);
            return 100;
          }
          return prev + 10;
        });
      }, 100);
    }
    return () => clearInterval(interval);
  }, [isDownloadModalOpen, isDownloadingState]);

  const handleAnalyzeUrl = (urlToSet?: string) => {
    if (urlToSet) setSampleUrl(urlToSet);
    setIsAnalyzing(true);
    setIsAnalyzed(false);
    setTimeout(() => {
      setIsAnalyzing(false);
      setIsAnalyzed(true);
    }, 800);
  };

  const copyToClipboard = (text: string, label: string) => {
    navigator.clipboard.writeText(text);
    setCopiedToast(`Copied ${label}!`);
    setTimeout(() => setCopiedToast(''), 2500);
  };

  const cleanNotes = sanitizeReleaseNotes(releaseState.releaseNotes);

  return (
    <div className="min-h-screen bg-[#eef1f6] text-[#2d3748] font-inter relative overflow-x-hidden selection:bg-[#6c63ff] selection:text-white">
      {/* Toast Notification */}
      {copiedToast && (
        <div className="fixed top-6 right-6 z-[100] neu-flat-sm bg-[#eef1f6] px-5 py-3 rounded-2xl flex items-center gap-3 border border-white/60 shadow-xl animate-bounce">
          <CheckCircle2 className="w-5 h-5 text-[#6c63ff]" />
          <span className="text-sm font-semibold text-[#2d3748] font-poppins">{copiedToast}</span>
        </div>
      )}

      {/* NAVBAR */}
      <header className="sticky top-0 z-50 bg-[#eef1f6]/90 backdrop-blur-md transition-all duration-300 py-4 px-4 sm:px-8">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          {/* Logo */}
          <a href="#" aria-label="Netly Downloader" className="flex items-center gap-3 group">
            <div className="w-11 h-11 neu-interactive rounded-2xl flex items-center justify-center text-[#6c63ff] font-bold text-xl group-hover:scale-105 transition-transform">
              <div className="w-7 h-7 rounded-xl bg-gradient-to-tr from-[#6c63ff] to-[#a78bfa] flex items-center justify-center text-white shadow-md">
                <Download className="w-4 h-4 stroke-[2.5]" />
              </div>
            </div>
            <span className="text-2xl font-extrabold font-poppins tracking-tight text-[#2d3748]">
              Net<span className="text-gradient">ly</span>
            </span>
          </a>

          {/* Desktop Nav Links */}
          <nav aria-label="Main Navigation" className="hidden md:flex items-center gap-1 neu-pressed px-4 py-2 rounded-full">
            {[
              { name: 'Features', href: '#features' },
              { name: 'How it works', href: '#how-it-works' },
              { name: 'Screenshots', href: '#screenshots' },
              { name: 'Reviews', href: '#reviews' },
              { name: 'FAQ', href: '#faq' },
            ].map((link) => (
              <a
                key={link.name}
                href={link.href}
                className="px-4 py-2 text-sm font-semibold text-[#5a6578] hover:text-[#6c63ff] transition-colors rounded-full hover:bg-white/40"
              >
                {link.name}
              </a>
            ))}
          </nav>

          {/* Navbar Right Actions */}
          <div className="hidden sm:flex items-center gap-3">
            {/* GitHub Repo Button */}
            <a
              href={GITHUB_REPO_URL}
              target="_blank"
              rel="noopener noreferrer"
              className="neu-interactive px-4 py-3 rounded-2xl text-[#2d3748] hover:text-[#6c63ff] font-poppins font-semibold text-sm flex items-center gap-2 transition-colors"
              title="View GitHub Repository"
            >
              <Github className="w-4 h-4" />
              <span>GitHub</span>
            </a>

            {/* Dynamic Download Button */}
            <button
              onClick={handleOpenDownloadModal}
              className="neu-accent-btn px-6 py-3 rounded-2xl text-white font-poppins font-semibold text-sm flex items-center gap-2 cursor-pointer"
            >
              <Download className="w-4 h-4" />
              <span>
                {releaseState.isLoading
                  ? 'Loading latest version...'
                  : `Download APK (${releaseState.version})`}
              </span>
            </button>
          </div>

          {/* Mobile Hamburger Button */}
          <button
            onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
            className="md:hidden w-11 h-11 neu-interactive rounded-2xl flex items-center justify-center text-[#2d3748]"
            aria-label="Toggle menu"
          >
            {isMobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
          </button>
        </div>

        {/* Mobile Menu Dropdown */}
        {isMobileMenuOpen && (
          <div className="md:hidden pt-4 pb-6 px-4">
            <nav aria-label="Mobile Navigation" className="neu-flat p-6 rounded-3xl flex flex-col gap-4">
              {[
                { name: 'Features', href: '#features' },
                { name: 'How it works', href: '#how-it-works' },
                { name: 'Screenshots', href: '#screenshots' },
                { name: 'Reviews', href: '#reviews' },
                { name: 'FAQ', href: '#faq' },
              ].map((link) => (
                <a
                  key={link.name}
                  href={link.href}
                  onClick={() => setIsMobileMenuOpen(false)}
                  className="px-4 py-3 text-base font-semibold text-[#2d3748] hover:text-[#6c63ff] neu-pressed rounded-2xl flex items-center justify-between"
                >
                  <span>{link.name}</span>
                  <ArrowRight className="w-4 h-4 text-[#6c63ff]" />
                </a>
              ))}
              
              <a
                href={GITHUB_REPO_URL}
                target="_blank"
                rel="noopener noreferrer"
                className="px-4 py-3 text-base font-semibold text-[#2d3748] hover:text-[#6c63ff] neu-pressed rounded-2xl flex items-center justify-between"
              >
                <div className="flex items-center gap-2">
                  <Github className="w-4 h-4" />
                  <span>GitHub Repository</span>
                </div>
                <ExternalLink className="w-4 h-4 text-[#6c63ff]" />
              </a>

              <button
                onClick={() => {
                  setIsMobileMenuOpen(false);
                  handleOpenDownloadModal();
                }}
                className="w-full neu-accent-btn py-4 rounded-2xl text-white font-poppins font-bold text-center flex items-center justify-center gap-2 mt-2"
              >
                <Download className="w-5 h-5" />
                <span>
                  {releaseState.isLoading
                    ? 'Loading latest version...'
                    : `Download Latest APK (${releaseState.version})`}
                </span>
              </button>
            </nav>
          </div>
        )}
      </header>

      {/* MAIN CONTENT */}
      <main id="main-content">
        {/* HERO SECTION */}
        <section aria-labelledby="hero-title" className="pt-8 pb-16 md:py-20 px-4 sm:px-8">
        <div className="max-w-7xl mx-auto grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-8 items-center">
          {/* Left Text Content */}
          <div className="lg:col-span-7 flex flex-col items-start space-y-6">
            {/* Eyebrow Tag */}
            <div className="neu-flat-sm px-5 py-2.5 rounded-full flex items-center gap-2.5 text-xs sm:text-sm font-semibold text-[#6c63ff] font-poppins">
              <span className="w-2.5 h-2.5 rounded-full bg-[#6c63ff] animate-ping inline-block" />
              <Sparkles className="w-4 h-4 text-[#6c63ff]" />
              <span>100% Free · No Watermark</span>
            </div>

            {/* Headline */}
            <h1 id="hero-title" className="text-3xl sm:text-5xl lg:text-6xl font-extrabold font-poppins text-[#1a202c] leading-[1.15] tracking-tight">
              Download videos from{' '}
              <span className="text-gradient underline decoration-[#a78bfa]/30 decoration-wavy">anywhere</span>, in one tap
            </h1>

            {/* Subtext */}
            <p className="text-base sm:text-lg text-[#5a6578] leading-relaxed max-w-2xl font-normal">
              Save HD & 4K videos, reels, and MP3 audio directly from TikTok, Instagram, YouTube, and 1000+ social platforms directly to your Android device — fast, safe, and watermark-free.
            </p>

            {/* CTA Buttons */}
            <div className="pt-2 flex flex-col sm:flex-row items-stretch sm:items-center gap-4 w-full sm:w-auto">
              <button
                onClick={handleOpenDownloadModal}
                className="neu-accent-btn px-8 py-4 rounded-2xl text-white font-poppins font-bold text-base flex items-center justify-center gap-3 cursor-pointer group"
              >
                <Smartphone className="w-5 h-5 group-hover:scale-110 transition-transform" />
                <span>
                  {releaseState.isLoading
                    ? 'Loading latest version...'
                    : 'Download Latest APK'}
                </span>
                <ArrowRight className="w-5 h-5 opacity-80 group-hover:translate-x-1 transition-transform" />
              </button>

              <a
                href="#how-it-works"
                className="neu-interactive px-7 py-4 rounded-2xl text-[#2d3748] font-poppins font-semibold text-base flex items-center justify-center gap-2"
              >
                <span>Learn more</span>
              </a>
            </div>

            {/* Micro Specs Banner */}
            <div className="pt-4 flex flex-wrap items-center gap-6 text-xs sm:text-sm text-[#64748b]">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-[#6c63ff]" />
                <span>Supports 1000+ Sites</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-[#6c63ff]" />
                <span>No Registration Required</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-[#6c63ff]" />
                <span>
                  {releaseState.isLoading ? (
                    'Checking version...'
                  ) : (
                    `Latest Version · ${releaseState.version} (${releaseState.apkSize})`
                  )}
                </span>
              </div>
            </div>
          </div>

          {/* Right Phone Mockup Illustration */}
          <div className="lg:col-span-5 flex justify-center lg:justify-end">
            <div className="w-full max-w-[340px] sm:max-w-[380px] neu-flat-lg p-5 rounded-[40px] relative border border-white/60">
              {/* Phone Frame Mockup */}
              <div className="bg-[#1e2330] rounded-[32px] p-4 text-white shadow-inner relative overflow-hidden border border-gray-700/50">
                {/* Notch / Speaker */}
                <div className="w-32 h-5 bg-black/60 mx-auto rounded-b-xl flex items-center justify-center gap-2 mb-3">
                  <div className="w-3 h-3 rounded-full bg-gray-800 border border-gray-700" />
                  <div className="w-10 h-1.5 rounded-full bg-gray-800" />
                </div>

                {/* Simulated Phone Top Bar */}
                <div className="flex items-center justify-between px-2 mb-4 text-[11px] text-gray-400 font-medium">
                  <span>09:41</span>
                  <div className="flex items-center gap-1.5">
                    <span className="text-[10px] bg-[#6c63ff]/30 text-[#a78bfa] px-1.5 py-0.5 rounded font-bold">5G</span>
                    <div className="w-4 h-2 border border-gray-400 rounded-sm p-0.5 flex items-center">
                      <div className="w-full h-full bg-[#6c63ff] rounded-xs" />
                    </div>
                  </div>
                </div>

                {/* Netly Phone App UI Mockup */}
                <div className="space-y-4">
                  {/* Phone Header Logo */}
                  <div className="flex items-center justify-between bg-gray-800/60 p-3 rounded-2xl border border-gray-700/40">
                    <div className="flex items-center gap-2">
                      <div className="w-7 h-7 rounded-lg bg-gradient-to-tr from-[#6c63ff] to-[#a78bfa] flex items-center justify-center text-white">
                        <Download className="w-3.5 h-3.5" />
                      </div>
                      <span className="font-poppins font-bold text-sm tracking-wide">Netly Mobile</span>
                    </div>
                    <span className="text-[10px] text-emerald-400 font-semibold bg-emerald-950/80 px-2 py-0.5 rounded-full border border-emerald-500/30">
                      Ready
                    </span>
                  </div>

                  {/* URL Input Box inside phone */}
                  <div className="bg-gray-900/90 p-3.5 rounded-2xl border border-gray-700/60 space-y-2">
                    <label className="text-[10px] uppercase tracking-wider text-gray-400 font-bold block">
                      Video URL Auto-Detect
                    </label>
                    <div className="flex items-center gap-2 bg-gray-800/90 px-3 py-2 rounded-xl border border-gray-700">
                      <Search className="w-3.5 h-3.5 text-[#a78bfa] shrink-0" />
                      <input
                        type="text"
                        value={sampleUrl}
                        onChange={(e) => setSampleUrl(e.target.value)}
                        className="bg-transparent text-xs text-gray-200 outline-none w-full font-mono truncate"
                      />
                    </div>
                    <div className="flex items-center justify-between pt-1">
                      <div className="flex gap-1.5">
                        <button
                          onClick={() => handleAnalyzeUrl('https://tiktok.com/@creator/video/991')}
                          className="text-[10px] bg-gray-800 hover:bg-gray-700 px-2 py-1 rounded-lg text-gray-300 transition-colors"
                        >
                          TikTok
                        </button>
                        <button
                          onClick={() => handleAnalyzeUrl('https://instagram.com/reel/C8x9')}
                          className="text-[10px] bg-gray-800 hover:bg-gray-700 px-2 py-1 rounded-lg text-gray-300 transition-colors"
                        >
                          Insta
                        </button>
                        <button
                          onClick={() => handleAnalyzeUrl('https://youtube.com/watch?v=Netly')}
                          className="text-[10px] bg-gray-800 hover:bg-gray-700 px-2 py-1 rounded-lg text-gray-300 transition-colors"
                        >
                          YouTube
                        </button>
                      </div>
                      <button
                        onClick={() => handleAnalyzeUrl()}
                        className="text-xs bg-[#6c63ff] hover:bg-[#5b52f0] text-white font-bold px-3 py-1 rounded-lg flex items-center gap-1 transition-colors"
                      >
                        {isAnalyzing ? (
                          <span className="animate-spin text-[10px]">🌀</span>
                        ) : (
                          <span>Grab</span>
                        )}
                      </button>
                    </div>
                  </div>

                  {/* Video Preview Card inside phone */}
                  {isAnalyzed && (
                    <div className="bg-gray-900/90 p-3.5 rounded-2xl border border-gray-700/60 space-y-3 animate-fadeIn">
                      <div className="relative rounded-xl overflow-hidden bg-gradient-to-br from-indigo-950 to-slate-900 h-28 flex items-center justify-center border border-indigo-500/30">
                        {/* Play Button Glow */}
                        <div className="w-12 h-12 rounded-full bg-gradient-to-tr from-[#6c63ff] to-[#a78bfa] flex items-center justify-center text-white shadow-lg pulse-glow cursor-pointer">
                          <Play className="w-5 h-5 fill-white ml-0.5" />
                        </div>
                        <div className="absolute bottom-2 left-2 right-2 flex justify-between items-center bg-black/60 backdrop-blur-xs px-2 py-1 rounded-lg text-[10px]">
                          <span className="font-semibold text-gray-200 truncate max-w-[180px]">
                            Summer_Vacation_4K.mp4
                          </span>
                          <span className="text-[#a78bfa] font-mono">03:24</span>
                        </div>
                      </div>

                      {/* Quality Picker Pills */}
                      <div className="grid grid-cols-3 gap-1.5 text-[10px]">
                        {['4K 2160p', '1080p HD', 'MP3 Audio'].map((q) => (
                          <button
                            key={q}
                            onClick={() => setSelectedQuality(q)}
                            className={`py-1.5 px-2 rounded-lg font-semibold text-center transition-all ${
                              selectedQuality === q
                                ? 'bg-gradient-to-r from-[#6c63ff] to-[#a78bfa] text-white shadow-sm'
                                : 'bg-gray-800 text-gray-400 hover:text-gray-200'
                            }`}
                          >
                            {q}
                          </button>
                        ))}
                      </div>

                      {/* Download Progress Bar */}
                      <div className="bg-gray-800/90 p-2.5 rounded-xl space-y-1.5 border border-gray-700/40">
                        <div className="flex justify-between text-[10px]">
                          <span className="text-gray-300 font-medium">Downloading...</span>
                          <span className="text-[#a78bfa] font-bold font-mono">14.2 MB/s</span>
                        </div>
                        <div className="w-full h-2 bg-gray-700 rounded-full overflow-hidden">
                          <div className="h-full bg-gradient-to-r from-[#6c63ff] to-[#a78bfa] w-[88%] rounded-full animate-pulse" />
                        </div>
                      </div>
                    </div>
                  )}
                </div>

                {/* Bottom Navigation Indicator */}
                <div className="w-28 h-1 bg-gray-600 mx-auto rounded-full mt-4 opacity-60" />
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* FEATURES SECTION */}
      <section id="features" className="py-20 px-4 sm:px-8">
        <div className="max-w-7xl mx-auto space-y-12">
          {/* Section Header */}
          <div className="text-center max-w-3xl mx-auto space-y-4">
            <div className="inline-flex neu-flat-sm px-4 py-2 rounded-full text-xs font-bold uppercase tracking-wider text-[#6c63ff]">
              Why Choose Netly
            </div>
            <h2 className="text-3xl sm:text-4xl font-extrabold font-poppins text-[#1a202c]">
              Built for speed, simplicity & complete privacy
            </h2>
            <p className="text-[#5a6578] text-base sm:text-lg">
              Everything you need in a modern Android video downloader, crafted with soft tactile design.
            </p>
          </div>

          {/* Grid of 6 Neumorphic Feature Cards */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {[
              {
                icon: Globe,
                title: 'Multi-Platform Support',
                desc: 'Download videos seamlessly from TikTok, Instagram, YouTube, Twitter/X, Facebook, Pinterest, and 1000+ video sites.',
              },
              {
                icon: Film,
                title: 'HD & 4K Quality',
                desc: 'Save videos in 1080p Full HD, 2K, or ultra crisp 4K resolution. Extract high quality 320kbps MP3 audio with original fidelity.',
              },
              {
                icon: Zap,
                title: 'Multi-Thread Accelerator',
                desc: 'Engineered with custom hardware acceleration algorithms for up to 10x ultra-fast mobile download speeds.',
              },
              {
                icon: Scissors,
                title: 'No Watermark',
                desc: 'Enjoy clean, pure video files without intrusive TikTok or Instagram logo watermarks obscuring your content.',
              },
              {
                icon: Layers,
                title: 'Batch Downloads',
                desc: 'Queue up multiple links or download entire channel playlists at once with intelligent multi-tasking queue.',
              },
              {
                icon: Lock,
                title: 'Private & Secure Vault',
                desc: 'Built-in PIN/Biometric protected media folder. Zero tracking, no sign-up required, 100% private offline storage.',
              },
            ].map((feature, idx) => (
              <div
                key={idx}
                className="neu-interactive p-8 rounded-[24px] flex flex-col items-start gap-5 group"
              >
                <div className="w-14 h-14 neu-flat-sm rounded-2xl flex items-center justify-center text-[#6c63ff] group-hover:scale-110 transition-transform">
                  <feature.icon className="w-7 h-7 stroke-[2]" />
                </div>
                <h3 className="text-xl font-bold font-poppins text-[#1a202c]">
                  {feature.title}
                </h3>
                <p className="text-sm text-[#5a6578] leading-relaxed">
                  {feature.desc}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* HOW IT WORKS SECTION */}
      <section id="how-it-works" className="py-20 px-4 sm:px-8 bg-[#eef1f6]/60">
        <div className="max-w-7xl mx-auto space-y-16">
          {/* Section Header */}
          <div className="text-center max-w-2xl mx-auto space-y-4">
            <div className="inline-flex neu-flat-sm px-4 py-2 rounded-full text-xs font-bold uppercase tracking-wider text-[#6c63ff]">
              Simple 3-Step Process
            </div>
            <h2 className="text-3xl sm:text-4xl font-extrabold font-poppins text-[#1a202c]">
              How Netly works in 3 easy steps
            </h2>
            <p className="text-[#5a6578] text-base">
              No technical setup required. Copy, paste, and save any video in under 5 seconds.
            </p>
          </div>

          {/* 3 Step Cards Grid */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8 relative">
            {[
              {
                step: '01',
                title: 'Copy the Link',
                desc: 'Open TikTok, Instagram, or YouTube. Tap the "Share" button and click "Copy Link".',
                icon: Copy,
                sample: 'https://tiktok.com/@video/12345',
              },
              {
                step: '02',
                title: 'Paste in Netly',
                desc: 'Launch Netly on your Android phone. Our smart clipboard detector pastes your URL automatically.',
                icon: Smartphone,
                sample: 'Auto-Detected Link ✨',
              },
              {
                step: '03',
                title: 'Tap Download',
                desc: 'Select your preferred video resolution or MP3 format and tap Download to save straight to gallery.',
                icon: Download,
                sample: 'Saved to Gallery 4K',
              },
            ].map((item, index) => (
              <div
                key={index}
                className="neu-flat p-8 rounded-[28px] relative flex flex-col justify-between space-y-6"
              >
                {/* Step Number Badge */}
                <div className="flex items-center justify-between">
                  <span className="text-4xl font-extrabold font-poppins text-gradient opacity-80">
                    {item.step}
                  </span>
                  <div className="w-12 h-12 neu-pressed rounded-2xl flex items-center justify-center text-[#6c63ff]">
                    <item.icon className="w-6 h-6" />
                  </div>
                </div>

                <div className="space-y-3">
                  <h3 className="text-xl font-bold font-poppins text-[#1a202c]">
                    {item.title}
                  </h3>
                  <p className="text-sm text-[#5a6578] leading-relaxed">
                    {item.desc}
                  </p>
                </div>

                {/* Micro Action Box */}
                <div
                  onClick={() => copyToClipboard(item.sample, `Step ${item.step}`)}
                  className="neu-pressed px-4 py-3 rounded-2xl text-xs font-mono text-[#6c63ff] flex items-center justify-between cursor-pointer hover:bg-white/40 transition-colors"
                >
                  <span className="truncate">{item.sample}</span>
                  <Copy className="w-3.5 h-3.5 shrink-0 opacity-70" />
                </div>
              </div>
            ))}
          </div>

          {/* Interactive Live URL Simulator Banner */}
          <div className="neu-flat p-8 rounded-[32px] max-w-4xl mx-auto space-y-6">
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div>
                <h4 className="text-lg font-bold font-poppins text-[#1a202c]">
                  Test Netly Link Grabber Demo
                </h4>
                <p className="text-sm text-[#5a6578]">
                  Click any sample social media link below to see Netly instant detection in action:
                </p>
              </div>
              <span className="neu-flat-sm px-3 py-1.5 rounded-xl text-xs font-bold text-[#6c63ff] font-mono">
                Live Preview
              </span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              {[
                { name: 'TikTok Reel (No Watermark)', url: 'https://tiktok.com/@netly/video/8812' },
                { name: 'Instagram 4K Reel', url: 'https://instagram.com/reel/NetlyUltra' },
                { name: 'YouTube Music Video', url: 'https://youtube.com/watch?v=NetlyMP3' },
              ].map((btn, i) => (
                <button
                  key={i}
                  onClick={() => {
                    handleAnalyzeUrl(btn.url);
                    window.scrollTo({ top: 0, behavior: 'smooth' });
                  }}
                  className="neu-interactive px-4 py-3 rounded-2xl text-xs font-semibold text-[#2d3748] text-left flex items-center justify-between group"
                >
                  <span className="truncate">{btn.name}</span>
                  <ExternalLink className="w-3.5 h-3.5 text-[#6c63ff] group-hover:translate-x-0.5 transition-transform" />
                </button>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* SCREENSHOTS SECTION */}
      <section id="screenshots" className="py-20 px-4 sm:px-8">
        <div className="max-w-7xl mx-auto space-y-12">
          {/* Section Header with Controls */}
          <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
            <div className="space-y-3 max-w-2xl">
              <div className="inline-flex neu-flat-sm px-4 py-2 rounded-full text-xs font-bold uppercase tracking-wider text-[#6c63ff]">
                App Preview
              </div>
              <h2 className="text-3xl sm:text-4xl font-extrabold font-poppins text-[#1a202c]">
                Designed for seamless Android experience
              </h2>
              <p className="text-[#5a6578] text-base">
                Swipe through Netly's soft tactile UI interfaces carefully crafted for comfort and clarity.
              </p>
            </div>

            {/* Scroll Navigation Arrows */}
            <div className="flex items-center gap-3">
              <button
                onClick={() => scrollCarousel('left')}
                className="w-12 h-12 neu-interactive rounded-2xl flex items-center justify-center text-[#2d3748]"
                aria-label="Scroll left"
              >
                <ChevronLeft className="w-6 h-6" />
              </button>
              <button
                onClick={() => scrollCarousel('right')}
                className="w-12 h-12 neu-interactive rounded-2xl flex items-center justify-center text-[#2d3748]"
                aria-label="Scroll right"
              >
                <ChevronRight className="w-6 h-6" />
              </button>
            </div>
          </div>

          {/* Horizontal Scrollable Carousel of 5 Phone Screen Mockups */}
          <div
            ref={carouselRef}
            className="flex items-stretch gap-6 overflow-x-auto custom-scroll pb-8 pt-2 px-2"
          >
            {[
              {
                screen: 'Home Screen',
                subtitle: 'Trending feed & quick links',
                gradient: 'from-indigo-600 to-purple-600',
                badge: 'Dashboard',
                features: ['Instant Link Detector', '1000+ Supported Apps', 'Recent Activity'],
              },
              {
                screen: 'Paste Link',
                subtitle: 'Auto URL detection & info',
                gradient: 'from-purple-600 to-pink-600',
                badge: 'Auto Paste',
                features: ['Clipboard Sync', 'Thumbnail Grabber', 'Title & Creator Info'],
              },
              {
                screen: 'Choose Quality',
                subtitle: '1080p, 4K or MP3 Audio',
                gradient: 'from-blue-600 to-indigo-600',
                badge: 'Formats',
                features: ['2160p 4K Ultra HD', '1080p Full HD', '320kbps MP3 Extractor'],
              },
              {
                screen: 'Downloads',
                subtitle: 'Fast download manager',
                gradient: 'from-emerald-600 to-teal-600',
                badge: 'Accelerator',
                features: ['10x Acceleration', 'Pause & Resume', 'Background Downloads'],
              },
              {
                screen: 'Built-in Player',
                subtitle: 'HD video & music player',
                gradient: 'from-violet-600 to-indigo-800',
                badge: 'Media Vault',
                features: ['Gesture Controls', 'Background Audio', 'Passcode Vault'],
              },
            ].map((mock, idx) => (
              <div
                key={idx}
                className="min-w-[270px] sm:min-w-[300px] neu-flat p-5 rounded-[32px] flex flex-col justify-between shrink-0 space-y-4"
              >
                {/* Phone Mockup Representation */}
                <div className="bg-[#181c26] rounded-[24px] p-4 text-white space-y-4 border border-gray-800 shadow-md">
                  {/* Speaker Notch */}
                  <div className="w-20 h-3 bg-black/70 mx-auto rounded-b-lg mb-2" />

                  {/* Header inside screen mockup */}
                  <div className="flex items-center justify-between pb-2 border-b border-gray-800">
                    <span className="text-xs font-bold font-poppins">{mock.screen}</span>
                    <span className="text-[9px] bg-[#6c63ff]/20 text-[#a78bfa] px-2 py-0.5 rounded-full font-semibold">
                      {mock.badge}
                    </span>
                  </div>

                  {/* Screen Content Graphic */}
                  <div className={`h-48 rounded-2xl bg-gradient-to-br ${mock.gradient} p-4 flex flex-col justify-between text-white relative overflow-hidden shadow-inner`}>
                    <div className="absolute top-0 right-0 w-32 h-32 bg-white/10 rounded-full blur-xl pointer-events-none" />
                    
                    <div className="flex justify-between items-center text-xs opacity-90 font-medium">
                      <span>Netly Pro</span>
                      <Smartphone className="w-4 h-4" />
                    </div>

                    <div className="space-y-1">
                      <div className="w-10 h-10 rounded-xl bg-white/20 backdrop-blur-md flex items-center justify-center mb-2">
                        <Play className="w-5 h-5 fill-white" />
                      </div>
                      <p className="text-sm font-bold font-poppins leading-tight">
                        {mock.screen}
                      </p>
                      <p className="text-[11px] opacity-80 font-light">
                        {mock.subtitle}
                      </p>
                    </div>
                  </div>

                  {/* Highlights Bullet List */}
                  <div className="space-y-1.5 pt-1">
                    {mock.features.map((f, i) => (
                      <div key={i} className="flex items-center gap-2 text-[11px] text-gray-300">
                        <Check className="w-3 h-3 text-[#a78bfa]" />
                        <span>{f}</span>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Card Title Foot */}
                <div className="text-center pt-1">
                  <span className="text-sm font-bold font-poppins text-[#1a202c]">
                    {mock.screen}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* REVIEWS SECTION */}
      <section id="reviews" className="py-20 px-4 sm:px-8 bg-[#eef1f6]/60">
        <div className="max-w-7xl mx-auto space-y-12">
          {/* Section Header */}
          <div className="text-center max-w-2xl mx-auto space-y-4">
            <div className="inline-flex neu-flat-sm px-4 py-2 rounded-full text-xs font-bold uppercase tracking-wider text-[#6c63ff]">
              User Reviews
            </div>
            <h2 className="text-3xl sm:text-4xl font-extrabold font-poppins text-[#1a202c]">
              Loved by over 2 million Android users
            </h2>
            <p className="text-[#5a6578] text-base">
              Here is why users rate Netly 4.9 stars on Android community forums.
            </p>
          </div>

          {/* 3 Neumorphic Review Cards */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            {[
              {
                name: 'Alex Rivera',
                initials: 'AR',
                role: 'Verified Android User',
                quote:
                  'Netly is by far the cleanest video downloader for Android! No annoying ads, no watermark on TikTok reels, and downloads take literally 2 seconds.',
                rating: 5,
              },
              {
                name: 'Priya Sharma',
                initials: 'PS',
                role: 'Content Creator',
                quote:
                  'I love the MP3 extractor feature. I can download music videos as high-quality audio files straight to my phone storage. Highly recommended!',
                rating: 5,
              },
              {
                name: 'Marcus Chen',
                initials: 'MC',
                role: 'Verified User',
                quote:
                  'The soft Neumorphic UI design looks super sleek on my Galaxy S24! The batch download feature saved me hours when backing up my travel videos.',
                rating: 5,
              },
            ].map((review, i) => (
              <div
                key={i}
                className="neu-flat p-8 rounded-[28px] flex flex-col justify-between space-y-6"
              >
                {/* Stars Rating */}
                <div className="flex items-center gap-1">
                  {[...Array(review.rating)].map((_, s) => (
                    <Star
                      key={s}
                      className="w-5 h-5 fill-[#6c63ff] text-[#6c63ff]"
                    />
                  ))}
                </div>

                {/* Quote */}
                <p className="text-sm text-[#4a5568] leading-relaxed italic">
                  "{review.quote}"
                </p>

                {/* Author Info */}
                <div className="flex items-center gap-3 pt-2">
                  <div className="w-11 h-11 neu-pressed rounded-full flex items-center justify-center font-bold font-poppins text-xs text-[#6c63ff]">
                    {review.initials}
                  </div>
                  <div>
                    <h4 className="text-sm font-bold font-poppins text-[#1a202c]">
                      {review.name}
                    </h4>
                    <span className="text-xs text-[#a78bfa] font-medium flex items-center gap-1">
                      <ShieldCheck className="w-3.5 h-3.5 text-[#6c63ff]" />
                      {review.role}
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* DOWNLOAD CTA SECTION */}
      <section id="download" className="py-20 px-4 sm:px-8">
        <div className="max-w-5xl mx-auto">
          <div className="neu-flat-lg p-8 sm:p-14 rounded-[36px] text-center space-y-8 relative overflow-hidden border border-white/80">
            {/* Background Glow */}
            <div className="absolute -top-24 -left-24 w-64 h-64 bg-[#6c63ff]/10 rounded-full blur-3xl pointer-events-none" />
            <div className="absolute -bottom-24 -right-24 w-64 h-64 bg-[#a78bfa]/10 rounded-full blur-3xl pointer-events-none" />

            <div className="max-w-2xl mx-auto space-y-4">
              <div className="w-16 h-16 neu-interactive rounded-2xl mx-auto flex items-center justify-center text-[#6c63ff] mb-2">
                <Smartphone className="w-8 h-8" />
              </div>
              <h2 className="text-3xl sm:text-5xl font-extrabold font-poppins text-[#1a202c]">
                Get Netly on your phone
              </h2>
              <p className="text-[#5a6578] text-base sm:text-lg">
                Join over 2 million satisfied users enjoying ultra-fast, watermark-free video downloads today.
              </p>

              {/* Version & Status Tag */}
              <div className="inline-flex items-center gap-2 neu-flat-sm px-4 py-1.5 rounded-full text-xs font-semibold text-[#6c63ff]">
                <Tag className="w-3.5 h-3.5" />
                <span>
                  {releaseState.isLoading
                    ? 'Fetching latest stable release...'
                    : `Latest Stable · ${releaseState.version}`}
                </span>
                {releaseState.formattedReleaseDate && (
                  <span className="text-[#64748b] ml-1">
                    · Released {releaseState.formattedReleaseDate}
                  </span>
                )}
              </div>
            </div>

            {/* Main Download Button */}
            <div className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-2">
              <button
                onClick={handleOpenDownloadModal}
                className="w-full sm:w-auto neu-accent-btn px-10 py-5 rounded-2xl text-white font-poppins font-bold text-lg flex items-center justify-center gap-3 cursor-pointer group shadow-xl"
              >
                <Download className="w-6 h-6 group-hover:scale-110 transition-transform" />
                <span>
                  {releaseState.isLoading
                    ? 'Loading latest version...'
                    : `Download Latest APK (${releaseState.version})`}
                </span>
              </button>

              <a
                href={GITHUB_REPO_URL}
                target="_blank"
                rel="noopener noreferrer"
                className="w-full sm:w-auto neu-interactive px-7 py-5 rounded-2xl text-[#2d3748] hover:text-[#6c63ff] font-poppins font-semibold text-base flex items-center justify-center gap-2 transition-colors"
              >
                <Github className="w-5 h-5" />
                <span>View on GitHub</span>
              </a>
            </div>

            {/* Error / Fallback message if API failed */}
            {releaseState.isError && (
              <div className="flex items-center justify-center gap-2 text-xs text-amber-700 bg-amber-50 py-2 px-4 rounded-xl max-w-md mx-auto">
                <AlertCircle className="w-4 h-4 text-amber-600 shrink-0" />
                <span>Latest version unavailable.</span>
                <a
                  href={GITHUB_RELEASES_URL}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="font-bold underline hover:text-[#6c63ff]"
                >
                  View Releases on GitHub
                </a>
              </div>
            )}

            {/* App Specs Row */}
            <div className="pt-8 border-t border-gray-300/60 grid grid-cols-1 sm:grid-cols-3 gap-6 text-center">
              <div className="neu-pressed p-4 rounded-2xl">
                <span className="text-xs text-[#64748b] block font-medium">App Size</span>
                <span className="text-base font-bold font-poppins text-[#1a202c]">
                  {releaseState.apkSize}
                </span>
              </div>
              <div className="neu-pressed p-4 rounded-2xl">
                <span className="text-xs text-[#64748b] block font-medium">Latest Version</span>
                <span className="text-base font-bold font-poppins text-[#1a202c]">
                  {releaseState.version}
                </span>
              </div>
              <div className="neu-pressed p-4 rounded-2xl">
                <span className="text-xs text-[#64748b] block font-medium">
                  {releaseState.formattedReleaseDate ? 'Release Date' : 'Compatibility'}
                </span>
                <span className="text-base font-bold font-poppins text-[#1a202c]">
                  {releaseState.formattedReleaseDate
                    ? `Released ${releaseState.formattedReleaseDate}`
                    : 'Android 6.0+'}
                </span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* FAQ SECTION */}
      <section id="faq" className="py-20 px-4 sm:px-8 bg-[#eef1f6]/60">
        <div className="max-w-4xl mx-auto space-y-12">
          {/* Section Header */}
          <div className="text-center space-y-4">
            <div className="inline-flex neu-flat-sm px-4 py-2 rounded-full text-xs font-bold uppercase tracking-wider text-[#6c63ff]">
              Got Questions?
            </div>
            <h2 className="text-3xl sm:text-4xl font-extrabold font-poppins text-[#1a202c]">
              Frequently Asked Questions
            </h2>
            <p className="text-[#5a6578] text-base">
              Find answers to common questions about installing and using Netly.
            </p>
          </div>

          {/* Neumorphic Accordion */}
          <div className="space-y-4">
            {[
              {
                q: 'Is Netly completely free to use?',
                a: 'Yes! Netly is 100% free to download and use. There are no hidden subscription fees, micro-transactions, or compulsory paid tiers.',
              },
              {
                q: 'What video platforms are supported?',
                a: 'Netly supports over 1000+ social and streaming video platforms including TikTok, Instagram Reels, YouTube, Twitter/X, Facebook, Pinterest, Reddit, and Vimeo.',
              },
              {
                q: 'Do I need an account or login to download videos?',
                a: 'No registration or login is required. You can simply copy any public video link and download it instantly without giving any personal information.',
              },
              {
                q: 'Is downloading the APK safe for my Android phone?',
                a: 'Absolutely. Netly APK is scanned for malware, adware, and viruses before every release. It contains no harmful code and adheres strictly to Android security guidelines.',
              },
              {
                q: 'Where are downloaded videos stored on my device?',
                a: 'Downloaded videos and MP3 files are saved directly in your phone’s default "Downloads/Netly" folder and automatically sync with your System Gallery app.',
              },
            ].map((faq, index) => {
              const isOpen = activeFaq === index;
              return (
                <div
                  key={index}
                  className="neu-flat rounded-[24px] overflow-hidden transition-all duration-200"
                >
                  <button
                    onClick={() => setActiveFaq(isOpen ? null : index)}
                    className="w-full px-6 py-5 text-left flex items-center justify-between gap-4 focus:outline-none"
                  >
                    <span className="font-poppins font-bold text-base sm:text-lg text-[#1a202c]">
                      {faq.q}
                    </span>
                    <div className="w-9 h-9 neu-pressed rounded-xl flex items-center justify-center text-[#6c63ff] shrink-0">
                      {isOpen ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
                    </div>
                  </button>
                  {isOpen && (
                    <div className="px-6 pb-6 pt-1 text-sm text-[#5a6578] leading-relaxed border-t border-gray-200/50">
                      {faq.a}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      </section>
      </main>

      {/* FOOTER */}
      <footer className="py-12 px-4 sm:px-8 border-t border-gray-300/50 text-sm text-[#64748b]">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between gap-6">
          {/* Logo & Copyright */}
          <div className="flex flex-col items-center md:items-start gap-2">
            <div className="flex items-center gap-2">
              <div className="w-7 h-7 rounded-xl bg-gradient-to-tr from-[#6c63ff] to-[#a78bfa] flex items-center justify-center text-white">
                <Download className="w-4 h-4" />
              </div>
              <span className="font-poppins font-extrabold text-lg text-[#1a202c]">
                Net<span className="text-gradient">ly</span>
              </span>
            </div>
            <p className="text-xs text-[#64748b] text-center md:text-left">
              © {new Date().getFullYear()} Netly. All rights reserved. For personal offline backups and fair use.
            </p>
          </div>

          {/* Links */}
          <div className="flex flex-wrap items-center justify-center gap-6 text-xs font-semibold">
            <a href="#features" className="hover:text-[#6c63ff] transition-colors">Features</a>
            <a href="#how-it-works" className="hover:text-[#6c63ff] transition-colors">How it works</a>
            <a href="#faq" className="hover:text-[#6c63ff] transition-colors">FAQ</a>
            <a
              href={GITHUB_REPO_URL}
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-[#6c63ff] transition-colors flex items-center gap-1.5"
            >
              <Github className="w-3.5 h-3.5" />
              <span>GitHub</span>
            </a>
            <a
              href={GITHUB_RELEASES_URL}
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-[#6c63ff] transition-colors"
            >
              Releases
            </a>
            <a href="#" className="hover:text-[#6c63ff] transition-colors">Privacy Policy</a>
            <a href="#" className="hover:text-[#6c63ff] transition-colors">Terms</a>
          </div>
        </div>
      </footer>

      {/* SPECIAL REQUIREMENT — FLOATING DOWNLOAD BUTTON (FAB) */}
      <div className="fixed right-6 bottom-6 z-[99]">
        <button
          onClick={handleOpenDownloadModal}
          className="neu-floating-fab group flex items-center justify-center text-white font-poppins font-bold cursor-pointer rounded-full p-4 sm:px-6 sm:py-4 shadow-2xl transition-all"
          aria-label={`Download Netly APK ${releaseState.version}`}
          title={`Download Netly ${releaseState.version}`}
        >
          <Download className="w-6 h-6 group-hover:scale-110 transition-transform shrink-0" />
          <span className="hidden sm:inline-block ml-2.5 text-sm tracking-wide">
            Download {releaseState.version}
          </span>
        </button>
      </div>

      {/* DOWNLOAD APK MODAL */}
      {isDownloadModalOpen && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/40 backdrop-blur-xs animate-fadeIn">
          <div className="neu-flat p-8 rounded-[32px] max-w-md w-full relative space-y-5 border border-white/80 max-h-[90vh] overflow-y-auto custom-scroll">
            {/* Close Modal Button */}
            <button
              onClick={() => setIsDownloadModalOpen(false)}
              className="absolute top-6 right-6 w-9 h-9 neu-interactive rounded-xl flex items-center justify-center text-gray-600 hover:text-black cursor-pointer"
              aria-label="Close modal"
            >
              <X className="w-5 h-5" />
            </button>

            {/* Header */}
            <div className="flex items-center gap-4 pt-2">
              <div className="w-14 h-14 neu-flat-sm rounded-2xl flex items-center justify-center text-[#6c63ff] shrink-0">
                <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-[#6c63ff] to-[#a78bfa] flex items-center justify-center text-white">
                  <Download className="w-6 h-6" />
                </div>
              </div>
              <div>
                <h3 className="text-xl font-bold font-poppins text-[#1a202c]">
                  Netly Android APK
                </h3>
                <span className="text-xs text-[#6c63ff] font-semibold bg-[#6c63ff]/10 px-2.5 py-0.5 rounded-full inline-block mt-0.5">
                  Version {releaseState.version} · {releaseState.apkSize}
                  {releaseState.formattedReleaseDate ? ` · ${releaseState.formattedReleaseDate}` : ''}
                </span>
              </div>
            </div>

            {/* Progress / Status Box */}
            <div className="neu-pressed p-4 rounded-2xl space-y-2.5">
              <div className="flex justify-between items-center text-xs font-semibold">
                <span className="text-[#2d3748]">
                  {downloadProgress < 100 ? 'Starting Download...' : 'Download Ready!'}
                </span>
                <span className="text-[#6c63ff] font-mono">{downloadProgress}%</span>
              </div>

              {/* Progress Bar */}
              <div className="w-full h-2.5 bg-gray-300/80 rounded-full overflow-hidden p-0.5">
                <div
                  className="h-full bg-gradient-to-r from-[#6c63ff] to-[#a78bfa] rounded-full transition-all duration-200"
                  style={{ width: `${downloadProgress}%` }}
                />
              </div>

              <p className="text-[11px] text-[#64748b] leading-tight">
                {downloadProgress < 100
                  ? 'Connecting to GitHub Releases to fetch the official signed Netly APK...'
                  : `Downloading ${releaseState.apkFileName}. If your download does not start automatically, click the button below.`}
              </p>
            </div>

            {/* What's New from GitHub Release notes if available */}
            {cleanNotes.length > 0 && (
              <div className="neu-pressed p-4 rounded-2xl text-left space-y-2 border border-indigo-100/50">
                <div className="flex items-center justify-between text-xs font-bold text-[#6c63ff]">
                  <span className="flex items-center gap-1.5">
                    <Sparkles className="w-3.5 h-3.5" />
                    What's New in {releaseState.version}
                  </span>
                  <a
                    href={releaseState.releaseUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-[10px] text-gray-500 hover:text-[#6c63ff] flex items-center gap-1 transition-colors"
                  >
                    Notes <ExternalLink className="w-2.5 h-2.5" />
                  </a>
                </div>
                <ul className="space-y-1.5 text-xs text-[#5a6578]">
                  {cleanNotes.slice(0, 5).map((note, idx) => (
                    <li key={idx} className="flex items-start gap-2 text-[11px] leading-snug">
                      <Check className="w-3.5 h-3.5 text-[#6c63ff] shrink-0 mt-0.5" />
                      <span>{note}</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {/* Action Buttons */}
            <div className="space-y-2.5 pt-1">
              <a
                href={releaseState.apkDownloadUrl || GITHUB_RELEASES_URL}
                download={releaseState.apkFileName || 'Netly.apk'}
                target="_blank"
                rel="noopener noreferrer"
                className="w-full neu-accent-btn py-3.5 rounded-2xl text-white font-poppins font-bold text-center flex items-center justify-center gap-2 cursor-pointer shadow-md"
              >
                <Download className="w-5 h-5" />
                <span>Save {releaseState.apkFileName || 'Netly APK'}</span>
              </a>

              <div className="flex gap-2">
                <a
                  href={releaseState.releaseUrl || GITHUB_RELEASES_URL}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex-1 neu-interactive py-2.5 rounded-xl text-xs font-semibold text-[#5a6578] hover:text-[#6c63ff] text-center flex items-center justify-center gap-1.5"
                >
                  <Github className="w-3.5 h-3.5" />
                  <span>GitHub Release</span>
                </a>
                <button
                  onClick={() => setIsDownloadModalOpen(false)}
                  className="flex-1 neu-interactive py-2.5 rounded-xl text-xs font-semibold text-[#5a6578] text-center cursor-pointer"
                >
                  Close
                </button>
              </div>
            </div>

            {/* Security Badge Foot */}
            <div className="flex items-center justify-center gap-2 text-xs text-emerald-600 font-semibold pt-1">
              <ShieldCheck className="w-4 h-4 text-emerald-500" />
              <span>Scanned by Google Play Protect · Adware Free</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

