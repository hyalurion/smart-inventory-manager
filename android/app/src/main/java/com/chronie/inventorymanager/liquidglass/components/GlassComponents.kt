package com.chronie.inventorymanager.liquidglass.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chronie.inventorymanager.R
import com.chronie.inventorymanager.domain.model.InventoryItem
import com.chronie.inventorymanager.domain.model.StatusFilter
import com.chronie.inventorymanager.domain.model.StockStatus
import com.chronie.inventorymanager.domain.model.getStockStatus
import com.chronie.inventorymanager.domain.model.isExpired
import com.chronie.inventorymanager.domain.model.isExpiringSoon
import com.chronie.inventorymanager.liquidglass.backdrop.backdrops.rememberCanvasBackdrop
import com.chronie.inventorymanager.liquidglass.backdrop.drawBackdrop
import com.chronie.inventorymanager.ui.theme.GlassTypography
import com.chronie.inventorymanager.ui.theme.getGlassColors
import com.chronie.inventorymanager.ui.theme.GlassColorScheme
import com.chronie.inventorymanager.utils.CategoryNameConverter
import java.text.SimpleDateFormat
import java.util.*

/** 液态玻璃搜索框 */
@Composable
fun GlassSearchBar(
        value: String,
        onValueChange: (String) -> Unit,
        placeholder: String,
        modifier: Modifier = Modifier
) {
        val isLightTheme = !isSystemInDarkTheme()
        val colors = getGlassColors(isLightTheme)
        val backdrop = rememberCanvasBackdrop {
                drawRoundRect(
                        color = colors.container,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                )
        }

        Box(
                modifier =
                        modifier
                                .drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { RoundedCornerShape(16.dp) },
                                        effects = {}
                                )
                                .alpha(colors.alpha)
        ) {
                BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        textStyle = GlassTypography.bodyMedium.copy(color = colors.text),
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                        decorationBox = { innerTextField ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription =
                                                        stringResource(R.string.inventory_search),
                                                modifier = Modifier.size(20.dp),
                                                tint = colors.text.copy(alpha = 0.6f)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Box(modifier = Modifier.weight(1f)) {
                                                if (value.isEmpty()) {
                                                        Text(
                                                                text = placeholder,
                                                                style =
                                                                        GlassTypography.bodyMedium
                                                                                .copy(
                                                                                        color =
                                                                                                colors.text
                                                                                                        .copy(
                                                                                                                alpha =
                                                                                                                        0.6f
                                                                                                        )
                                                                                )
                                                        )
                                                }
                                                innerTextField()
                                        }
                                        if (value.isNotEmpty()) {
                                                IconButton(
                                                        onClick = { onValueChange("") },
                                                        modifier = Modifier.size(20.dp)
                                                ) {
                                                        Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = null,
                                                                tint =
                                                                        colors.text.copy(
                                                                                alpha = 0.6f
                                                                        )
                                                        )
                                                }
                                        }
                                }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { /* Handle search */})
                )
        }
}

/** 液态玻璃筛选器 */
@Composable
fun GlassFilterChip(
        text: String,
        isSelected: Boolean,
        onSelected: (Boolean) -> Unit,
        modifier: Modifier = Modifier,
        icon: ImageVector? = null
) {
        val isLightTheme = !isSystemInDarkTheme()
        val colors = getGlassColors(isLightTheme)

        Box(
                modifier =
                        modifier.background(
                                        color =
                                                if (isSelected) colors.selectedContainer
                                                else colors.container,
                                        shape = RoundedCornerShape(20.dp)
                                )
                                .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color =
                                                if (isSelected) colors.selectedBorder
                                                else Color.Transparent,
                                        shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { onSelected(!isSelected) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
        ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                        if (icon != null) {
                                Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint =
                                                if (isSelected) colors.primary
                                                else colors.text.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                                text = text,
                                style =
                                        GlassTypography.labelMedium.copy(
                                                color =
                                                        if (isSelected) colors.primary
                                                        else colors.text.copy(alpha = 0.7f)
                                        )
                        )
                }
        }
}

/** 液态玻璃下拉菜单（添加/编辑页与筛选页共用，保证样式一致；选项以 Material3 DropdownMenu 浮层呈现） */
@Composable
fun GlassDropdownMenu(
        selectedItem: String,
        items: List<String>,
        onItemSelected: (String) -> Unit,
        placeholder: String,
        modifier: Modifier = Modifier,
        multiSelect: Boolean = false,
        selectedItems: List<String> = emptyList(),
        colors: GlassColorScheme? = null,
        compact: Boolean = false
) {
        val glassColors = colors ?: getGlassColors(!isSystemInDarkTheme())
        var expanded by remember { mutableStateOf(false) }
        // 记录触发器（锚点）宽度，使浮层宽度与触发器一致并受最大宽度限制
        val anchorWidthPx = remember { mutableStateOf(0) }
        val density = LocalDensity.current

        Column(modifier = modifier) {
                if (!compact) {
                        Text(
                                text = placeholder,
                                style = GlassTypography.bodySmall.copy(
                                        color = glassColors.text.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(bottom = 4.dp)
                        )
                }

                val dropdownBackdrop = rememberCanvasBackdrop {
                        drawRoundRect(
                                color = glassColors.container,
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                        )
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                        // 下拉触发框（玻璃态）
                        Box(
                                modifier = Modifier
                                        .drawBackdrop(
                                                backdrop = dropdownBackdrop,
                                                shape = { RoundedCornerShape(12.dp) },
                                                effects = {}
                                        )
                                        .alpha(glassColors.alpha)
                                        .clickable { expanded = !expanded }
                                        .onGloballyPositioned { coords ->
                                                anchorWidthPx.value = coords.size.width
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Text(
                                                text = if (multiSelect && selectedItems.isNotEmpty()) {
                                                        stringResource(R.string.add_save) + " (${selectedItems.size})"
                                                } else if (selectedItem.isEmpty()) {
                                                        placeholder
                                                } else {
                                                        selectedItem
                                                },
                                                style = GlassTypography.bodyMedium.copy(
                                                        color = if ((multiSelect && selectedItems.isNotEmpty()) || selectedItem.isNotEmpty())
                                                                glassColors.text
                                                        else glassColors.text.copy(alpha = 0.5f)
                                                )
                                        )
                                        Icon(
                                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = glassColors.text.copy(alpha = 0.7f),
                                                modifier = Modifier.size(20.dp)
                                        )
                                }
                        }

                        // 浮层菜单：宽度与触发器一致（或受最大 320.dp 限制），带展开/收起动画
                        val menuModifier = if (anchorWidthPx.value > 0) {
                                val anchorDp = with(density) { anchorWidthPx.value.toDp() }
                                Modifier.widthIn(max = 320.dp).width(anchorDp)
                        } else {
                                Modifier.widthIn(max = 320.dp)
                        }

                        DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = menuModifier.background(Color.Transparent)
                        ) {
                                AnimatedVisibility(
                                        visible = expanded,
                                        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(animationSpec = tween(120)),
                                        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(animationSpec = tween(100))
                                ) {
                                        Column(
                                                modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(
                                                                glassColors.container.copy(alpha = 0.95f),
                                                                shape = RoundedCornerShape(12.dp)
                                                        )
                                                        .border(
                                                                width = 1.dp,
                                                                color = glassColors.border,
                                                                shape = RoundedCornerShape(12.dp)
                                                        )
                                                        .padding(vertical = 4.dp)
                                        ) {
                                                items.forEachIndexed { index, item ->
                                                        val isSelected = if (multiSelect) {
                                                                selectedItems.contains(item)
                                                        } else {
                                                                selectedItem == item
                                                        }
                                                        DropdownMenuItem(
                                                                text = {
                                                                        Text(
                                                                                text = item,
                                                                                style = GlassTypography.bodyMedium.copy(
                                                                                        color = if (isSelected)
                                                                                                glassColors.primary
                                                                                        else glassColors.text
                                                                                )
                                                                        )
                                                                },
                                                                trailingIcon = if (isSelected) {
                                                                        {
                                                                                Icon(
                                                                                        imageVector = Icons.Default.Check,
                                                                                        contentDescription = null,
                                                                                        tint = glassColors.primary,
                                                                                        modifier = Modifier.size(16.dp)
                                                                                )
                                                                        }
                                                                } else null,
                                                                onClick = {
                                                                        if (multiSelect) {
                                                                                val newSelected = if (isSelected) {
                                                                                        selectedItems - item
                                                                                } else {
                                                                                        selectedItems + item
                                                                                }
                                                                                onItemSelected(newSelected.joinToString(","))
                                                                        } else {
                                                                                onItemSelected(item)
                                                                                expanded = false
                                                                        }
                                                                },
                                                                modifier = Modifier.background(
                                                                        if (isSelected)
                                                                                glassColors.selectedContainer.copy(alpha = 0.3f)
                                                                        else Color.Transparent
                                                                )
                                                        )

                                                        if (index < items.lastIndex) {
                                                                HorizontalDivider(
                                                                        color = glassColors.border,
                                                                        thickness = 0.5.dp,
                                                                        modifier = Modifier.padding(horizontal = 16.dp)
                                                                )
                                                        }
                                                }
                                        }
                                }
                        }
                }
        }
}

/** 液态玻璃统计卡片 */
@Composable
fun GlassStatsCard(
        title: String,
        value: String,
        icon: ImageVector,
        iconTint: Color,
        modifier: Modifier = Modifier,
        trend: String? = null
) {
        val isLightTheme = !isSystemInDarkTheme()
        val colors = getGlassColors(isLightTheme)
        val backdrop = rememberCanvasBackdrop {
                drawRoundRect(
                        color = colors.cardContainer,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                )
        }

        Box(
                modifier =
                        modifier
                                .drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { RoundedCornerShape(16.dp) },
                                        effects = {}
                                )
                                .alpha(colors.alpha)
                                .padding(16.dp),
                contentAlignment = Alignment.Center
        ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                                modifier =
                                        Modifier.size(48.dp)
                                                .background(
                                                        color = iconTint.copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(12.dp)
                                                ),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        imageVector = icon,
                                        contentDescription = title,
                                        modifier = Modifier.size(24.dp),
                                        tint = iconTint
                                )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                                Text(
                                        text = title,
                                        style =
                                                GlassTypography.bodySmall.copy(
                                                        color = colors.text.copy(alpha = 0.7f)
                                                )
                                )
                                Text(
                                        text = value,
                                        style =
                                                GlassTypography.headlineSmall.copy(
                                                        color = colors.text,
                                                        fontWeight = FontWeight.Bold
                                                )
                                )
                                if (trend != null) {
                                        Text(
                                                text = trend,
                                                style =
                                                        GlassTypography.bodySmall.copy(
                                                                color =
                                                                        colors.text.copy(
                                                                                alpha = 0.6f
                                                                        )
                                                        )
                                        )
                                }
                        }
                }
        }
}

/** 液态玻璃库存物品卡片 */
@Composable
fun GlassInventoryItemCard(
        item: InventoryItem,
        onUse: () -> Unit,
        onEdit: () -> Unit,
        onDelete: () -> Unit,
        modifier: Modifier = Modifier
) {
        val isLightTheme = !isSystemInDarkTheme()
        val colors = getGlassColors(isLightTheme)
        val backdrop = rememberCanvasBackdrop {
                drawRoundRect(
                        color = colors.cardContainer,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                )
        }

        val status = item.getStockStatus()
        val statusColor =
                when (status) {
                        StockStatus.NORMAL ->
                                if (isLightTheme) Color(0xFF4CAF50) else Color(0xFF4CAF50)
                        StockStatus.LOW_STOCK -> Color(0xFFFF9800)
                        StockStatus.EXPIRING_SOON -> Color(0xFFFF5722)
                        StockStatus.EXPIRED -> Color(0xFFF44336)
                }

        Box(
                modifier =
                        modifier
                                .drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { RoundedCornerShape(16.dp) },
                                        effects = {}
                                )
                                .alpha(colors.alpha)
                                .padding(16.dp),
                contentAlignment = Alignment.Center
        ) {
                Column {
                        // 顶部状态栏
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        // 名称区域：允许换行并在右侧留出按钮空间
                                        Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                        text = item.name,
                                                        style = GlassTypography.titleMedium.copy(
                                                                color = colors.text,
                                                                fontWeight = FontWeight.Bold
                                                        ),
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        // 状态指示器
                                        Box(
                                                modifier = Modifier
                                                        .size(8.dp)
                                                        .background(
                                                                color = statusColor,
                                                                shape = RoundedCornerShape(4.dp)
                                                        )
                                        )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // 编辑按钮
                                        IconButton(onClick = onEdit) {
                                                Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = null,
                                                        tint = colors.text
                                                )
                                        }
                                        // 使用1个库存按钮（打勾图标）
                                        IconButton(onClick = onUse, enabled = item.quantity > 0) {
                                                Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint =
                                                                if (item.quantity > 0) colors.text
                                                                else colors.text.copy(alpha = 0.5f)
                                                )
                                        }
                                        // 删除整个条目按钮
                                        IconButton(onClick = onDelete) {
                                                Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = null,
                                                        tint = colors.text
                                                )
                                        }
                                }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 详细信息
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                Column {
                                        Text(
                                                text =
                                                        stringResource(id = R.string.add_category) +
                                                                ": " +
                                                                CategoryNameConverter.getDisplayName(item.category, LocalContext.current),
                                                style =
                                                        GlassTypography.bodySmall.copy(
                                                                color =
                                                                        colors.text.copy(
                                                                                alpha = 0.7f
                                                                        )
                                                        )
                                        )
                                        Text(
                                                text =
                                                        stringResource(id = R.string.add_unit) +
                                                                ": " +
                                                                item.unit,
                                                style =
                                                        GlassTypography.bodySmall.copy(
                                                                color =
                                                                        colors.text.copy(
                                                                                alpha = 0.7f
                                                                        )
                                                        )
                                        )
                                }
                                Column {
                                        Text(
                                                text =
                                                        stringResource(id = R.string.add_quantity) +
                                                                ": " +
                                                                item.quantity,
                                                style =
                                                        GlassTypography.titleMedium.copy(
                                                                color = colors.text,
                                                                fontWeight = FontWeight.Bold
                                                        )
                                        )
                                        Text(
                                                text =
                                                        stringResource(
                                                                id = R.string.add_minquantity
                                                        ) + ": " + item.minQuantity,
                                                style =
                                                        GlassTypography.bodySmall.copy(
                                                                color =
                                                                        colors.text.copy(
                                                                                alpha = 0.6f
                                                                        )
                                                        )
                                        )
                                }
                        }

                        // 过期日期和使用次数
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                item.expiryDate?.let { expiry ->
                                        val isExpired = item.isExpired()
                                        val isExpiringSoon = item.isExpiringSoon()
                                        Text(
                                                text =
                                                        stringResource(
                                                                id = R.string.add_expirydate
                                                        ) +
                                                                ": " +
                                                                SimpleDateFormat(
                                                                                "yyyy-MM-dd",
                                                                                Locale.getDefault()
                                                                        )
                                                                        .format(expiry),
                                                style =
                                                        GlassTypography.bodySmall.copy(
                                                                color =
                                                                        when {
                                                                                isExpired ->
                                                                                        Color(
                                                                                                0xFFF44336
                                                                                        )
                                                                                isExpiringSoon ->
                                                                                        Color(
                                                                                                0xFFFF5722
                                                                                        )
                                                                                else ->
                                                                                        colors.text
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.7f
                                                                                                )
                                                                        }
                                                        )
                                        )
                                }
                        }

                        // 描述
                        if (item.description.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                        text = item.description,
                                        style =
                                                GlassTypography.bodySmall.copy(
                                                        color = colors.text.copy(alpha = 0.6f)
                                                ),
                                        maxLines = 2
                                )
                        }
                }
        }
}

/** 液态玻璃筛选栏 */
@Composable
fun GlassFilterBar(
        categories: List<String>,
        selectedCategory: String,
        onCategorySelected: (String) -> Unit,
        statusFilter: StatusFilter,
        onStatusFilterChanged: (StatusFilter) -> Unit,
        modifier: Modifier = Modifier
) {
        val context = LocalContext.current
        LazyColumn(
                modifier = modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // 分类筛选
                item {
                        Text(
                                text = stringResource(R.string.inventory_category),
                                style = GlassTypography.labelLarge.copy(color = Color.Gray),
                                modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                        LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                                items(listOf("") + categories) { category ->
                                        GlassFilterChip(
                                                text =
                                                        if (category.isEmpty())
                                                                stringResource(
                                                                        R.string
                                                                                .inventory_allcategories
                                                                )
                                                        else CategoryNameConverter.getDisplayName(category, context),
                                                isSelected = selectedCategory == category,
                                                onSelected = { onCategorySelected(category) },
                                                icon =
                                                        if (category.isEmpty()) Icons.AutoMirrored.Filled.List
                                                        else Icons.Default.Category
                                        )
                                }
                        }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // 状态筛选
                item {
                        Text(
                                text = stringResource(R.string.inventory_status),
                                style = GlassTypography.labelLarge.copy(color = Color.Gray),
                                modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                        LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                                items(StatusFilter.values().toList()) { filter ->
                                        val icon =
                                                when (filter) {
                                                        StatusFilter.ALL -> Icons.AutoMirrored.Filled.List
                                                        StatusFilter.NORMAL ->
                                                                Icons.Default.CheckCircle
                                                        StatusFilter.LOW_STOCK ->
                                                                Icons.Default.Warning
                                                        StatusFilter.EXPIRING_SOON ->
                                                                Icons.Default.Schedule
                                                        StatusFilter.EXPIRED -> Icons.Default.Error
                                                }

                                        GlassFilterChip(
                                                text =
                                                        when (filter) {
                                                                StatusFilter.ALL ->
                                                                        stringResource(
                                                                                R.string
                                                                                        .inventory_showexpired
                                                                        )
                                                                StatusFilter.NORMAL ->
                                                                        stringResource(
                                                                                R.string
                                                                                        .status_normal
                                                                        )
                                                                StatusFilter.LOW_STOCK ->
                                                                        stringResource(
                                                                                R.string
                                                                                        .status_lowstock
                                                                        )
                                                                StatusFilter.EXPIRING_SOON ->
                                                                        stringResource(
                                                                                R.string
                                                                                        .status_expiringsoon
                                                                        )
                                                                StatusFilter.EXPIRED ->
                                                                        stringResource(
                                                                                R.string
                                                                                        .status_expired
                                                                        )
                                                        },
                                                isSelected = statusFilter == filter,
                                                onSelected = { onStatusFilterChanged(filter) },
                                                icon = icon
                                        )
                                }
                        }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
        }
}

/** 液态玻璃确认对话框 */
@Composable
fun GlassConfirmDialog(
        title: String? = null,
        titleRes: Int? = null,
        message: String? = null,
        content: @Composable (() -> Unit)? = null,
        confirmText: String? = null,
        cancelText: String? = null,
        dismissText: String? = null, // 用于内容对话框的兼容
        onConfirm: (() -> Unit)? = null, // 改名为onConfirmClick以保持一致
        onConfirmClick: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null,
        onDismissClick: (() -> Unit)? = null,
        isVisible: Boolean = true,
        useIconButtons: Boolean = false, // 控制是否使用图标按钮
        showButtons: Boolean = true // 控制是否显示底部按钮
) {
        val visible by remember { mutableStateOf(isVisible) }
        
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) + 
                    slideInVertically(
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                        initialOffsetY = { it / 3 }
                    ) +
                    scaleIn(
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                        initialScale = 0.9f
                    ),
            exit = fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)) + 
                   slideOutVertically(
                       animationSpec = tween(200, easing = FastOutSlowInEasing),
                       targetOffsetY = { it / 3 }
                   ) +
                   scaleOut(
                       animationSpec = tween(200, easing = FastOutSlowInEasing),
                       targetScale = 0.9f
                   )
        ) {

        val defaultConfirmText = stringResource(R.string.add_save)
        val defaultCancelText = stringResource(R.string.add_cancel)
        val finalConfirmText = confirmText ?: defaultConfirmText
        val finalCancelText = cancelText ?: defaultCancelText

        // 为了兼容旧版本和新版本
        val onConfirmClickFinal = onConfirmClick ?: onConfirm ?: {}
        val onDismissClickFinal = onDismissClick ?: onCancel ?: {}

        val isLightTheme = !isSystemInDarkTheme()
        val colors = getGlassColors(isLightTheme)
        val backdrop = rememberCanvasBackdrop {
                drawRoundRect(
                        color = colors.cardContainer,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f)
                )
        }

        Dialog(
                onDismissRequest = onDismissClickFinal,
                properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
                Box(
                        modifier =
                                Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp)
                                        .heightIn(max = 600.dp)
                                        .drawBackdrop(
                                                backdrop = backdrop,
                                                shape = { RoundedCornerShape(20.dp) },
                                                effects = {}
                                        )
                                        .alpha(colors.alpha)
                                        .padding(24.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { },
                        contentAlignment = Alignment.Center
                ) {
                        Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                        ) {
                                Text(
                                        text = titleRes?.let { stringResource(it) } ?: title ?: stringResource(R.string.error_title),
                                        style =
                                                GlassTypography.titleLarge.copy(
                                                        color = colors.text,
                                                        fontWeight = FontWeight.Bold
                                                ),
                                        modifier = Modifier.padding(bottom = 12.dp)
                                )

                                // 根据是否提供content显示内容
                                when {
                                        content != null -> {
                                                content()
                                        }
                                        message != null -> {
                                                Text(
                                                        text = message,
                                                        style =
                                                                GlassTypography.bodyMedium.copy(
                                                                        color =
                                                                                colors.text.copy(
                                                                                        alpha = 0.8f
                                                                                )
                                                                ),
                                                        modifier = Modifier.padding(bottom = 24.dp)
                                                )
                                        }
                                        else -> {
                                                Text(
                                                        text =
                                                                stringResource(
                                                                        R.string
                                                                                .airecord_norecordsgenerated
                                                                ),
                                                        style =
                                                                GlassTypography.bodyMedium.copy(
                                                                        color =
                                                                                colors.text.copy(
                                                                                        alpha = 0.5f
                                                                                )
                                                                ),
                                                        modifier = Modifier.padding(bottom = 24.dp)
                                                )
                                        }
                                }

                                if (showButtons) {
                                    // 按钮区域
                                    Row(
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                    ) {
                                            if (useIconButtons) {
                                                    // 取消按钮（×）
                                                    IconButton(
                                                            onClick = { onDismissClickFinal() },
                                                            modifier =
                                                                    Modifier.background(
                                                                                    color =
                                                                                            colors.container,
                                                                                    shape =
                                                                                            RoundedCornerShape(
                                                                                                    12.dp
                                                                                            )
                                                                            )
                                                                            .padding(8.dp)
                                                    ) {
                                                            Icon(
                                                                    imageVector = Icons.Default.Close,
                                                                    contentDescription =
                                                                            finalCancelText,
                                                                    tint = colors.text
                                                            )
                                                    }

                                                    Spacer(modifier = Modifier.width(16.dp))

                                                    // 确认按钮（√）
                                                    IconButton(
                                                            onClick = { onConfirmClickFinal() },
                                                            modifier =
                                                                    Modifier.background(
                                                                                    color =
                                                                                            Color(
                                                                                                    0xFF2196F3
                                                                                            ),
                                                                                    shape =
                                                                                            RoundedCornerShape(
                                                                                                    12.dp
                                                                                            )
                                                                            )
                                                                            .padding(8.dp)
                                                    ) {
                                                            Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription =
                                                                            finalConfirmText,
                                                                    tint = Color.White
                                                            )
                                                    }
                                            } else {
                                                    // 传统文本按钮
                                                    Button(
                                                            onClick = { onDismissClickFinal() },
                                                            colors =
                                                                    ButtonDefaults.buttonColors(
                                                                            containerColor =
                                                                                    colors.container
                                                                    ),
                                                            shape = RoundedCornerShape(12.dp),
                                                            modifier = Modifier.weight(1f)
                                                    ) {
                                                            Text(
                                                                    text =
                                                                            if (dismissText != null)
                                                                                    dismissText
                                                                            else finalCancelText,
                                                                    style =
                                                                            GlassTypography.labelLarge
                                                                                    .copy(
                                                                                            color =
                                                                                                    colors.text
                                                                                    )
                                                            )
                                                    }

                                                    Button(
                                                            onClick = { onConfirmClickFinal() },
                                                            colors =
                                                                    ButtonDefaults.buttonColors(
                                                                            containerColor =
                                                                                    Color(0xFF2196F3)
                                                                    ),
                                                            shape = RoundedCornerShape(12.dp),
                                                            modifier = Modifier.weight(1f)
                                                    ) {
                                                            Text(
                                                                    text = finalConfirmText,
                                                                    style =
                                                                            GlassTypography.labelLarge
                                                                            .copy(
                                                                                    color =
                                                                                            Color.White
                                                                            )
                                                            )
                                                    }
                                            }
                                    }
                                }
                        }
                }
        }
        }
}
