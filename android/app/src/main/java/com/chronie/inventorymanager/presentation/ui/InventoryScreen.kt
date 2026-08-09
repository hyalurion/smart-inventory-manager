package com.chronie.inventorymanager.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.core.*
import kotlinx.coroutines.delay
import com.chronie.inventorymanager.R
import com.chronie.inventorymanager.domain.model.InventoryItem
import com.chronie.inventorymanager.domain.model.StockStatistics
import com.chronie.inventorymanager.domain.model.StatusFilter
import com.chronie.inventorymanager.domain.model.SortOption
import com.chronie.inventorymanager.domain.model.UnifiedFilter
import com.chronie.inventorymanager.domain.model.StockStatus
import com.chronie.inventorymanager.liquidglass.components.*
import com.chronie.inventorymanager.liquidglass.utils.GlassButton
import com.chronie.inventorymanager.liquidglass.utils.GlassContainer
import com.chronie.inventorymanager.presentation.viewmodel.InventoryViewModel
import com.chronie.inventorymanager.ui.theme.getGlassColors

/** 库存主页面 显示库存物品列表，支持搜索、筛选、统计显示和操作功能 */
@Composable
fun InventoryScreen(
        viewModel: InventoryViewModel = hiltViewModel(),
        onAddItem: () -> Unit = {},
        onEditItem: (InventoryItem) -> Unit = {},
        onViewItem: (InventoryItem) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLightTheme = !isSystemInDarkTheme()
    val glassColors = getGlassColors(isLightTheme)

    // 对话框状态
    var showUnifiedFilterDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadInventory()
        viewModel.loadStatistics()
    }

    // 优先渲染错误覆盖层，确保真正显示在最顶层
    if (uiState.error != null) {
        Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
        ) {
            ErrorOverlay(message = uiState.error!!, onDismiss = viewModel::clearError)
        }
    } else {
        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .background(
                                        brush =
                                                Brush.verticalGradient(
                                                        colors =
                                                                listOf(
                                                                        glassColors.primary.copy(
                                                                                alpha = 0.1f
                                                                        ),
                                                                        glassColors.secondary.copy(
                                                                                alpha = 0.05f
                                                                        )
                                                                )
                                                )
                                )
        ) {
            // 主内容区域
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    // 统计卡片区域
                    InventoryStatsSection(
                            statistics = uiState.statistics,
                            isLoading = uiState.isLoadingStats
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 搜索和筛选栏（包含刷新按钮）
                    InventoryFilterSection(
                            searchQuery = uiState.searchQuery,
                            onSearchChanged = viewModel::updateSearchQuery,
                            onShowFilterDialog = { showUnifiedFilterDialog = true },
                            onRefresh = { viewModel.refreshData() },
                            onClearFilters = { viewModel.clearFilters() },
                            selectedCategory = uiState.selectedCategory,
                            statusFilter = uiState.statusFilter,
                            sortOption = uiState.sortOption
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 库存物品列表
                    InventoryItemsList(
                            items = uiState.filteredItems,
                            onItemClick = { item -> onViewItem(item) },
                            onEditItem = { item -> onEditItem(item) },
                            onDeleteItem = { item -> viewModel.deleteItem(item) },
                            onUseItem = { item -> viewModel.useItem(item) }
                    )
                    
                    // 占位区域已移至InventoryItemsList内部作为最后一项
                    // 避免在主Column中重复添加占位区域
                }

                // 统一筛选对话框
                UnifiedFilterDialog(
                        categories = uiState.availableCategories,
                        currentFilter = UnifiedFilter(
                                selectedCategories = if (uiState.selectedCategory != null) listOf(uiState.selectedCategory!!) else emptyList(),
                                statusFilter = uiState.statusFilter,
                                sortOption = uiState.sortOption
                        ),
                        onFilterChanged = { filter ->
                                // 更新筛选条件
                                viewModel.updateCategory(
                                        if (filter.selectedCategories.isEmpty()) null else filter.selectedCategories.first()
                                )
                                viewModel.updateStatusFilter(filter.statusFilter)
                                viewModel.updateSortOption(filter.sortOption)
                                showUnifiedFilterDialog = false
                        },
                        onDismiss = { showUnifiedFilterDialog = false },
                        isVisible = showUnifiedFilterDialog
                )



                // 加载覆盖层 - 在对话框之下，但在主内容之上
                if (uiState.isLoading || uiState.isLoadingStats) {
                    LoadingOverlay()
                }
            }
        }
    }
}

/** 统计信息卡片区域 */
@Composable
private fun InventoryStatsSection(statistics: StockStatistics?, isLoading: Boolean) {
    val isLightTheme = !isSystemInDarkTheme()
    val glassColors = getGlassColors(isLightTheme)
    
    if (isLoading || statistics == null) {
        // 统计卡片骨架屏
        GlassContainer(modifier = Modifier.fillMaxWidth(), backgroundAlpha = 0.2f, enableBlur = false) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        text = stringResource(R.string.inventory_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = glassColors.text
                )
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(4) { index -> GlassStatsCardSkeleton() }
                }
            }
        }
    } else {
        GlassContainer(modifier = Modifier.fillMaxWidth(), backgroundAlpha = 0.15f, enableBlur = false) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                        text = stringResource(R.string.inventory_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = glassColors.text
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        GlassStatsCard(
                                title = stringResource(R.string.stats_totalitems),
                                value = statistics.totalItems.toString(),
                                icon = Icons.Default.Inventory2,
                                iconTint = glassColors.info
                        )
                    }
                    item {
                        GlassStatsCard(
                                title = stringResource(R.string.stats_lowstock),
                                value = statistics.lowStockItems.toString(),
                                icon = Icons.Default.Warning,
                                iconTint = glassColors.warning
                        )
                    }
                    item {
                        GlassStatsCard(
                                title = stringResource(R.string.stats_expiringsoon),
                                value = statistics.expiringSoonItems.toString(),
                                icon = Icons.Default.Schedule,
                                iconTint = glassColors.warning
                        )
                    }
                    item {
                        GlassStatsCard(
                                title = stringResource(R.string.status_expired),
                                value = statistics.expiredItems.toString(),
                                icon = Icons.Default.Error,
                                iconTint = glassColors.error
                        )
                    }
                }
            }
        }
    }
}

/** 搜索和筛选区域 */
@Composable
private fun InventoryFilterSection(
        searchQuery: String,
        onSearchChanged: (String) -> Unit,
        onShowFilterDialog: () -> Unit,
        onRefresh: () -> Unit,
        onClearFilters: () -> Unit = {},
        selectedCategory: String? = null,
        statusFilter: StatusFilter = StatusFilter.ALL,
        sortOption: SortOption = SortOption.NAME_ASC
) {
    val isLightTheme = !isSystemInDarkTheme()
    val glassColors = getGlassColors(isLightTheme)
    
    GlassContainer(modifier = Modifier.fillMaxWidth(), backgroundAlpha = 0.1f, enableBlur = false) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // 搜索框
            GlassSearchBar(
                    value = searchQuery,
                    onValueChange = onSearchChanged,
                    placeholder = stringResource(R.string.inventory_search),
                    modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 筛选和操作按钮行
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // 统一筛选按钮
                IconButton(
                        onClick = onShowFilterDialog,
                        modifier =
                                Modifier.clip(RoundedCornerShape(12.dp))
                                        .background(glassColors.cardContainer.copy(alpha = 0.4f))
                ) {
                    Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.inventory_filter),
                            tint = glassColors.primary,
                            modifier = Modifier.size(20.dp)
                    )
                }

                // 刷新按钮
                IconButton(
                        onClick = onRefresh,
                        modifier =
                                Modifier.clip(RoundedCornerShape(12.dp))
                                        .background(glassColors.cardContainer.copy(alpha = 0.4f))
                ) {
                    Icon(
                            imageVector = Icons.Default.Refresh,
                            tint = glassColors.primary,
                            contentDescription = stringResource(R.string.purchaselist_refresh),
                            modifier = Modifier.size(20.dp)
                    )
                }

                // 清除筛选按钮（当有筛选条件时显示）
                val hasActiveFilters = selectedCategory != null || 
                                      statusFilter != StatusFilter.ALL || 
                                      sortOption != SortOption.NAME_ASC
                
                if (hasActiveFilters) {
                    IconButton(
                            onClick = onClearFilters,
                            modifier =
                                    Modifier.clip(RoundedCornerShape(12.dp))
                                            .background(glassColors.cardContainer.copy(alpha = 0.4f))
                    ) {
                        Icon(
                                imageVector = Icons.Default.Close,
                                tint = glassColors.primary,
                                contentDescription = stringResource(R.string.filter_clear),
                                modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/** 库存物品列表 */
@Composable
private fun InventoryItemsList(
        items: List<InventoryItem>,
        onItemClick: (InventoryItem) -> Unit,
        onEditItem: (InventoryItem) -> Unit,
        onDeleteItem: (InventoryItem) -> Unit,
        onUseItem: (InventoryItem) -> Unit
) {
    val isLightTheme = !isSystemInDarkTheme()
    val glassColors = getGlassColors(isLightTheme)
    
    if (items.isEmpty()) {
        GlassContainer(modifier = Modifier.fillMaxWidth(), backgroundAlpha = 0.1f, enableBlur = false) {
            Column(
                    modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = glassColors.text.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                        text = stringResource(id = R.string.inventory_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = glassColors.text.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                        text = stringResource(id = R.string.inventory_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = glassColors.text.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(
                    items = items,
                    key = { it.id }
            ) { item ->
                GlassInventoryItemCard(
                        item = item,
                        onUse = { onUseItem(item) },
                        onEdit = { onEditItem(item) },
                        onDelete = { onDeleteItem(item) }
                )
            }
            
            // 底部占位项，用于适应底部导航栏高度
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

/** 加载覆盖层 */
@Composable
private fun LoadingOverlay() {
    val isLightTheme = !isSystemInDarkTheme()
    val glassColors = getGlassColors(isLightTheme)
    
    Box(
            modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
    ) {
        GlassContainer(modifier = Modifier.padding(32.dp), backgroundAlpha = 0.2f) {
            Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = glassColors.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                        text = stringResource(id = R.string.loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = glassColors.text
                )
            }
        }
    }
}

/** 错误覆盖层 */
@Composable
private fun ErrorOverlay(message: String, onDismiss: () -> Unit) {
    val isLightTheme = !isSystemInDarkTheme()
    val glassColors = getGlassColors(isLightTheme)
    
    // 动画状态
    var isVisible by remember { mutableStateOf(false) }
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    )

    // 启动动画
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // 关闭动画处理
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            delay(300) // 等待动画完成
            onDismiss()
        }
    }

    Box(
            modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = animatedAlpha)
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                        }
                    },
            contentAlignment = Alignment.Center
    ) {
        // 背景暗化层
        Box(
                modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f * animatedAlpha))
        )

        // 错误内容毛玻璃容器
        GlassContainer(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .graphicsLayer(
                            scaleX = animatedScale,
                            scaleY = animatedScale,
                            alpha = animatedAlpha
                        ),
                backgroundAlpha = 0.25f,
                enableBlur = false
        ) {
            Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = glassColors.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                        text = stringResource(id = R.string.error_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = glassColors.text
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = glassColors.text,
                        modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                GlassButton(
                        onClick = {
                            // 触发淡出动画
                            isVisible = false
                        },
                        text = stringResource(id = R.string.dismiss)
                )
            }
        }
    }
}

/** 筛选和排序栏 */
@Composable
private fun FilterSortBar(
        categories: List<String>,
        selectedCategory: String?,
        statusFilter: StatusFilter,
        onCategoryChanged: (String?) -> Unit,
        onStatusFilterChanged: (StatusFilter) -> Unit,
        onClearFilters: () -> Unit,
        onShowCategoryDialog: () -> Unit = {},
        onShowStatusDialog: () -> Unit = {},
        onShowSortDialog: () -> Unit = {},
        onShowAdvancedDialog: () -> Unit = {}
) {
    val isLightTheme = !isSystemInDarkTheme()
    val glassColors = getGlassColors(isLightTheme)

    // 对话框状态
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showAdvancedDialog by remember { mutableStateOf(false) }

    Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        // 分类筛选
        GlassFilterChip(
                text = selectedCategory ?: stringResource(id = R.string.inventory_title),
                isSelected = selectedCategory != null,
                onSelected = { isSelected ->
                    if (isSelected) {
                        showCategoryDialog = true
                    } else {
                        onCategoryChanged(null)
                    }
                },
                modifier = Modifier
        )

        // 状态筛选
        GlassFilterChip(
                text =
                        when (statusFilter) {
                            StatusFilter.ALL -> stringResource(id = R.string.inventory_title)
                            StatusFilter.NORMAL -> stringResource(id = R.string.inventory_title)
                            StatusFilter.LOW_STOCK -> stringResource(id = R.string.inventory_title)
                            StatusFilter.EXPIRING_SOON ->
                                    stringResource(id = R.string.inventory_title)
                            StatusFilter.EXPIRED -> stringResource(id = R.string.inventory_title)
                        },
                isSelected = statusFilter != StatusFilter.ALL,
                onSelected = { isSelected ->
                    if (isSelected) {
                        showStatusDialog = true
                    } else {
                        onStatusFilterChanged(StatusFilter.ALL)
                    }
                },
                modifier = Modifier
        )

        Spacer(modifier = Modifier.weight(1f))

        // 高级筛选按钮
        IconButton(
                onClick = { showAdvancedDialog = true },
                modifier =
                        Modifier.clip(RoundedCornerShape(8.dp))
                                .background(glassColors.container.copy(alpha = 0.3f))
        ) {
            Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = glassColors.text.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
            )
        }

        // 排序按钮
        IconButton(
                onClick = { showSortDialog = true },
                modifier =
                        Modifier.clip(RoundedCornerShape(8.dp))
                                .background(glassColors.container.copy(alpha = 0.3f))
        ) {
            Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = null,
                    tint = glassColors.text.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
            )
        }

        // 清除筛选按钮
        if (selectedCategory != null || statusFilter != StatusFilter.ALL) {
            IconButton(
                    onClick = onClearFilters,
                    modifier =
                            Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(glassColors.container.copy(alpha = 0.3f))
            ) {
                Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = glassColors.text.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    // 对话框显示
    if (showCategoryDialog) {
        CategoryFilterDialog(
                categories = categories,
                selectedCategories =
                        if (selectedCategory != null) listOf(selectedCategory) else emptyList(),
                onCategoriesSelected = { categories ->
                    onCategoryChanged(if (categories.isEmpty()) null else categories.first())
                    showCategoryDialog = false
                },
                onDismiss = { showCategoryDialog = false }
        )
    }

    if (showStatusDialog) {
        StatusFilterDialog(
                selectedStatuses =
                        listOf(
                                when (statusFilter) {
                                    StatusFilter.ALL -> StockStatus.NORMAL // 占位值，不会被选中
                                    StatusFilter.NORMAL -> StockStatus.NORMAL
                                    StatusFilter.LOW_STOCK -> StockStatus.LOW_STOCK
                                    StatusFilter.EXPIRING_SOON -> StockStatus.EXPIRING_SOON
                                    StatusFilter.EXPIRED -> StockStatus.EXPIRED
                                }
                        ),
                onStatusesSelected = { statuses ->
                    val newFilter =
                            when {
                                statuses.isEmpty() -> StatusFilter.ALL
                                statuses.contains(StockStatus.NORMAL) -> StatusFilter.NORMAL
                                statuses.contains(StockStatus.LOW_STOCK) -> StatusFilter.LOW_STOCK
                                statuses.contains(StockStatus.EXPIRING_SOON) ->
                                        StatusFilter.EXPIRING_SOON
                                statuses.contains(StockStatus.EXPIRED) -> StatusFilter.EXPIRED
                                else -> StatusFilter.ALL
                            }
                    onStatusFilterChanged(newFilter)
                    showStatusDialog = false
                },
                onDismiss = { showStatusDialog = false }
        )
    }

    if (showSortDialog) {
        SortDialog(
                currentSort = "",
                onSortSelected = { /* TODO: 实现排序逻辑 */},
                onDismiss = { showSortDialog = false }
        )
    }

    if (showAdvancedDialog) {
        AdvancedFilterDialog(
                dateRange = Pair(null, null),
                onDateRangeChanged = { /* TODO: 实现日期范围筛选 */},
                minQuantity = null,
                onMinQuantityChanged = { /* TODO: 实现最小数量筛选 */},
                onDismiss = { showAdvancedDialog = false },
                onApplyFilters = { /* TODO: 应用高级筛选 */}
        )
    }
}

/**
 * 统计卡片骨架屏
 */
@Composable
private fun GlassStatsCardSkeleton() {
    val glassColors = getGlassColors(true)

    Row(
            modifier =
                    Modifier.clip(RoundedCornerShape(12.dp))
                            .background(glassColors.container.copy(alpha = 0.3f))
                            .padding(16.dp)
                            .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 骨架图标
        Box(
                modifier =
                        Modifier.size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(glassColors.text.copy(alpha = 0.2f))
        )

        Column(modifier = Modifier.weight(1f)) {
            // 骨架标题
            Box(
                    modifier =
                            Modifier.height(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(glassColors.text.copy(alpha = 0.2f))
                                    .fillMaxWidth(0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 骨架数值
            Box(
                    modifier =
                            Modifier.height(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(glassColors.text.copy(alpha = 0.2f))
                                    .fillMaxWidth(0.4f)
            )
        }
    }
}
