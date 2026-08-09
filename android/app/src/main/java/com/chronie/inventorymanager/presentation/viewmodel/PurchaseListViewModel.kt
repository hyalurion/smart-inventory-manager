package com.chronie.inventorymanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.chronie.inventorymanager.data.network.InventoryApiService
import com.smartinventory.models.PurchaseListItem
import com.smartinventory.models.PurchaseListResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Inject

/**
 * Purchase List ViewModel
 * Handles business logic for purchase list functionality
 */
@HiltViewModel
class PurchaseListViewModel @Inject constructor(
    private val inventoryApiService: InventoryApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<PurchaseListUiState>(PurchaseListUiState.Loading)
    val uiState: StateFlow<PurchaseListUiState> = _uiState.asStateFlow()

    private val _purchaseList = MutableStateFlow<List<PurchaseListItem>>(emptyList())
    val purchaseList: StateFlow<List<PurchaseListItem>> = _purchaseList.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadPurchaseList()
    }

    /**
     * Load purchase list from API
     */
    fun loadPurchaseList() {
        viewModelScope.launch {
            try {
                _uiState.value = PurchaseListUiState.Loading
                val response: Response<PurchaseListResponse> = inventoryApiService.getPurchaseList()
                
                if (response.isSuccessful) {
                    val purchaseListResponse = response.body()
                    if (purchaseListResponse != null && purchaseListResponse.success) {
                        val items = purchaseListResponse.data ?: emptyList()
                        _purchaseList.value = items
                        _uiState.value = if (items.isEmpty()) {
                            PurchaseListUiState.Empty
                        } else {
                            PurchaseListUiState.Success(items)
                        }
                    } else {
                        _uiState.value = PurchaseListUiState.Error(
                            purchaseListResponse?.error ?: "Unknown error"
                        )
                    }
                } else {
                    _uiState.value = PurchaseListUiState.Error("Network error: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = PurchaseListUiState.Error(e.message ?: "Network error")
            }
        }
    }

    /**
     * Refresh purchase list (pull-to-refresh)
     */
    fun refreshList() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadPurchaseList()
            _isRefreshing.value = false
        }
    }

    /**
     * Filter purchase list by category
     */
    fun filterByCategory(category: String) {
        val allItems = _purchaseList.value
        val filteredItems = if (category.isEmpty()) {
            allItems
        } else {
            allItems.filter { it.category == category }
        }
        
        _uiState.value = if (filteredItems.isEmpty()) {
            PurchaseListUiState.Empty
        } else {
            PurchaseListUiState.Success(filteredItems)
        }
    }

    /**
     * Get all unique categories from purchase list
     */
    fun getCategories(): List<String> {
        return _purchaseList.value.map { it.category }.distinct().sorted()
    }

    /**
     * Get total number of items to purchase
     */
    fun getTotalItemsCount(): Int {
        return _purchaseList.value.size
    }

    /**
     * Get total suggested quantity across all items
     */
    fun getTotalSuggestedQuantity(): Int {
        return _purchaseList.value.sumOf { it.suggestedQuantity }
    }

    /**
     * Get critically low items count
     */
    fun getCriticallyLowItemsCount(): Int {
        return _purchaseList.value.count { it.isCriticallyLow() }
    }

    /**
     * Get items grouped by category
     */
    fun getItemsByCategory(): Map<String, List<PurchaseListItem>> {
        return _purchaseList.value.groupBy { it.category }
    }
}

/**
 * Purchase List UI State
 */
sealed class PurchaseListUiState {
    data object Loading : PurchaseListUiState()
    data class Success(val items: List<PurchaseListItem>) : PurchaseListUiState()
    data object Empty : PurchaseListUiState()
    data class Error(val message: String) : PurchaseListUiState()
}