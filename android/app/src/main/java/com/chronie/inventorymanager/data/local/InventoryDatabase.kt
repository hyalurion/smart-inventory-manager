package com.chronie.inventorymanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.chronie.inventorymanager.domain.model.InventoryItem

/** 本地库存数据库 */
@Database(entities = [InventoryItem::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class InventoryDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao

    companion object {
        const val DATABASE_NAME = "inventory_database"
    }
}
