package com.chronie.inventorymanager.domain.usecase

import com.chronie.inventorymanager.data.repository.InventoryRepository
import com.chronie.inventorymanager.domain.model.InventoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 获取库存物品列表用例
 */
class GetInventoryItemsUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    suspend fun execute(): List<InventoryItem> = withContext(Dispatchers.IO) {
        repository.getAllItems()
    }
}