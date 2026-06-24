package com.jbcbros.qbitremote.ui.upload

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jbcbros.qbitremote.R
import com.jbcbros.qbitremote.data.repository.QbRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UploadUiState(
    val magnetUrl: String = "",
    val selectedCategory: String = "uncategorized",
    val categories: List<String> = listOf("uncategorized"),
    val availableTags: List<String> = emptyList(),
    val selectedTags: Set<String> = emptySet(),
    val isSubmitting: Boolean = false,
    val resultMessage: String? = null,
    val addSuccess: Boolean = false
)

@HiltViewModel
class UploadViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: QbRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
        loadTags()
    }

    fun loadCategories() {
        viewModelScope.launch {
            val cats = repository.getCategories()
            _uiState.value = _uiState.value.copy(
                categories = listOf("uncategorized") + cats
            )
        }
    }

    fun loadTags() {
        viewModelScope.launch {
            val tags = repository.getTags()
            _uiState.value = _uiState.value.copy(availableTags = tags)
        }
    }

    fun toggleTag(tag: String) {
        _uiState.value = _uiState.value.copy(
            selectedTags = if (tag in _uiState.value.selectedTags) {
                _uiState.value.selectedTags - tag
            } else {
                _uiState.value.selectedTags + tag
            }
        )
    }

    fun readClipboard(context: Context): String {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
        _uiState.value = _uiState.value.copy(magnetUrl = text)
        return text
    }

    fun setMagnet(url: String) {
        _uiState.value = _uiState.value.copy(magnetUrl = url)
    }

    fun setCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun addByUrl(urls: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, resultMessage = null)
            val trimmed = urls.trim()
            if (trimmed.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    resultMessage = context.getString(R.string.msg_empty_url)
                )
                return@launch
            }
            val category = if (_uiState.value.selectedCategory == "uncategorized") null else _uiState.value.selectedCategory
            val tags = _uiState.value.selectedTags.joinToString(",").ifBlank { null }
            val success = repository.addTorrentByUrl(trimmed, category, tags)
            _uiState.value = _uiState.value.copy(
                isSubmitting = false,
                resultMessage = if (success) context.getString(R.string.msg_add_success)
                    else context.getString(R.string.msg_add_failed) +
                        (repository.connectionError.value?.let { ": $it" } ?: ""),
                addSuccess = success
            )
        }
    }

    fun addByFile(uri: Uri, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, resultMessage = null)
            val category = if (_uiState.value.selectedCategory == "uncategorized") null else _uiState.value.selectedCategory
            val tags = _uiState.value.selectedTags.joinToString(",").ifBlank { null }
            val success = repository.addTorrentFile(uri, category, tags)
            _uiState.value = _uiState.value.copy(
                isSubmitting = false,
                resultMessage = if (success) context.getString(R.string.msg_add_success)
                    else context.getString(R.string.msg_add_failed) +
                        (repository.connectionError.value?.let { ": $it" } ?: ""),
                addSuccess = success
            )
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(resultMessage = null)
    }
}
