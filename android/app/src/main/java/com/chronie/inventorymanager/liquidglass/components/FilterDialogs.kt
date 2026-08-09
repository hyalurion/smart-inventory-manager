package com.chronie.inventorymanager.liquidglass.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import java.text.SimpleDateFormat
import java.util.Locale
import com.chronie.inventorymanager.R
import com.chronie.inventorymanager.domain.model.InventoryItem
import com.chronie.inventorymanager.domain.model.StockStatus
import com.chronie.inventorymanager.domain.model.StatusFilter
import com.chronie.inventorymanager.domain.model.SortOption
import com.chronie.inventorymanager.domain.model.UnifiedFilter
import com.chronie.inventorymanager.ui.theme.GlassColorScheme
import com.chronie.inventorymanager.ui.theme.GlassTypography
import com.chronie.inventorymanager.ui.theme.getGlassColors
import com.chronie.inventorymanager.utils.CategoryNameConverter

/**
 * 分类选择对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilterDialog(
    categories: List<String>,
    selectedCategories: List<String>,
    onCategoriesSelected: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLightTheme = !isSystemInDarkTheme()
    val colors = getGlassColors(isLightTheme)
    
    GlassConfirmDialog(
        title = stringResource(id = R.string.inventory_category),
        content = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(categories) { category ->
                    CategoryFilterItem(
                        category = category,
                        isSelected = selectedCategories.contains(category),
                        onSelected = { isSelected ->
                            val newSelection = if (isSelected) {
                                selectedCategories + category
                            } else {
                                selectedCategories - category
                            }
                            onCategoriesSelected(newSelection)
                        },
                        colors = colors
                    )
                }
            }
        },
        confirmText = stringResource(id = R.string.add_save),
        dismissText = stringResource(id = R.string.add_cancel),
        onConfirmClick = onDismiss,
        onDismissClick = onDismiss,
        isVisible = true
        )
    }

/**
 * 分类筛选项
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterItem(
    category: String,
    isSelected: Boolean,
    onSelected: (Boolean) -> Unit,
    colors: GlassColorScheme
) {
    Surface(
        onClick = { onSelected(!isSelected) },
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected) colors.selectedContainer else colors.container.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) colors.selectedBorder else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = CategoryNameConverter.getDisplayName(category),
                style = GlassTypography.bodyLarge.copy(
                    color = if (isSelected) colors.primary else colors.text,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = colors.primary
                )
            }
        }
    }
}

/**
 * 物品操作对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemActionDialog(
    item: InventoryItem,
    onUseOne: () -> Unit,
    onEdit: () -> Unit,
    onView: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLightTheme = !isSystemInDarkTheme()
    val colors = getGlassColors(isLightTheme)
    
    GlassConfirmDialog(
            title = item.name,
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 物品基本信息
                    Text(
                        text = "",
                        style = GlassTypography.bodyMedium.copy(
                            color = colors.text.copy(alpha = 0.7f)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 操作按钮组（使用图标版本）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 使用一个物品
                        IconActionButton(
                            icon = Icons.Default.Remove,
                            enabled = item.quantity > 0,
                            onClick = {
                                println("ItemActionDialog: 使用物品按钮被点击 - ${item.name}")
                                onUseOne()
                                onDismiss()
                            },
                            colors = colors,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // 编辑物品
                        IconActionButton(
                            icon = Icons.Default.Edit,
                            onClick = {
                                onEdit()
                                onDismiss()
                            },
                            colors = colors,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // 查看详情
                        IconActionButton(
                            icon = Icons.Default.Info,
                            onClick = {
                                onView()
                                onDismiss()
                            },
                            colors = colors,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // 删除物品
                        IconActionButton(
                            icon = Icons.Default.Delete,
                            onClick = {
                                onDelete()
                                onDismiss()
                            },
                            colors = colors,
                            isDestructive = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmText = "",
            dismissText = "",
            onConfirmClick = {},
            onDismissClick = onDismiss,
            useIconButtons = true
        )
}

/**
 * 图标操作按钮组件（纯图标版本）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IconActionButton(
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
    colors: GlassColorScheme,
    isDestructive: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = {
            println("IconActionButton: Surface onClick triggered")
            onClick()
        },
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = when {
            !enabled -> colors.container.copy(alpha = 0.3f)
            isDestructive -> Color(0xFFFFEBEE).copy(alpha = 0.8f)
            else -> colors.container.copy(alpha = 0.5f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = when {
                !enabled -> colors.border.copy(alpha = 0.3f)
                isDestructive -> Color(0xFFF44336).copy(alpha = 0.5f)
                else -> colors.border.copy(alpha = 0.5f)
            }
        ),
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = when {
                    !enabled -> colors.text.copy(alpha = 0.3f)
                    isDestructive -> Color(0xFFF44336)
                    else -> colors.text.copy(alpha = 0.8f)
                },
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * 操作按钮组件（带文字版本）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
    colors: GlassColorScheme,
    isDestructive: Boolean = false
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = when {
                    !enabled -> colors.container.copy(alpha = 0.3f)
                    isDestructive -> Color(0xFFFFEBEE).copy(alpha = 0.8f)
                    else -> colors.container.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = when {
                    !enabled -> colors.border.copy(alpha = 0.3f)
                    isDestructive -> Color(0xFFF44336).copy(alpha = 0.5f)
                    else -> colors.border.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        tonalElevation = 0.dp
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = when {
                    !enabled -> colors.text.copy(alpha = 0.3f)
                    isDestructive -> Color(0xFFF44336)
                    else -> colors.text.copy(alpha = 0.8f)
                }
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = GlassTypography.bodyLarge.copy(
                    color = when {
                        !enabled -> colors.text.copy(alpha = 0.3f)
                        isDestructive -> Color(0xFFF44336)
                        else -> colors.text
                    },
                    fontWeight = if (isDestructive) FontWeight.Bold else FontWeight.Normal
                )
            )
        }
    }
}

/**
 * 状态筛选对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusFilterDialog(
    selectedStatuses: List<StockStatus>,
    onStatusesSelected: (List<StockStatus>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLightTheme = !isSystemInDarkTheme()
    val colors = getGlassColors(isLightTheme)
    
    val statusOptions = listOf(
        StockStatus.NORMAL to stringResource(id = R.string.status_normal),
        StockStatus.LOW_STOCK to stringResource(id = R.string.status_lowstock),
        StockStatus.EXPIRING_SOON to stringResource(id = R.string.status_expiringsoon),
        StockStatus.EXPIRED to stringResource(id = R.string.status_expired)
    )
    
    GlassConfirmDialog(
        title = stringResource(id = R.string.inventory_status),
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                statusOptions.forEach { (status, label) ->
                    StatusFilterItem(
                        status = status,
                        label = label,
                        isSelected = selectedStatuses.contains(status),
                        onSelected = { isSelected ->
                            val newSelection = if (isSelected) {
                                selectedStatuses + status
                            } else {
                                selectedStatuses - status
                            }
                            onStatusesSelected(newSelection)
                        },
                        colors = colors
                    )
                }
            }
        },
        confirmText = stringResource(id = R.string.add_save),
        dismissText = stringResource(id = R.string.add_cancel),
        onConfirmClick = onDismiss,
        onDismissClick = onDismiss,
        isVisible = true
    )
}

/**
 * 状态筛选项
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusFilterItem(
    status: StockStatus,
    label: String,
    isSelected: Boolean,
    onSelected: (Boolean) -> Unit,
    colors: GlassColorScheme
) {
    val statusColor = when (status) {
        StockStatus.NORMAL -> if (isSystemInDarkTheme()) Color(0xFF4CAF50) else Color(0xFF4CAF50)
        StockStatus.LOW_STOCK -> Color(0xFFFF9800)
        StockStatus.EXPIRING_SOON -> Color(0xFFFF5722)
        StockStatus.EXPIRED -> Color(0xFFF44336)
    }
    
    Surface(
        onClick = { onSelected(!isSelected) },
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected) colors.selectedContainer else colors.container.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) colors.selectedBorder else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 状态颜色指示器
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = statusColor,
                            shape = RoundedCornerShape(6.dp)
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    style = GlassTypography.bodyLarge.copy(
                        color = if (isSelected) colors.primary else colors.text,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = colors.primary
                )
            }
        }
    }
}

/**
 * 高级筛选对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedFilterDialog(
    dateRange: Pair<Long?, Long?>,
    onDateRangeChanged: (Pair<Long?, Long?>) -> Unit,
    minQuantity: Int?,
    onMinQuantityChanged: (Int?) -> Unit,
    onDismiss: () -> Unit,
    onApplyFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLightTheme = !isSystemInDarkTheme()
    val colors = getGlassColors(isLightTheme)
    
    var tempDateRange by remember { mutableStateOf(dateRange) }
    var tempMinQuantity by remember { mutableStateOf(minQuantity) }
    
    GlassConfirmDialog(
        title = stringResource(R.string.filter_title),
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 数量筛选
                Text(
                    text = stringResource(R.string.add_minquantity),
                    style = GlassTypography.bodyMedium.copy(
                        color = colors.text.copy(alpha = 0.8f)
                    )
                )
                OutlinedTextField(
                    value = tempMinQuantity?.toString() ?: "",
                    onValueChange = { value ->
                        tempMinQuantity = value.toIntOrNull()
                    },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.add_minquantity),
                            style = GlassTypography.bodyMedium
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.text,
                        unfocusedTextColor = colors.text.copy(alpha = 0.7f),
                        focusedBorderColor = colors.selectedBorder,
                        unfocusedBorderColor = colors.border,
                        focusedLabelColor = colors.text.copy(alpha = 0.7f),
                        unfocusedLabelColor = colors.text.copy(alpha = 0.5f)
                    ),
                    textStyle = GlassTypography.bodyMedium
                )
            }
        },
        confirmText = stringResource(id = R.string.add_save),
        dismissText = stringResource(id = R.string.add_cancel),
        onConfirmClick = {
            onDateRangeChanged(tempDateRange)
            onMinQuantityChanged(tempMinQuantity)
            onApplyFilters()
            onDismiss()
        },
        onDismissClick = onDismiss,
        isVisible = true
    )
}

/**
 * 排序选项对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortDialog(
    currentSort: String,
    onSortSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLightTheme = !isSystemInDarkTheme()
    val colors = getGlassColors(isLightTheme)
    
    val sortOptions = listOf(
        stringResource(R.string.sort_name_asc) to "name_asc",
        stringResource(R.string.sort_name_desc) to "name_desc",
        stringResource(R.string.sort_quantity_asc) to "quantity_asc",
        stringResource(R.string.sort_quantity_desc) to "quantity_desc",
        stringResource(R.string.sort_date_added_desc) to "created_desc",
        stringResource(R.string.sort_date_added_asc) to "created_asc"
    )
    
    GlassConfirmDialog(
        title = stringResource(R.string.sort_title),
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                sortOptions.forEach { (label, value) ->
                    SortOptionItem(
                        label = label,
                        isSelected = currentSort == value,
                        onSelected = {
                            onSortSelected(value)
                            onDismiss()
                        },
                        colors = colors
                    )
                }
            }
        },
        confirmText = stringResource(id = R.string.add_cancel),
        dismissText = "",
        onConfirmClick = onDismiss,
        onDismissClick = {},
        isVisible = true
    )
}

/**
 * 排序选项项
 */
@Composable
private fun SortOptionItem(
    label: String,
    isSelected: Boolean,
    onSelected: () -> Unit,
    colors: GlassColorScheme
) {
    Surface(
        onClick = onSelected,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected) colors.selectedContainer else colors.container.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) colors.selectedBorder else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = GlassTypography.bodyLarge.copy(
                    color = if (isSelected) colors.primary else colors.text,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = colors.primary
                )
            }
        }
    }
}

/**
 * 统一筛选对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedFilterDialog(
    categories: List<String>,
    currentFilter: UnifiedFilter,
    onFilterChanged: (UnifiedFilter) -> Unit,
    onDismiss: () -> Unit,
    isVisible: Boolean = false
) {
    val context = LocalContext.current
    var tempFilter by remember(currentFilter) { mutableStateOf(currentFilter) }
    
    // 当 currentFilter 改变时，更新 tempFilter
    LaunchedEffect(currentFilter) {
        tempFilter = currentFilter
    }
    
    // 预先获取排序选项的显示文本映射，避免在非Composable上下文中调用stringResource
    val sortOptionDisplayMap = remember {
        SortOption.values().associateWith { sortOption ->
            when (sortOption) {
                SortOption.NAME_ASC -> context.getString(R.string.sort_name_asc)
                SortOption.NAME_DESC -> context.getString(R.string.sort_name_desc)
                SortOption.QUANTITY_ASC -> context.getString(R.string.sort_quantity_asc)
                SortOption.QUANTITY_DESC -> context.getString(R.string.sort_quantity_desc)
                SortOption.DATE_ADDED_ASC -> context.getString(R.string.sort_date_added_asc)
                SortOption.DATE_ADDED_DESC -> context.getString(R.string.sort_date_added_desc)
                SortOption.EXPIRY_DATE_ASC -> context.getString(R.string.sort_expiry_date_asc)
                SortOption.EXPIRY_DATE_DESC -> context.getString(R.string.sort_expiry_date_desc)
                SortOption.UPDATED_AT_ASC -> context.getString(R.string.sort_updated_at_asc)
                SortOption.UPDATED_AT_DESC -> context.getString(R.string.sort_updated_at_desc)
            }
        }
    }
    
    // 预先获取状态筛选选项的显示文本映射
    val statusFilterDisplayMap = remember {
        StatusFilter.values().associateWith { statusFilter ->
            context.getString(statusFilter.displayNameRes)
        }
    }
    val statusFilterByDisplay = remember { statusFilterDisplayMap.entries.associate { (key, value) -> value to key } }
    
    val sortOptions = remember { SortOption.values().map { sortOption -> sortOptionDisplayMap[sortOption]!! } }
    val sortOptionByDisplay = remember { sortOptionDisplayMap.entries.associate { (key, value) -> value to key } }

    if (isVisible) {
        GlassConfirmDialog(
            title = stringResource(id = R.string.inventory_filter),
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                // 分类下拉菜单（多选）
                GlassDropdownMenu(
                    selectedItem = if (tempFilter.selectedCategories.isEmpty()) "" else tempFilter.selectedCategories.map { CategoryNameConverter.getDisplayName(it, context) }.joinToString(", "),
                    items = categories.map { CategoryNameConverter.getDisplayName(it, context) },
                    onItemSelected = { selected ->
                        val selectedKeys = if (selected.isEmpty()) emptyList() else {
                            selected.split(", ").map { displayName ->
                                // 根据显示名称找到对应的分类键名
                                categories.find { category -> CategoryNameConverter.getDisplayName(category, context) == displayName } ?: displayName
                            }
                        }
                        tempFilter = tempFilter.copy(selectedCategories = selectedKeys)
                    },
                    placeholder = stringResource(id = R.string.inventory_category),
                    multiSelect = true,
                    selectedItems = tempFilter.selectedCategories.map { CategoryNameConverter.getDisplayName(it, context) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 状态下拉菜单（单选）
                GlassDropdownMenu(
                    selectedItem = statusFilterDisplayMap[tempFilter.statusFilter] ?: "",
                    items = StatusFilter.values().map { statusFilterDisplayMap[it] ?: "" },
                    onItemSelected = { selected ->
                        tempFilter = tempFilter.copy(statusFilter = statusFilterByDisplay[selected] ?: StatusFilter.ALL)
                    },
                    placeholder = stringResource(id = R.string.inventory_status),
                    modifier = Modifier.fillMaxWidth()
                )
                
                    // 排序下拉菜单（单选）
                    GlassDropdownMenu(
                        selectedItem = sortOptionDisplayMap[tempFilter.sortOption] ?: "",
                        items = sortOptions,
                        onItemSelected = { selected ->
                            val sortOption = sortOptionByDisplay[selected] ?: SortOption.NAME_ASC
                            tempFilter = tempFilter.copy(sortOption = sortOption)
                        },
                        placeholder = stringResource(id = R.string.inventory_sort),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmText = stringResource(id = R.string.add_save),
            dismissText = stringResource(id = R.string.add_cancel),
            onConfirmClick = {
                onFilterChanged(tempFilter)
                onDismiss()
            },
            onDismissClick = onDismiss,
            isVisible = true
        )
    }
}