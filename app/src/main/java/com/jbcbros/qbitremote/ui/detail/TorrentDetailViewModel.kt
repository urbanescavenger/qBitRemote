package com.jbcbros.qbitremote.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jbcbros.qbitremote.data.model.Torrent
import com.jbcbros.qbitremote.data.repository.QbRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val torrent: Torrent? = null,
    val isLoading: Boolean = false,
    val actionMessage: String? = null
)

@HiltViewModel
class TorrentDetailViewModel @Inject constructor(
    private val repository: QbRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadTorrent(hash: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val torrents = repository.getTorrents()
            val torrent = torrents.find { it.hash == hash }
            _uiState.value = _uiState.value.copy(torrent = torrent, isLoading = false)
        }
    }

    fun pauseTorrent(hash: String) {
        viewModelScope.launch {
            val success = repository.stopTorrent(hash)
            _uiState.value = _uiState.value.copy(actionMessage = if (success) "已暂停" else "操作失败")
            if (success) loadTorrent(hash)
        }
    }

    fun resumeTorrent(hash: String) {
        viewModelScope.launch {
            val success = repository.startTorrent(hash)
            _uiState.value = _uiState.value.copy(actionMessage = if (success) "已恢复" else "操作失败")
            if (success) loadTorrent(hash)
        }
    }

    fun recheckTorrent(hash: String) {
        viewModelScope.launch {
            val success = repository.recheckTorrent(hash)
            _uiState.value = _uiState.value.copy(actionMessage = if (success) "开始校验" else "操作失败")
            if (success) loadTorrent(hash)
        }
    }

    fun deleteTorrent(hash: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            val success = repository.deleteTorrent(hash)
            _uiState.value = _uiState.value.copy(actionMessage = if (success) "已删除" else "删除失败")
            if (success) onDeleted()
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }
}
