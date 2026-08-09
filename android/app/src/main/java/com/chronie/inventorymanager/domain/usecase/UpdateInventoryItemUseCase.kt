package com.chronie.inventorymanager.domain.usecase

import com.chronie.inventorymanager.data.repository.InventoryRepository
import com.chronie.inventorymanager.domain.model.InventoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 更新库存物品用例
 */
class UpdateInventoryItemUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    suspend fun execute(item: InventoryItem) = withContext(Dispatchers.IO) {
        repository.updateItem(item)
    }
}