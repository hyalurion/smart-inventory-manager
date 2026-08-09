package com.chronie.inventorymanager.data.repository

import com.chronie.inventorymanager.data.local.InventoryDao
import com.chronie.inventorymanager.data.network.*
import com.chronie.inventorymanager.domain.model.InventoryItem
import com.chronie.inventorymanager.domain.model.StatusFilter
import com.chronie.inventorymanager.domain.model.StockStatistics
import com.smartinventory.models.PurchaseListResponse
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * 库存数据仓库：负责与后端 API 交互，并借助 Room 提供本地缓存与离线能力。
 * - 列表/详情优先走网络并回写本地；网络失败或后端报错时回退到本地缓存。
 * - 写操作（增/改/删/使用）采用 write-through：成功后再同步本地库。
 */
class InventoryRepository @Inject constructor(
    private val apiService: InventoryApiService,
    private val inventoryDao: InventoryDao
) {

    /** 获取所有库存物品（网络优先 + 本地缓存 + 离线回退） */
    suspend fun getAllItems(): List<InventoryItem> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getItems()
                if (response.isSuccessful) {
                    val items = response.body()?.data ?: emptyList()
                    inventoryDao.insertAll(items)
                    items
                } else {
                    inventoryDao.getAll()
                }
            } catch (e: Exception) {
                inventoryDao.getAll()
            }
        }

    /** 根据搜索条件获取物品（失败时回退到本地缓存） */
    suspend fun getItemsByFilter(
        searchQuery: String = "",
        category: String? = null,
        statusFilter: StatusFilter = StatusFilter.ALL,
        page: Int = 1,
        limit: Int = 100
    ): List<InventoryItem> =
        withContext(Dispatchers.IO) {
            try {
                val response =
                    apiService.getItems(
                        search = searchQuery,
                        category = category ?: "",
                        page = page,
                        perPage = limit
                    )
                if (response.isSuccessful) {
                    val items = response.body()?.data ?: emptyList()
                    inventoryDao.insertAll(items)
                    items
                } else {
                    inventoryDao.getAll()
                }
            } catch (e: Exception) {
                inventoryDao.getAll()
            }
        }

    /** 根据ID获取单个物品（失败时回退到本地缓存） */
    suspend fun getItemById(id: String): InventoryItem? =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getItem(id)
                if (response.isSuccessful) {
                    response.body()?.data
                } else {
                    inventoryDao.getById(id)
                }
            } catch (e: Exception) {
                inventoryDao.getById(id)
            }
        }

    /** 创建新物品 */
    suspend fun createItem(item: InventoryItem): InventoryItem =
        withContext(Dispatchers.IO) {
            val response = apiService.createItem(item)
            if (response.isSuccessful && response.body()?.data != null) {
                val created = response.body()!!.data
                inventoryDao.insert(created)
                created
            } else {
                throw response.apiError()
            }
        }

    /** 更新物品 */
    suspend fun updateItem(item: InventoryItem): InventoryItem =
        withContext(Dispatchers.IO) {
            val response = apiService.updateItem(item.id, item)
            if (response.isSuccessful && response.body()?.data != null) {
                val updated = response.body()!!.data
                inventoryDao.insert(updated)
                updated
            } else {
                throw response.apiError()
            }
        }

    /** 删除物品 */
    suspend fun deleteItem(id: String): Boolean =
        withContext(Dispatchers.IO) {
            val response = apiService.deleteItem(id)
            if (response.isSuccessful) {
                inventoryDao.delete(id)
                true
            } else {
                throw response.apiError()
            }
        }

    /** 使用物品（减少库存数量） */
    suspend fun useItem(id: String): InventoryItem =
        withContext(Dispatchers.IO) {
            val response = apiService.useItem(id)
            if (response.isSuccessful && response.body()?.data != null) {
                val item = response.body()!!.data
                inventoryDao.insert(item)
                item
            } else {
                throw response.apiError()
            }
        }

    /** 获取库存统计数据 */
    suspend fun getStatistics(): StockStatistics =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getStatistics()
                if (response.isSuccessful && response.body()?.data != null) {
                    response.body()!!.data
                } else {
                    StockStatistics(0, 0, 0, 0, 0)
                }
            } catch (e: Exception) {
                StockStatistics(0, 0, 0, 0, 0)
            }
        }

    /** 获取所有分类 */
    suspend fun getCategories(): List<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getCategories()
                if (response.isSuccessful) {
                    response.body()?.data ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

    /** 批量同步数据（占位：暂未对接后端批量接口） */
    suspend fun syncData(items: List<InventoryItem>): Boolean =
        withContext(Dispatchers.IO) {
            true
        }

    /** 批量调整库存数量（占位：暂未对接后端批量接口） */
    suspend fun batchUpdateQuantities(updates: Map<String, Int>): Boolean =
        withContext(Dispatchers.IO) {
            true
        }

    /** 批量删除（对齐 Go 后端 operation=delete，使用 int ID） */
    suspend fun batchDelete(ids: List<String>): Boolean =
        withContext(Dispatchers.IO) {
            val intIds = ids.mapNotNull { it.toIntOrNull() }
            if (intIds.isEmpty()) return@withContext false
            apiService.batch(BatchRequest("delete", intIds)).isSuccessful
        }

    /** 批量修改分类（对齐 Go 后端 operation=update_category，使用 int ID） */
    suspend fun batchUpdateCategory(ids: List<String>, category: String): Boolean =
        withContext(Dispatchers.IO) {
            val intIds = ids.mapNotNull { it.toIntOrNull() }
            if (intIds.isEmpty()) return@withContext false
            apiService.batch(BatchRequest("update_category", intIds, category)).isSuccessful
        }

    /** 将后端错误响应解析为带 error_code / error_data 的结构化异常 */
    private fun <T> Response<T>.apiError(): ApiException {
        val envelope = runCatching {
            errorBody()?.string()?.let { Gson().fromJson(it, ApiErrorEnvelope::class.java) }
        }.getOrNull()
        val code = envelope?.error_code
        val data = envelope?.error_data
        val message = when (code) {
            "insufficientStock" -> "库存不足：${data?.get("itemName") ?: ""}".trimEnd('：')
            else -> "操作失败${code?.let { "（$it）" } ?: ""}"
        }
        return ApiException(code, message)
    }

    private data class ApiErrorEnvelope(
        val error_code: String? = null,
        val error_data: Map<String, Any?>? = null
    )
}

/** 公共 API 工厂类 */
object ApiFactory {
    private const val BASE_URL = "http://192.168.10.9:5000/"

    fun create(): InventoryApiService {
        val logging = okhttp3.logging.HttpLoggingInterceptor().apply {
            level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
        }

        val client = okhttp3.OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        // 自定义Gson解析器，支持ISO 8601日期格式
        val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
            .create()

        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create(gson))
            .client(client)
            .build()

        return retrofit.create(InventoryApiService::class.java)
    }
}

class MockInventoryApiService : InventoryApiService {
    override suspend fun getItems(
        search: String,
        category: String,
        showExpired: Boolean,
        page: Int,
        perPage: Int
    ): Response<InventoryItemsResponse> {
        return Response.success(
            InventoryItemsResponse(true, emptyList(), PaginationInfo(1, 1, 50, 0))
        )
    }

    override suspend fun getItem(id: String): Response<SingleItemResponse> {
        return Response.error(404, "".toResponseBody(null))
    }

    override suspend fun createItem(item: InventoryItem): Response<SingleItemResponse> {
        return Response.success(SingleItemResponse(true, item))
    }

    override suspend fun updateItem(id: String, item: InventoryItem): Response<SingleItemResponse> {
        return Response.success(SingleItemResponse(true, item))
    }

    override suspend fun deleteItem(id: String): Response<DeleteResponse> {
        return Response.success(DeleteResponse(true, "Deleted"))
    }

    override suspend fun useItem(id: String): Response<SingleItemResponse> {
        return Response.error(404, "".toResponseBody(null))
    }

    override suspend fun getStatistics(): Response<StatisticsResponse> {
        return Response.success(StatisticsResponse(true, StockStatistics(0, 0, 0, 0, 0)))
    }

    override suspend fun getCategories(): Response<CategoriesResponse> {
        return Response.success(CategoriesResponse(true, emptyList()))
    }

    override suspend fun batch(request: BatchRequest): Response<BatchResponse> {
        return Response.success(BatchResponse(true, "Success"))
    }

    override suspend fun getPurchaseList(): Response<PurchaseListResponse> {
        return Response.success(PurchaseListResponse(true, emptyList(), ""))
    }
}
