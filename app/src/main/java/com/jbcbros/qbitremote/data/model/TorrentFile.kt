package com.jbcbros.qbitremote.data.model

/** Read-only view of a file inside a torrent (qBittorrent /api/v2/torrents/files). */
data class TorrentFile(
    val index: Int = 0,
    val name: String = "",
    val size: Long = 0L,
    val progress: Float = 0f,
    val priority: Int = 0
)
