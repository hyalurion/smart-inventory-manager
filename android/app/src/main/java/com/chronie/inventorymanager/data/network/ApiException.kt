package com.chronie.inventorymanager.data.network

/**
 * 后端返回的结构化错误。
 * 后端在失败响应中会携带 `error_code` / `error_data`（如 insufficientStock + itemName），
 * 成功响应中会携带 `message_code` / `message_data`。统一用本异常把这些信息带到 UI 层，
 * 以便展示更友好的本地化提示，而不是笼统的“操作失败”。
 */
class ApiException(
    val code: String? = null,
    message: String
) : Exception(message)
