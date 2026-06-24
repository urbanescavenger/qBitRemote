package com.jbcbros.qbitremote.data.repository

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jbcbros.qbitremote.R
import com.jbcbros.qbitremote.data.api.QbApiService
import com.jbcbros.qbitremote.data.api.QbAuthInterceptor
import com.jbcbros.qbitremote.data.api.QbCookieJar
import com.jbcbros.qbitremote.data.model.LoginResult
import com.jbcbros.qbitremote.data.model.ServerConfig
import com.jbcbros.qbitremote.data.model.Torrent
import com.jbcbros.qbitremote.data.model.TransferInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class QbRepository @Inject constructor(
    private val context: Context
) {
    private val cookieJar = QbCookieJar()
    private var currentConfig: ServerConfig = ServerConfig()
    private var apiService: QbApiService? = null

    /** Last connection error encountered by polling, surfaced to the UI as a Snackbar. */
    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    companion object {
        private val HOST = stringPreferencesKey("host")
        private val PORT = stringPreferencesKey("port")
        private val USERNAME = stringPreferencesKey("username")
        private val PASSWORD = stringPreferencesKey("password")
        private val NICKNAME = stringPreferencesKey("nickname")
        private val SSL = booleanPreferencesKey("ssl")
    }

    val serverConfig: Flow<ServerConfig> = context.dataStore.data.map { prefs ->
        ServerConfig(
            nickname = prefs[NICKNAME] ?: "",
            host = prefs[HOST] ?: "",
            port = prefs[PORT] ?: "",
            username = prefs[USERNAME] ?: "",
            password = prefs[PASSWORD] ?: "",
            ssl = prefs[SSL] ?: false
        )
    }

    suspend fun loadConfig(): ServerConfig {
        val config = serverConfig.first()
        currentConfig = config
        updateApiService(config)
        return config
    }

    suspend fun saveConfig(config: ServerConfig) {
        context.dataStore.edit { prefs ->
            prefs[HOST] = config.host
            prefs[PORT] = config.port
            prefs[USERNAME] = config.username
            prefs[PASSWORD] = config.password
            prefs[NICKNAME] = config.nickname
            prefs[SSL] = config.ssl
        }
        currentConfig = config
        updateApiService(config)
    }

    private fun updateApiService(config: ServerConfig) {
        val baseUrl = config.baseUrl()
        if (baseUrl.isBlank()) return

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(logging)
            .addInterceptor(QbAuthInterceptor { currentConfig })
            .build()

        apiService = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(QbApiService::class.java)
    }

    suspend fun testLogin(config: ServerConfig): LoginResult {
        val baseUrl = config.baseUrl()
        if (baseUrl.isBlank()) return LoginResult(false, error = context.getString(R.string.msg_no_config))

        val tempClient = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .build()

        val tempService = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(tempClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(QbApiService::class.java)

        return try {
            val res = tempService.login(config.username, config.password)
            val body = res.body()?.string()?.trim() ?: ""
            val ok = res.code() == 204 || (res.code() == 200 && body.lowercase() == "ok.")
            if (ok) {
                apiService = tempService
                currentConfig = config
            }
            LoginResult(ok, res.code(), body)
        } catch (e: Exception) {
            LoginResult(false, error = e.message)
        }
    }

    suspend fun login(): LoginResult {
        val service = apiService ?: return LoginResult(false, error = context.getString(R.string.msg_no_config))
        return try {
            val res = service.login(currentConfig.username, currentConfig.password)
            val body = res.body()?.string()?.trim() ?: ""
            val ok = res.code() == 204 || (res.code() == 200 && body.lowercase() == "ok.")
            LoginResult(ok, res.code(), body)
        } catch (e: Exception) {
            LoginResult(false, error = e.message)
        }
    }

    suspend fun getTorrents(filter: String? = null, sort: String = "added_on", reverse: Boolean = true): List<Torrent> {
        val service = apiService ?: return emptyList()
        return try {
            val res = service.getTorrents(sort = sort, reverse = reverse, filter = filter)
            val body = res.body()?.string() ?: return emptyList()
            val list = com.google.gson.Gson().fromJson(body, Array<Torrent>::class.java)
            _connectionError.value = null
            list?.toList() ?: emptyList()
        } catch (e: Exception) {
            _connectionError.value = context.getString(R.string.error_connection)
            emptyList()
        }
    }

    suspend fun getTransferInfo(): TransferInfo? {
        val service = apiService ?: return null
        return try {
            val res = service.getTransferInfo()
            val body = res.body()?.string() ?: return null
            com.google.gson.Gson().fromJson(body, TransferInfo::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCategories(): List<String> {
        val service = apiService ?: return emptyList()
        return try {
            val res = service.getCategories()
            val body = res.body()?.string() ?: return emptyList()
            val map = com.google.gson.Gson().fromJson(body, Map::class.java) as? Map<String, Map<String, Any>>
            map?.keys?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTags(): List<String> {
        val service = apiService ?: return emptyList()
        return try {
            val res = service.getTags()
            val body = res.body()?.string() ?: return emptyList()
            val arr = com.google.gson.Gson().fromJson(body, Array<String>::class.java)
            arr?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addTorrentByUrl(urls: String, category: String? = null, tags: String? = null): Boolean {
        val service = apiService ?: return false
        return try {
            val res = service.addTorrentByUrl(urls, category, tags)
            val body = res.body()?.string()?.lowercase()?.trim() ?: ""
            res.isSuccessful && !body.contains("fail")
        } catch (e: Exception) {
            false
        }
    }

    suspend fun addTorrentFile(uri: Uri, category: String? = null, tags: String? = null): Boolean {
        val service = apiService ?: return false
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                val fileName = uri.lastPathSegment ?: "torrent"
                val requestBody = bytes.toRequestBody("application/x-bittorrent".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("torrents", fileName, requestBody)
                val categoryBody = category?.let {
                    it.toRequestBody("text/plain".toMediaTypeOrNull())
                }
                val tagsBody = tags?.takeIf { it.isNotBlank() }?.let {
                    it.toRequestBody("text/plain".toMediaTypeOrNull())
                }
                val res = service.addTorrentFile(part, categoryBody, tagsBody)
                val body = res.body()?.string()?.lowercase()?.trim() ?: ""
                return res.isSuccessful && !body.contains("fail")
            } ?: return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    suspend fun stopTorrent(hash: String): Boolean = action { apiService?.stopTorrent(hash) }
    suspend fun startTorrent(hash: String): Boolean = action { apiService?.startTorrent(hash) }
    suspend fun recheckTorrent(hash: String): Boolean = action { apiService?.recheckTorrent(hash) }

    /** qBittorrent accepts hashes="all" to act on every torrent. */
    suspend fun stopAllTorrents(): Boolean = action { apiService?.stopTorrent("all") }
    suspend fun startAllTorrents(): Boolean = action { apiService?.startTorrent("all") }

    suspend fun deleteTorrent(hash: String, deleteFiles: Boolean = true): Boolean {
        val service = apiService ?: return false
        return try {
            val res = service.deleteTorrent(hash, deleteFiles.toString())
            res.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun action(block: suspend () -> retrofit2.Response<okhttp3.ResponseBody>?): Boolean {
        return try {
            val res = block()
            res?.isSuccessful == true
        } catch (e: Exception) {
            false
        }
    }
}
