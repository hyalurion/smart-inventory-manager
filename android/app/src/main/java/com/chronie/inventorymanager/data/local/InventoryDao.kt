package com.chronie.inventorymanager.data.local

import androidx.room.*
import com.chronie.inventorymanager.domain.model.InventoryItem

/** 库存物品本地访问对象（Room DAO 骨架） */
@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items")
    suspend fun getAll(): List<InventoryItem>

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getById(id: String): InventoryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InventoryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<InventoryItem>)

    @Update
    suspend fun update(item: InventoryItem)

    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM inventory_items")
    suspend fun clear()
}
