package com.jbcbros.qbitremote.data.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for Gson parsing of qBittorrent /torrents/info.
 *
 * Background: v1.6.0 declared Torrent.tags as List<String>, but qBittorrent returns tags as a
 * comma-separated STRING — Gson threw JsonSyntaxException and the whole list came back empty.
 * These tests pin the correct type so that regression cannot silently return.
 */
class TorrentParsingTest {

    private val gson = Gson()

    private val sample = """
        [
          {
            "hash": "abc123",
            "name": "Big Buck Bunny",
            "state": "downloading",
            "progress": 0.5,
            "size": 1024,
            "total_size": 2048,
            "dlspeed": 100,
            "upspeed": 50,
            "dl_limit": 0,
            "up_limit": 0,
            "downloaded": 512,
            "uploaded": 256,
            "ratio": 0.5,
            "category": "movies",
            "tags": "movie,hd",
            "num_complete": 5,
            "trackers_count": 1,
            "tracker": "http://example.test/announce",
            "save_path": "/downloads",
            "seeding_time": 0
          }
        ]
    """.trimIndent()

    @Test
    fun parsesTorrentArray() {
        val list = gson.fromJson(sample, Array<Torrent>::class.java)
        assertNotNull(list)
        assertEquals(1, list!!.size)
    }

    @Test
    fun tagsIsCommaStringNotArray() {
        val list = gson.fromJson(sample, Array<Torrent>::class.java)!!
        // qBittorrent returns tags as a comma-separated string, NOT a JSON array.
        assertEquals("movie,hd", list[0].tags)
    }

    @Test
    fun parsesCoreFields() {
        val t = gson.fromJson(sample, Array<Torrent>::class.java)!![0]
        assertEquals("abc123", t.hash)
        assertEquals("Big Buck Bunny", t.name)
        assertEquals("downloading", t.state)
        assertEquals(0.5f, t.progress, 0.0001f)
        assertEquals(1024L, t.size)
        assertEquals(2048L, t.total_size)
        assertEquals(100L, t.dlspeed)
        assertEquals(50L, t.upspeed)
        assertEquals(0L, t.dl_limit)
        assertEquals(0L, t.up_limit)
        assertEquals(512L, t.downloaded)
        assertEquals(256L, t.uploaded)
        assertEquals(0.5f, t.ratio, 0.0001f)
        assertEquals("movies", t.category)
        assertEquals(5, t.num_complete)
        assertEquals("/downloads", t.save_path)
    }

    @Test
    fun missingFieldsUseDefaults() {
        val minimal = """[{ "hash": "h", "name": "n", "state": "s" }]""".trimIndent()
        val t = gson.fromJson(minimal, Array<Torrent>::class.java)!![0]
        assertEquals("h", t.hash)
        assertEquals("", t.tags)
        assertEquals(0L, t.size)
        assertEquals(0f, t.progress, 0.0001f)
        assertTrue(t.tags.isEmpty())
    }
}
