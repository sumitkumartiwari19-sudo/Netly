package com.netly.app.domain.repository

import com.netly.app.domain.model.VideoInfo
import com.netly.app.domain.model.VideoStreamInfo

interface ExtractorRepository {
    suspend fun extractVideoInfo(url: String): Result<VideoInfo>
    suspend fun searchVideos(query: String): Result<List<VideoInfo>>
    suspend fun getStreamInfo(url: String): Result<VideoStreamInfo>
}
