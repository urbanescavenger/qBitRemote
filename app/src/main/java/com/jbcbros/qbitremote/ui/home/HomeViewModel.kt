package com.jbcbros.qbitremote.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jbcbros.qbitremote.data.model.ServerConfig
import com.jbcbros.qbitremote.data.model.Torrent
import com.jbcbros.qbitremote.data.model.TransferInfo
import com.jbcbros.qbitremote.data.repository.QbRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class FilterType(val value: String?) {
    data object All : FilterType(null)
    data object Downloading : FilterType("downloading")
    data object Completed : FilterType("completed")
}

data class HomeUiState(
    val torrents: List<Torrent> = emptyList(),
    val transferInfo: TransferInfo = TransferInfo(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val filter: FilterType = FilterType.All,
    val hasConfig: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: QbRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    init {
        viewModelScope.launch {
            val config = repository.serverConfig.firstOrNull() ?: ServerConfig()
            _uiState.value = _uiState.value.copy(hasConfig = config.host.isNotBlank())
            if (config.host.isNotBlank()) {
                repository.loadConfig()
                startPolling()
            }
        }
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(3000)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            if (!_uiState.value.isRefreshing) {
                _uiState.value = _uiState.value.copy(isRefreshing = true)
            }
            val config = repository.serverConfig.firstOrNull() ?: ServerConfig()
            if (config.host.isBlank()) {
                _uiState.value = _uiState.value.copy(isRefreshing = false, hasConfig = false)
                return@launch
            }

            repository.loadConfig()
            val transferInfo = repository.getTransferInfo() ?: TransferInfo()
            val torrents = repository.getTorrents(filter = _uiState.value.filter.value)
            _uiState.value = _uiState.value.copy(
                torrents = torrents,
                transferInfo = transferInfo,
                isRefreshing = false,
                hasConfig = true
            )
        }
    }

    fun setFilter(filter: FilterType) {
        _uiState.value = _uiState.value.copy(filter = filter)
        refresh()
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}
