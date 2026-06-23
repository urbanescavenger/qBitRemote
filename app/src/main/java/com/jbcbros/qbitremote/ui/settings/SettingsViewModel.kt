package com.jbcbros.qbitremote.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jbcbros.qbitremote.data.model.LoginResult
import com.jbcbros.qbitremote.data.model.ServerConfig
import com.jbcbros.qbitremote.data.repository.QbRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val nickname: String = "",
    val host: String = "",
    val port: String = "",
    val username: String = "",
    val password: String = "",
    val ssl: Boolean = false,
    val isTesting: Boolean = false,
    val testResult: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: QbRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val config = repository.serverConfig.firstOrNull() ?: ServerConfig()
            _uiState.value = SettingsUiState(
                nickname = config.nickname,
                host = config.host,
                port = config.port,
                username = config.username,
                password = config.password,
                ssl = config.ssl
            )
        }
    }

    fun updateNickname(value: String) { _uiState.value = _uiState.value.copy(nickname = value) }
    fun updateHost(value: String) { _uiState.value = _uiState.value.copy(host = value) }
    fun updatePort(value: String) { _uiState.value = _uiState.value.copy(port = value) }
    fun updateUsername(value: String) { _uiState.value = _uiState.value.copy(username = value) }
    fun updatePassword(value: String) { _uiState.value = _uiState.value.copy(password = value) }
    fun updateSsl(value: Boolean) { _uiState.value = _uiState.value.copy(ssl = value) }

    fun testAndSave(onSaved: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTesting = true, testResult = null)
            val config = ServerConfig(
                nickname = _uiState.value.nickname,
                host = _uiState.value.host,
                port = _uiState.value.port,
                username = _uiState.value.username,
                password = _uiState.value.password,
                ssl = _uiState.value.ssl
            )
            val result = repository.testLogin(config)
            if (result.ok) {
                repository.saveConfig(config)
                _uiState.value = _uiState.value.copy(isTesting = false, testResult = "登录成功，已保存", saved = true)
                onSaved()
            } else {
                val reason = when {
                    result.error != null -> "network: ${result.error}"
                    result.status == 401 -> "HTTP 401 — 用户名或密码错误"
                    result.status == 403 -> "HTTP 403 — 被 qBittorrent 封禁 IP"
                    else -> "HTTP ${result.status}: ${result.body}"
                }
                _uiState.value = _uiState.value.copy(isTesting = false, testResult = "无法通过验证。$reason", saved = false)
            }
        }
    }

    fun clearSaved() {
        _uiState.value = _uiState.value.copy(saved = false)
    }
}
