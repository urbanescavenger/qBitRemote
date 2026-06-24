package com.jbcbros.qbitremote.ui.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jbcbros.qbitremote.R
import com.jbcbros.qbitremote.data.model.Torrent
import com.jbcbros.qbitremote.data.model.TorrentFile
import com.jbcbros.qbitremote.data.model.Tracker
import com.jbcbros.qbitremote.data.repository.QbRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val torrent: Torrent? = null,
    val isLoading: Boolean = false,
    val actionMessage: String? = null,
    val actionSuccess: Boolean = false,
    val connectionError: String? = null,
    val files: List<TorrentFile> = emptyList(),
    val trackers: List<Tracker> = emptyList()
)

@HiltViewModel
class TorrentDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: QbRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    init {
        viewModelScope.launch {
            repository.connectionError.collect { error ->
                if (error != null) {
                    _uiState.value = _uiState.value.copy(connectionError = error)
                }
            }
        }
    }

    fun loadTorrent(hash: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val torrents = repository.getTorrents()
            val torrent = torrents.find { it.hash == hash }
            _uiState.value = _uiState.value.copy(torrent = torrent, isLoading = false)
        }
    }

    /** Files/trackers are heavier and rarely change; load once on entry, not every poll. */
    fun loadFiles(hash: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(files = repository.getTorrentFiles(hash))
        }
    }

    fun loadTrackers(hash: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(trackers = repository.getTorrentTrackers(hash))
        }
    }

    fun startPolling(hash: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                loadTorrent(hash)
                delay(3000)
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    fun pauseTorrent(hash: String) {
        viewModelScope.launch {
            val success = repository.stopTorrent(hash)
            onActionResult(success, R.string.msg_paused)
            if (success) loadTorrent(hash)
        }
    }

    fun resumeTorrent(hash: String) {
        viewModelScope.launch {
            val success = repository.startTorrent(hash)
            onActionResult(success, R.string.msg_resumed)
            if (success) loadTorrent(hash)
        }
    }

    fun recheckTorrent(hash: String) {
        viewModelScope.launch {
            val success = repository.recheckTorrent(hash)
            onActionResult(success, R.string.msg_recheck_started)
            if (success) loadTorrent(hash)
        }
    }

    fun deleteTorrent(hash: String, deleteFiles: Boolean, onDeleted: () -> Unit) {
        viewModelScope.launch {
            val success = repository.deleteTorrent(hash, deleteFiles)
            _uiState.value = _uiState.value.copy(
                actionMessage = context.getString(
                    if (success) R.string.msg_deleted else R.string.msg_delete_failed
                ),
                actionSuccess = success
            )
            if (success) onDeleted()
        }
    }

    private fun onActionResult(success: Boolean, successRes: Int) {
        _uiState.value = _uiState.value.copy(
            actionMessage = context.getString(if (success) successRes else R.string.msg_action_failed),
            actionSuccess = success
        )
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }

    fun clearConnectionError() {
        _uiState.value = _uiState.value.copy(connectionError = null)
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
