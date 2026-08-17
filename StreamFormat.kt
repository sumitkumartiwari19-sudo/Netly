package com.netly.app.domain.model

data class StreamFormat(
    val formatId: String,
    val quality: String,
    val container: String,
    val isAudioOnly: Boolean,
    val url: String,
    val fileSizeApprox: Long = 0L
)
