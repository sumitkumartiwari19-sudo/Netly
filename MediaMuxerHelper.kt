package com.netly.app.util

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

object MediaMuxerHelper {
    private const val TAG = "MediaMuxerHelper"

    /**
     * Muxes separate video and audio files into a single MP4 container file.
     *
     * @param videoFile Downloaded video-only stream file
     * @param audioFile Downloaded audio stream file
     * @param outputFile Output destination MP4 file
     * @param onProgress Optional callback for progress reporting (0-100)
     * @return true if muxing succeeded
     */
    suspend fun mux(
        videoFile: File,
        audioFile: File,
        outputFile: File,
        onProgress: (suspend (Int) -> Unit)? = null
    ): Boolean {
        if (!videoFile.exists() || videoFile.length() == 0L) {
            throw IllegalArgumentException("Video source file is missing or empty (${videoFile.length()} bytes)")
        }
        if (!audioFile.exists() || audioFile.length() == 0L) {
            throw IllegalArgumentException("Audio source file is missing or empty (${audioFile.length()} bytes)")
        }

        var videoExtractor: MediaExtractor? = null
        var audioExtractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null

        try {
            videoExtractor = MediaExtractor().apply {
                setDataSource(videoFile.absolutePath)
            }
            audioExtractor = MediaExtractor().apply {
                setDataSource(audioFile.absolutePath)
            }

            // 1. Locate video track in video source
            var videoTrackIndex = -1
            var videoFormat: MediaFormat? = null
            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i
                    videoFormat = format
                    break
                }
            }

            if (videoTrackIndex == -1 || videoFormat == null) {
                throw IllegalStateException("No video track found in source video stream: ${videoFile.name}")
            }

            // 2. Locate audio track in audio source
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until audioExtractor.trackCount) {
                val format = audioExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                throw IllegalStateException("No audio track found in source audio stream: ${audioFile.name}")
            }

            videoExtractor.selectTrack(videoTrackIndex)
            audioExtractor.selectTrack(audioTrackIndex)

            // 3. Prepare output file
            if (outputFile.exists()) {
                outputFile.delete()
            }
            outputFile.parentFile?.mkdirs()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerVideoTrackIndex = muxer.addTrack(videoFormat)
            val muxerAudioTrackIndex = muxer.addTrack(audioFormat)

            muxer.start()

            // 4. Calculate buffer size
            val maxVideoSize = try { videoFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) } catch (e: Exception) { 0 }
            val maxAudioSize = try { audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) } catch (e: Exception) { 0 }
            val bufferSize = maxOf(maxVideoSize, maxAudioSize, 2 * 1024 * 1024)
            val buffer = ByteBuffer.allocateDirect(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            val videoDurationUs = try { videoFormat.getLong(MediaFormat.KEY_DURATION) } catch (e: Exception) { 0L }

            var currentMaxVideoTimeUs = 0L
            var videoDone = false
            var audioDone = false
            var sampleCount = 0

            // 5. Interleave samples chronologically based on presentation timestamps
            while (!videoDone || !audioDone) {
                val videoTime = if (!videoDone) videoExtractor.sampleTime else Long.MAX_VALUE
                val audioTime = if (!audioDone) audioExtractor.sampleTime else Long.MAX_VALUE

                if (!videoDone && (audioDone || videoTime <= audioTime)) {
                    bufferInfo.offset = 0
                    val sampleSize = videoExtractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) {
                        videoDone = true
                    } else {
                        val pts = videoExtractor.sampleTime
                        bufferInfo.size = sampleSize
                        bufferInfo.presentationTimeUs = pts
                        bufferInfo.flags = videoExtractor.sampleFlags
                        muxer.writeSampleData(muxerVideoTrackIndex, buffer, bufferInfo)
                        
                        if (pts > currentMaxVideoTimeUs) {
                            currentMaxVideoTimeUs = pts
                        }
                        videoExtractor.advance()
                    }
                } else if (!audioDone) {
                    bufferInfo.offset = 0
                    val sampleSize = audioExtractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) {
                        audioDone = true
                    } else {
                        val pts = audioExtractor.sampleTime
                        bufferInfo.size = sampleSize
                        bufferInfo.presentationTimeUs = pts
                        bufferInfo.flags = audioExtractor.sampleFlags
                        muxer.writeSampleData(muxerAudioTrackIndex, buffer, bufferInfo)
                        audioExtractor.advance()
                    }
                }

                sampleCount++
                if (sampleCount % 50 == 0 && videoDurationUs > 0 && currentMaxVideoTimeUs > 0) {
                    val progress = ((currentMaxVideoTimeUs * 100) / videoDurationUs).toInt().coerceIn(0, 100)
                    onProgress?.invoke(progress)
                }
            }

            onProgress?.invoke(100)
            Log.d(TAG, "Muxing complete: ${outputFile.length()} bytes written to ${outputFile.name}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Muxing failed: ${e.message}", e)
            if (outputFile.exists()) {
                outputFile.delete()
            }
            throw e
        } finally {
            try {
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing muxer: ${e.message}")
            }
            try {
                videoExtractor?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing video extractor: ${e.message}")
            }
            try {
                audioExtractor?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing audio extractor: ${e.message}")
            }
        }
    }
}
