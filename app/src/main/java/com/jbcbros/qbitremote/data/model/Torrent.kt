package com.jbcbros.qbitremote.data.model

data class Torrent(
    val hash: String = "",
    val name: String = "",
    val state: String = "",
    val progress: Float = 0f,
    val size: Long = 0L,
    val total_size: Long = 0L,
    val dlspeed: Long = 0L,
    val upspeed: Long = 0L,
    val dl_limit: Long = 0L,
    val up_limit: Long = 0L,
    val downloaded: Long = 0L,
    val uploaded: Long = 0L,
    val ratio: Float = 0f,
    val category: String = "",
    val tags: List<String> = emptyList(),
    val num_complete: Int = 0,
    val trackers_count: Int = 0,
    val tracker: String = "",
    val save_path: String = "",
    val seeding_time: Long = 0L
)
