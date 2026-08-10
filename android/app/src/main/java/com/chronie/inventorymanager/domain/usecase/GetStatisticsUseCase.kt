package com.chronie.inventorymanager.domain.usecase

import com.chronie.inventorymanager.data.repository.InventoryRepository
import com.chronie.inventorymanager.domain.model.StockStatistics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 获取库存统计数据用例
 */
class GetStatisticsUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    suspend fun execute(): StockStatistics = withContext(Dispatchers.IO) {
        repository.getStatistics()
    }
}