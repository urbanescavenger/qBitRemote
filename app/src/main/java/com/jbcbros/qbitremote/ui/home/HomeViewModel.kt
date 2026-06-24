package com.jbcbros.qbitremote.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jbcbros.qbitremote.R
import com.jbcbros.qbitremote.data.model.ServerConfig
import com.jbcbros.qbitremote.data.model.Torrent
import com.jbcbros.qbitremote.data.model.TransferInfo
import com.jbcbros.qbitremote.data.repository.QbRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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

sealed class SortType(val value: String, val resId: Int) {
    data object Added : SortType("added_on", R.string.sort_added)
    data object Name : SortType("name", R.string.sort_name)
    data object Progress : SortType("progress", R.string.sort_progress)
    data object DlSpeed : SortType("dlspeed", R.string.sort_dlspeed)
    data object UpSpeed : SortType("upspeed", R.string.sort_upspeed)
    data object Ratio : SortType("ratio", R.string.sort_ratio)
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
    val snackbarMessage: String? = null,
    val categories: List<String> = emptyList(),
    val selectedCategory: String = "",
    val sort: SortType = SortType.Added,
    val sortReverse: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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
        // Surface connection errors (unreachable server / failed auth) as a state the UI renders.
        viewModelScope.launch {
            repository.connectionError.collect { error ->
                _uiState.value = _uiState.value.copy(error = error)
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
            allTorrents = repository.getTorrents(
                filter = _uiState.value.filter.value,
                sort = _uiState.value.sort.value,
                reverse = _uiState.value.sortReverse
            )
            val filtered = applySearch(allTorrents, _uiState.value.searchQuery)
            _uiState.value = _uiState.value.copy(
                torrents = filtered,
                transferInfo = transferInfo,
                isRefreshing = false,
                hasConfig = true
            )
            loadCategories()
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val cats = repository.getCategories()
            _uiState.value = _uiState.value.copy(categories = cats)
        }
    }

    fun setFilter(filter: FilterType) {
        _uiState.value = _uiState.value.copy(filter = filter)
        refresh()
    }

    fun setSort(sort: SortType) {
        _uiState.value = _uiState.value.copy(sort = sort)
        refresh()
    }

    fun toggleSortReverse() {
        _uiState.value = _uiState.value.copy(sortReverse = !_uiState.value.sortReverse)
        refresh()
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            torrents = applySearch(allTorrents, query)
        )
    }

    fun setCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        val filtered = if (category.isBlank()) {
            allTorrents
        } else {
            allTorrents.filter { it.category == category }
        }
        _uiState.value = _uiState.value.copy(
            torrents = applySearch(filtered, _uiState.value.searchQuery)
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
                snackbarMessage = context.getString(
                    if (success) R.string.msg_paused else R.string.msg_pause_failed
                )
            )
            if (success) refresh()
        }
    }

    fun resumeTorrent(hash: String) {
        viewModelScope.launch {
            val success = repository.startTorrent(hash)
            _uiState.value = _uiState.value.copy(
                snackbarMessage = context.getString(
                    if (success) R.string.msg_resumed else R.string.msg_resume_failed
                )
            )
            if (success) refresh()
        }
    }

    fun deleteTorrent(hash: String) {
        viewModelScope.launch {
            val success = repository.deleteTorrent(hash)
            _uiState.value = _uiState.value.copy(
                snackbarMessage = context.getString(
                    if (success) R.string.msg_deleted else R.string.msg_delete_failed
                )
            )
            if (success) refresh()
        }
    }

    fun pauseAll() {
        viewModelScope.launch {
            val success = repository.stopAllTorrents()
            _uiState.value = _uiState.value.copy(
                snackbarMessage = context.getString(
                    if (success) R.string.msg_paused_all else R.string.msg_action_failed
                )
            )
            if (success) refresh()
        }
    }

    fun resumeAll() {
        viewModelScope.launch {
            val success = repository.startAllTorrents()
            _uiState.value = _uiState.value.copy(
                snackbarMessage = context.getString(
                    if (success) R.string.msg_resumed_all else R.string.msg_action_failed
                )
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
