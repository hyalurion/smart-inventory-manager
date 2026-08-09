package com.chronie.inventorymanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.chronie.inventorymanager.domain.model.InventoryItem
import com.chronie.inventorymanager.domain.model.StatusFilter
import com.chronie.inventorymanager.domain.model.SortOption
import com.chronie.inventorymanager.domain.model.StockStatistics
import com.chronie.inventorymanager.domain.model.UnifiedFilter
import com.chronie.inventorymanager.domain.model.getStockStatus
import com.chronie.inventorymanager.domain.usecase.DeleteInventoryItemUseCase
import com.chronie.inventorymanager.domain.usecase.GetInventoryItemsUseCase
import com.chronie.inventorymanager.domain.usecase.GetStatisticsUseCase
import com.chronie.inventorymanager.domain.usecase.UpdateInventoryItemUseCase
import com.chronie.inventorymanager.domain.usecase.AddInventoryItemUseCase
import com.chronie.inventorymanager.domain.usecase.UseInventoryItemUseCase
import com.chronie.inventorymanager.data.network.ApiException
import java.util.Date
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 库存页面ViewModel 管理库存页面的UI状态和业务逻辑 */
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val getInventoryItemsUseCase: GetInventoryItemsUseCase,
    private val addInventoryItemUseCase: AddInventoryItemUseCase,
    private val getStatisticsUseCase: GetStatisticsUseCase,
    private val updateInventoryItemUseCase: UpdateInventoryItemUseCase,
    private val deleteInventoryItemUseCase: DeleteInventoryItemUseCase,
    private val useInventoryItemUseCase: UseInventoryItemUseCase
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    // 错误信息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** 加载库存数据 */
    fun loadInventory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val items = getInventoryItemsUseCase.execute()

                // 空列表是正常状态（服务器正常但暂无数据），不应误判为连接失败。
                // 真正的连接异常会在下方 catch 中按类型（UnknownHost/Connect/Timeout/ApiException）给出具体提示。
                val categories = items.mapNotNull { it.category }.distinct().sorted()
                _uiState.value = _uiState.value.copy(
                    items = items,
                    availableCategories = categories,
                    isLoading = false
                )
                // 重新应用筛选器
                applyFilters()
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is ApiException -> e.message ?: "操作失败"
                    is java.net.UnknownHostException -> "网络连接失败: 无法访问服务器 ${getBaseUrl()}"
                    is java.net.SocketTimeoutException -> "网络请求超时: 请检查网络连接"
                    is java.net.ConnectException -> "服务器连接失败: 请确认服务器 ${getBaseUrl()} 正在运行"
                    else -> "加载数据失败: ${e.message}"
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMessage
                )
            }
        }
    }
    
    /** 获取服务器地址 */
    private fun getBaseUrl(): String = "http://192.168.10.9:5000"

    /** 加载统计数据 */
    fun loadStatistics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingStats = true)

            try {
                val statistics = getStatisticsUseCase.execute()
                _uiState.value =
                        _uiState.value.copy(statistics = statistics, isLoadingStats = false)
            } catch (e: Exception) {
                _uiState.value =
                        _uiState.value.copy(isLoadingStats = false, error = (e as? ApiException)?.message ?: "加载统计失败: ${e.message}")
            }
        }
    }

    /** 刷新数据 */
    fun refreshData() {
        loadInventory()
        loadStatistics()
    }

    /** 更新搜索查询 */
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    /** 更新分类筛选 */
    fun updateCategory(category: String?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        applyFilters()
    }

    /** 更新状态筛选 */
    fun updateStatusFilter(statusFilter: StatusFilter) {
        _uiState.value = _uiState.value.copy(statusFilter = statusFilter)
        applyFilters()
    }

    /** 清除所有筛选 */
    fun clearFilters() {
        _uiState.value =
                _uiState.value.copy(
                        searchQuery = "",
                        selectedCategory = null,
                        statusFilter = StatusFilter.ALL,
                        sortOption = SortOption.NAME_ASC
                )
        applyFilters()
    }

    /** 更新排序选项 */
    fun updateSortOption(sortOption: SortOption) {
        _uiState.value = _uiState.value.copy(sortOption = sortOption)
        applyFilters()
    }

    /** 处理统一筛选变更 */
    fun onUnifiedFilterChanged(filter: UnifiedFilter) {
        _uiState.value = _uiState.value.copy(
            selectedCategory = filter.selectedCategories.firstOrNull(), // 统一筛选支持多分类，但UI中只取第一个
            statusFilter = filter.statusFilter,
            sortOption = filter.sortOption
        )
        applyFilters()
    }

    /** 使用物品（减少库存） */
    fun useItem(item: InventoryItem) {
        viewModelScope.launch {
            try {
                println("InventoryViewModel: 开始使用物品 - ID: ${item.id}, 名称: ${item.name}")
                
                // 调用 useInventoryItemUseCase，该方法会调用 API 端点 /api/inventory/items/{id}/use
                val updatedItem = useInventoryItemUseCase.execute(item.id)
                
                println("InventoryViewModel: 物品使用成功 - 更新后数量: ${updatedItem.quantity}")

                // 更新本地状态
                val updatedItems =
                        _uiState.value.items.map {
                            if (it.id == updatedItem.id) updatedItem else it
                        }
                _uiState.value = _uiState.value.copy(items = updatedItems)

                // 重新应用筛选器
                applyFilters()

                // 重新加载统计数据
                loadStatistics()
            } catch (e: Exception) {
                println("InventoryViewModel: 使用物品失败 - ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(error = (e as? ApiException)?.message ?: "使用物品失败: ${e.message}")
            }
        }
    }

    /** 删除物品 */
    fun deleteItem(item: InventoryItem) {
        viewModelScope.launch {
            try {
                deleteInventoryItemUseCase.execute(item.id)

                // 从本地状态中移除
                val updatedItems = _uiState.value.items.filter { it.id != item.id }
                _uiState.value = _uiState.value.copy(items = updatedItems)

                // 重新应用筛选器
                applyFilters()

                // 重新加载统计数据
                loadStatistics()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = (e as? ApiException)?.message ?: "删除物品失败: ${e.message}")
            }
        }
    }

    /** 添加物品（调用后端 API 并更新本地状态） */
    fun addItem(item: InventoryItem, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val created = addInventoryItemUseCase.execute(item)

                // 将新物品加入本地状态
                val updatedItems = _uiState.value.items + created
                _uiState.value = _uiState.value.copy(items = updatedItems)

                // 重新应用筛选器和刷新统计
                applyFilters()
                loadStatistics()

                onResult(true, null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = (e as? ApiException)?.message ?: "添加物品失败: ${e.message}")
                onResult(false, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** 更新物品（调用后端 API 并更新本地状态） */
    fun updateItem(item: InventoryItem, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val updated = updateInventoryItemUseCase.execute(item)

                val updatedItems = _uiState.value.items.map {
                    if (it.id == updated.id) updated else it
                }
                _uiState.value = _uiState.value.copy(items = updatedItems)

                applyFilters()
                loadStatistics()

                onResult(true, null)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = (e as? ApiException)?.message ?: "更新物品失败: ${e.message}")
                onResult(false, e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** 编辑物品 */
    fun editItem(item: InventoryItem) {
        // 这个方法通常会导航到编辑页面
        // 在实际实现中，这里会触发导航事件
    }

    /** 清除错误 */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /** 应用筛选器 */
    private fun applyFilters() {
        val currentState = _uiState.value
        val searchQuery = currentState.searchQuery
        val selectedCategory = currentState.selectedCategory
        val statusFilter = currentState.statusFilter
        val sortOption = currentState.sortOption

        val filteredItems =
                currentState.items.filter { item ->
                    // 搜索过滤
                    val matchesSearch =
                            if (searchQuery.isEmpty()) {
                                true
                            } else {
                                item.name.contains(searchQuery, ignoreCase = true) ||
                                        item.description.contains(
                                                searchQuery,
                                                ignoreCase = true
                                        )
                            }

                    // 分类过滤
                    val matchesCategory =
                            if (selectedCategory == null) {
                                true
                            } else {
                                item.category == selectedCategory
                            }

                    // 状态过滤
                    val matchesStatus =
                            when (statusFilter) {
                                StatusFilter.ALL -> true
                                StatusFilter.NORMAL ->
                                        item.getStockStatus() ==
                                                com.chronie.inventorymanager.domain.model
                                                        .StockStatus.NORMAL
                                StatusFilter.LOW_STOCK ->
                                        item.getStockStatus() ==
                                                com.chronie.inventorymanager.domain.model
                                                        .StockStatus.LOW_STOCK
                                StatusFilter.EXPIRING_SOON ->
                                        item.getStockStatus() ==
                                                com.chronie.inventorymanager.domain.model
                                                        .StockStatus.EXPIRING_SOON
                                StatusFilter.EXPIRED ->
                                        item.getStockStatus() ==
                                                com.chronie.inventorymanager.domain.model
                                                        .StockStatus.EXPIRED
                            }

                    matchesSearch && matchesCategory && matchesStatus
                }

        // 应用排序
        val sortedItems = filteredItems.sortedWith(compareBy<InventoryItem> { item ->
            when (sortOption) {
                SortOption.NAME_ASC -> item.name.lowercase()
                SortOption.NAME_DESC -> item.name.lowercase()
                SortOption.QUANTITY_ASC -> item.quantity.toString().padStart(10, '0')
                SortOption.QUANTITY_DESC -> item.quantity.toString().padStart(10, '0')
                SortOption.DATE_ADDED_ASC -> item.createdAt.toString()
                SortOption.DATE_ADDED_DESC -> item.createdAt.toString()
                SortOption.EXPIRY_DATE_ASC -> item.expiryDate?.toString() ?: ""
                SortOption.EXPIRY_DATE_DESC -> item.expiryDate?.toString() ?: ""
                SortOption.UPDATED_AT_ASC -> item.updatedAt.toString()
                SortOption.UPDATED_AT_DESC -> item.updatedAt.toString()
            }
        }.let { comparator ->
            when (sortOption) {
                SortOption.NAME_ASC, SortOption.QUANTITY_ASC, SortOption.DATE_ADDED_ASC, 
                     SortOption.EXPIRY_DATE_ASC, SortOption.UPDATED_AT_ASC -> comparator
                SortOption.NAME_DESC, SortOption.QUANTITY_DESC, SortOption.DATE_ADDED_DESC,
                SortOption.EXPIRY_DATE_DESC, SortOption.UPDATED_AT_DESC -> comparator.reversed()
            }
        })

        _uiState.value = _uiState.value.copy(filteredItems = sortedItems)
    }
}

/** 库存页面UI状态数据类 */
data class InventoryUiState(
        val items: List<InventoryItem> = emptyList(),
        val filteredItems: List<InventoryItem> = emptyList(),
        val availableCategories: List<String> = emptyList(),
        val statistics: StockStatistics? = null,
        val searchQuery: String = "",
        val selectedCategory: String? = null,
        val statusFilter: StatusFilter = StatusFilter.ALL,
        val sortOption: SortOption = SortOption.NAME_ASC,
        val isLoading: Boolean = false,
        val isLoadingStats: Boolean = false,
        val error: String? = null
)
