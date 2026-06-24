package com.jbcbros.qbitremote.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jbcbros.qbitremote.R
import com.jbcbros.qbitremote.data.model.ServerConfig
import com.jbcbros.qbitremote.data.repository.QbRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
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
                _uiState.value = _uiState.value.copy(isTesting = false, testResult = context.getString(R.string.msg_login_success), saved = true)
                onSaved()
            } else {
                val reason = when {
                    result.error != null -> context.getString(R.string.error_network, result.error)
                    result.status == 401 -> context.getString(R.string.error_http_401)
                    result.status == 403 -> context.getString(R.string.error_http_403)
                    else -> context.getString(R.string.error_http_code, result.status, result.body)
                }
                _uiState.value = _uiState.value.copy(isTesting = false, testResult = context.getString(R.string.msg_test_failed, reason), saved = false)
            }
        }
    }

    fun clearSaved() {
        _uiState.value = _uiState.value.copy(saved = false)
    }
}
