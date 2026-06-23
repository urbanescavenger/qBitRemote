package com.jbcbros.qbitremote.ui.upload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jbcbros.qbitremote.data.repository.QbRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UploadUiState(
    val magnetUrl: String = "",
    val selectedCategory: String = "uncategorized",
    val categories: List<String> = listOf("uncategorized"),
    val isSubmitting: Boolean = false,
    val resultMessage: String? = null
)

@HiltViewModel
class UploadViewModel @Inject constructor(
    private val repository: QbRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            // qBittorrent maindata API 返回复杂结构，简化处理
            // 实际项目中可以解析出分类列表
            _uiState.value = _uiState.value.copy(categories = listOf("uncategorized"))
        }
    }

    fun setMagnet(url: String) {
        _uiState.value = _uiState.value.copy(magnetUrl = url)
    }

    fun setCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun addByUrl(urls: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true)
            val trimmed = urls.trim()
            if (trimmed.isEmpty()) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, resultMessage = "未提供链接")
                return@launch
            }
            val category = if (_uiState.value.selectedCategory == "uncategorized") null else _uiState.value.selectedCategory
            val success = repository.addTorrentByUrl(trimmed, category)
            _uiState.value = _uiState.value.copy(
                isSubmitting = false,
                resultMessage = if (success) "添加成功" else "添加失败"
            )
            if (success) onSuccess()
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(resultMessage = null)
    }
}
