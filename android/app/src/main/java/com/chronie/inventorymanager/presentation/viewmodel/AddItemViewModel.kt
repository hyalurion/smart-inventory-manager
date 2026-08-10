package com.chronie.inventorymanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.chronie.inventorymanager.domain.model.InventoryItem
import com.chronie.inventorymanager.domain.usecase.AddInventoryItemUseCase
import com.chronie.inventorymanager.domain.usecase.GetInventoryItemsUseCase
import com.chronie.inventorymanager.data.network.ApiException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 添加物品页面UI状态 */
data class AddItemUiState(
    val categories: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

/** 添加物品页面ViewModel */
@HiltViewModel
class AddItemViewModel @Inject constructor(
    private val addInventoryItemUseCase: AddInventoryItemUseCase,
    private val getInventoryItemsUseCase: GetInventoryItemsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddItemUiState())
    val uiState: StateFlow<AddItemUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    /** 加载可用分类 */
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val items = getInventoryItemsUseCase.execute()
                val categories = items.mapNotNull { it.category }
                    .distinct()
                    .sorted()
                
                _uiState.value = _uiState.value.copy(categories = categories)
            } catch (e: Exception) {
                // 如果无法获取分类，使用默认分类
                val defaultCategories = emptyList<String>()
                _uiState.value = _uiState.value.copy(categories = defaultCategories)
            }
        }
    }

    /** 添加物品 */
    fun addItem(item: InventoryItem) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, isSuccess = false)

            try {
                addInventoryItemUseCase.execute(item)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true,
                    error = null
                )
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is ApiException -> e.message ?: "操作失败"
                    is java.net.UnknownHostException -> "网络连接失败: 无法访问服务器"
                    is java.net.SocketTimeoutException -> "网络请求超时: 请检查网络连接"
                    is java.net.ConnectException -> "服务器连接失败: 请确认服务器正在运行"
                    else -> "添加物品失败: ${e.message}"
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = false,
                    error = errorMessage
                )
            }
        }
    }

    /** 清除错误信息 */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /** 清除成功状态 */
    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(isSuccess = false)
    }
    
    /** 重置表单状态 */
    fun resetForm() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = null,
            isSuccess = false
        )
    }
}