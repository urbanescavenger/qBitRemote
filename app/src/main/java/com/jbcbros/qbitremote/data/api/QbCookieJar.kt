package com.jbcbros.qbitremote.data.api

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class QbCookieJar : CookieJar {
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        if (!cookieStore.containsKey(host)) {
            cookieStore[host] = mutableListOf()
        }
        cookieStore[host]?.apply {
            removeAll { existing ->
                cookies.any { it.name == existing.name }
            }
            addAll(cookies)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookieStore[url.host] ?: emptyList()
    }

    fun clear() {
        cookieStore.clear()
    }
}
