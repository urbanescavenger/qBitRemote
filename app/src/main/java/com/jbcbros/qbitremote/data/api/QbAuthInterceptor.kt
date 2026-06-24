package com.jbcbros.qbitremote.data.api

import com.jbcbros.qbitremote.data.model.ServerConfig
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * Transparently re-authenticates a qBittorrent session.
 *
 * qBittorrent responds `403` to any request issued without a valid session cookie. When the
 * Android app is backgrounded the session often expires silently; the next poll then receives a
 * run of `403`s and the UI looks empty even though the server is fine.
 *
 * This interceptor watches for `403`, performs a single synchronous [login][performLogin] using
 * the current [ServerConfig], and retries the original request exactly once.
 *
 * Constraints:
 * - GET requests are replayed (the polling/info calls are where silent expiry bites).
 * - POST action bodies are one-shot and cannot be replayed, so they are returned as-is; they
 *   recover on the next poll, which re-establishes auth via a GET.
 * - The login request itself carries [NO_AUTH_HEADER] so the interceptor never recurses.
 */
class QbAuthInterceptor(
    private val configProvider: () -> ServerConfig
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        // Login call or any explicit response other than 403: pass through.
        if (request.header(NO_AUTH_HEADER) != null) return response
        if (response.code != HTTP_FORBIDDEN) return response
        // Only GET can be safely replayed.
        if (request.method != "GET") return response

        response.close()

        val config = configProvider()
        if (config.baseUrl().isBlank()) {
            return chain.proceed(request)
        }

        runCatching { performLogin(chain, config).close() }
        // Retry the original GET once with the refreshed session cookie.
        return chain.proceed(request)
    }

    private fun performLogin(chain: Interceptor.Chain, config: ServerConfig): Response {
        val body = FormBody.Builder()
            .add("username", config.username)
            .add("password", config.password)
            .build()
        val loginRequest = Request.Builder()
            .url("${config.baseUrl()}/api/v2/auth/login")
            .post(body)
            .header(NO_AUTH_HEADER, "1")
            .build()
        return chain.proceed(loginRequest)
    }

    companion object {
        private const val HTTP_FORBIDDEN = 403
        const val NO_AUTH_HEADER = "X-Qbit-NoAuth"
    }
}
