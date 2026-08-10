package com.chronie.inventorymanager.domain.usecase

import com.chronie.inventorymanager.data.repository.InventoryRepository
import com.chronie.inventorymanager.domain.model.InventoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 使用库存物品用例（减少库存数量）
 */
class UseInventoryItemUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    suspend fun execute(itemId: String): InventoryItem = withContext(Dispatchers.IO) {
        println("UseInventoryItemUseCase: 调用 repository.useItem - itemId: $itemId")
        // 调用 repository 的 useItem 方法，该方法会调用 API 端点 /api/inventory/items/{id}/use
        val result = repository.useItem(itemId)
        println("UseInventoryItemUseCase: repository.useItem 返回成功")
        result
    }
}