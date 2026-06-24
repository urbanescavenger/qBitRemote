package com.jbcbros.qbitremote.data.model

data class Torrent(
    val hash: String,
    val name: String,
    val state: String,
    val progress: Float,
    val size: Long,
    val total_size: Long,
    val dlspeed: Long,
    val upspeed: Long,
    val dl_limit: Long = 0L,
    val up_limit: Long = 0L,
    val downloaded: Long,
    val uploaded: Long,
    val ratio: Float,
    val category: String = "",
    val tags: List<String> = emptyList(),
    val num_complete: Int = 0,
    val trackers_count: Int = 0,
    val tracker: String = "",
    val save_path: String = "",
    val seeding_time: Long = 0L
)
