package com.netly.app.data.updater.util

/**
 * Robust semantic version parser and comparator.
 * Handles formats like: "v1.0", "v1.1", "v1.1.0", "1.1.0", "1.9.0" < "1.10.0", "1.0.1-beta", "v2.0.0+build123"
 */
class SemanticVersion(
    val raw: String,
    val digits: List<Int>,
    val preRelease: String? = null
) : Comparable<SemanticVersion> {

    val major: Int get() = digits.getOrElse(0) { 0 }
    val minor: Int get() = digits.getOrElse(1) { 0 }
    val patch: Int get() = digits.getOrElse(2) { 0 }

    companion object {
        fun parse(versionString: String?): SemanticVersion {
            if (versionString.isNullOrBlank()) {
                return SemanticVersion("", listOf(0, 0, 0), null)
            }

            val clean = versionString.trim()
                .removePrefix("v")
                .removePrefix("V")
                .trim()

            // Split on '+' to remove build metadata
            val withoutBuild = clean.split('+')[0]

            // Split on '-' to separate version numbers from pre-release tags (e.g. "1.0.0-beta.1")
            val parts = withoutBuild.split('-', limit = 2)
            val versionNumbers = parts[0].split('.').mapNotNull { it.trim().toIntOrNull() }
            val preRelease = if (parts.size > 1) parts[1].trim().ifBlank { null } else null

            val normalizedDigits = if (versionNumbers.isEmpty()) {
                listOf(0, 0, 0)
            } else {
                versionNumbers
            }

            return SemanticVersion(clean, normalizedDigits, preRelease)
        }

        fun isNewer(remoteVersion: String?, currentVersion: String?): Boolean {
            val remote = parse(remoteVersion)
            val current = parse(currentVersion)
            return remote > current
        }
    }

    override fun compareTo(other: SemanticVersion): Int {
        val maxLen = maxOf(digits.size, other.digits.size)
        for (i in 0 until maxLen) {
            val d1 = digits.getOrElse(i) { 0 }
            val d2 = other.digits.getOrElse(i) { 0 }
            if (d1 != d2) {
                return d1.compareTo(d2)
            }
        }

        // If numeric components are equal:
        // A release without preRelease is NEWER than a preRelease (e.g., 1.0.0 > 1.0.0-beta)
        if (preRelease == null && other.preRelease != null) return 1
        if (preRelease != null && other.preRelease == null) return -1
        if (preRelease != null && other.preRelease != null) {
            return preRelease.compareTo(other.preRelease)
        }

        return 0
    }

    override fun toString(): String {
        return digits.joinToString(".") + if (preRelease != null) "-$preRelease" else ""
    }
}

