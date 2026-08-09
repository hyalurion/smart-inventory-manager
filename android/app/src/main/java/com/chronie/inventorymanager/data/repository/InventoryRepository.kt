package com.chronie.inventorymanager.data.repository

import com.chronie.inventorymanager.data.network.*
import com.chronie.inventorymanager.domain.model.InventoryItem
import com.chronie.inventorymanager.domain.model.StatusFilter
import com.chronie.inventorymanager.domain.model.StockStatistics
import com.smartinventory.models.PurchaseListResponse
import com.google.gson.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

/** 库存数据仓库 负责与后端API交互，管理本地缓存 */
class InventoryRepository(
    private val apiService: InventoryApiService = ApiFactory.create()
) {

    /** 获取所有库存物品 */
    suspend fun getAllItems(): List<InventoryItem> =
            withContext(Dispatchers.IO) {
                try {
                    val response = apiService.getItems()
                    if (response.isSuccessful) {
                        response.body()?.data ?: emptyList()
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    // 改进的错误处理 - 记录网络连接问题
                    when (e) {
                        is java.net.UnknownHostException -> {
                            println("网络连接失败: 无法访问服务器")
                        }
                        is java.net.SocketTimeoutException -> {
                            println("网络请求超时: 请检查网络连接")
                        }
                        is java.net.ConnectException -> {
                            println("网络连接异常: 服务器可能无法访问")
                        }
                        else -> {
                            println("获取数据失败: ${e.message}")
                        }
                    }
                    emptyList()
                }
            }

    /** 根据搜索条件获取物品 */
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
                        response.body()?.data ?: emptyList()
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }

    /** 根据ID获取单个物品 */
    suspend fun getItemById(id: String): InventoryItem? =
            withContext(Dispatchers.IO) {
                try {
                    val response = apiService.getItem(id)
                    if (response.isSuccessful) {
                        response.body()?.data
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }

    /** 创建新物品 */
    suspend fun createItem(item: InventoryItem): InventoryItem =
            withContext(Dispatchers.IO) {
                try {
                    val response = apiService.createItem(item)
                    if (response.isSuccessful && response.body()?.data != null) {
                        response.body()!!.data
                    } else {
                        throw Exception("Failed to create item")
                    }
                } catch (e: Exception) {
                    throw Exception("操作失败: ${e.message}")
                }
            }

    /** 更新物品 */
    suspend fun updateItem(item: InventoryItem): InventoryItem =
            withContext(Dispatchers.IO) {
                try {
                    val response = apiService.updateItem(item.id, item)
                    if (response.isSuccessful && response.body()?.data != null) {
                        response.body()!!.data
                    } else {
                        throw Exception("Failed to update item")
                    }
                } catch (e: Exception) {
                    throw Exception("操作失败: ${e.message}")
                }
            }

    /** 删除物品 */
    suspend fun deleteItem(id: String): Boolean =
            withContext(Dispatchers.IO) {
                try {
                    val response = apiService.deleteItem(id)
                    response.isSuccessful
                } catch (e: Exception) {
                    throw Exception("操作失败: ${e.message}")
                }
            }

    /** 使用物品（减少库存数量） */
    suspend fun useItem(id: String): InventoryItem =
            withContext(Dispatchers.IO) {
                try {
                    println("InventoryRepository: 调用 API - useItem(id: $id)")
                    val response = apiService.useItem(id)
                    println("InventoryRepository: API 响应 - isSuccessful: ${response.isSuccessful}, code: ${response.code()}")
                    
                    if (response.isSuccessful && response.body()?.data != null) {
                        val item = response.body()!!.data
                        println("InventoryRepository: 成功获取物品 - 数量: ${item.quantity}")
                        item
                    } else {
                        val errorBody = response.errorBody()?.string()
                        println("InventoryRepository: API 失败 - errorBody: $errorBody")
                        throw Exception("Failed to use item: ${response.code()} - $errorBody")
                    }
                } catch (e: Exception) {
                    println("InventoryRepository: 异常 - ${e.message}")
                    e.printStackTrace()
                    throw Exception("操作失败: ${e.message}")
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

    /** 批量同步数据 */
    suspend fun syncData(items: List<InventoryItem>): Boolean =
            withContext(Dispatchers.IO) {
                // Mock implementation as InventoryApiService doesn't support sync
                true
            }

    /** 批量调整库存数量 */
    suspend fun batchUpdateQuantities(updates: Map<String, Int>): Boolean =
            withContext(Dispatchers.IO) {
                // Mock implementation
                true
            }
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

    override suspend fun batchUseItems(itemIds: List<String>): Response<BatchUseResponse> {
        return Response.success(BatchUseResponse(true, emptyList(), "Success"))
    }

    override suspend fun batchDeleteItems(itemIds: List<String>): Response<BatchDeleteResponse> {
        return Response.success(BatchDeleteResponse(true, "Success"))
    }

    override suspend fun getPurchaseList(): Response<PurchaseListResponse> {
        return Response.success(PurchaseListResponse(true, emptyList(), ""))
    }
}
