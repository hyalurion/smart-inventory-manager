package com.chronie.inventorymanager.domain.usecase

import com.chronie.inventorymanager.data.repository.InventoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 删除库存物品用例
 */
class DeleteInventoryItemUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    suspend fun execute(itemId: String) = withContext(Dispatchers.IO) {
        repository.deleteItem(itemId)
    }
}