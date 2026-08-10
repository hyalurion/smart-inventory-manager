package com.chronie.inventorymanager.data.local

import androidx.room.TypeConverter
import com.chronie.inventorymanager.domain.model.SyncStatus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

/** Room 类型转换器：将 InventoryItem 中的非基础类型映射为可存储的列 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    @TypeConverter
    fun fromSyncStatus(value: String?): SyncStatus =
        value?.let { runCatching { SyncStatus.valueOf(it) }.getOrDefault(SyncStatus.SYNCED) }
            ?: SyncStatus.SYNCED

    @TypeConverter
    fun syncStatusToString(status: SyncStatus): String = status.name

    @TypeConverter
    fun fromStringMap(value: String?): Map<String, String> {
        if (value.isNullOrEmpty()) return emptyMap()
        return try {
            gson.fromJson<Map<String, String>>(
                value,
                object : TypeToken<Map<String, String>>() {}.type
            )
        } catch (_: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun stringMapToString(map: Map<String, String>): String = gson.toJson(map)

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return try {
            gson.fromJson<List<String>>(
                value,
                object : TypeToken<List<String>>() {}.type
            )
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun stringListToString(list: List<String>): String = gson.toJson(list)
}
