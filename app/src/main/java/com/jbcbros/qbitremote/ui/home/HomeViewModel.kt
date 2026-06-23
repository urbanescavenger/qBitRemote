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
    val error: String? = null,
    val searchQuery: String = "",
    val snackbarMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: QbRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var allTorrents: List<Torrent> = emptyList()

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
            allTorrents = repository.getTorrents(filter = _uiState.value.filter.value)
            val filtered = applySearch(allTorrents, _uiState.value.searchQuery)
            _uiState.value = _uiState.value.copy(
                torrents = filtered,
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

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            torrents = applySearch(allTorrents, query)
        )
    }

    private fun applySearch(torrents: List<Torrent>, query: String): List<Torrent> {
        if (query.isBlank()) return torrents
        val lowerQuery = query.lowercase()
        return torrents.filter { it.name.lowercase().contains(lowerQuery) }
    }

    fun pauseTorrent(hash: String) {
        viewModelScope.launch {
            val success = repository.stopTorrent(hash)
            _uiState.value = _uiState.value.copy(
                snackbarMessage = if (success) "已暂停" else "暂停失败"
            )
            if (success) refresh()
        }
    }

    fun resumeTorrent(hash: String) {
        viewModelScope.launch {
            val success = repository.startTorrent(hash)
            _uiState.value = _uiState.value.copy(
                snackbarMessage = if (success) "已恢复" else "恢复失败"
            )
            if (success) refresh()
        }
    }

    fun deleteTorrent(hash: String) {
        viewModelScope.launch {
            val success = repository.deleteTorrent(hash)
            _uiState.value = _uiState.value.copy(
                snackbarMessage = if (success) "已删除" else "删除失败"
            )
            if (success) refresh()
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}
