package com.jbcbros.qbitremote.data.model

/** Read-only view of a tracker for a torrent (qBittorrent /api/v2/torrents/trackers). */
data class Tracker(
    val url: String = "",
    val status: Int = 0,
    val tier: Int = 0,
    val num_peers: Int = 0,
    val num_seeds: Int = 0,
    val num_leeches: Int = 0,
    val msg: String = ""
)
