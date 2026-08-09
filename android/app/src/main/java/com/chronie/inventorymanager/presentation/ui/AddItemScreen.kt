package com.chronie.inventorymanager.presentation.ui

import android.app.DatePickerDialog
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.*
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.map
import com.chronie.inventorymanager.presentation.viewmodel.InventoryViewModel
import com.chronie.inventorymanager.R
import com.chronie.inventorymanager.domain.model.InventoryItem
import com.chronie.inventorymanager.liquidglass.backdrop.backdrops.rememberCanvasBackdrop
import com.chronie.inventorymanager.liquidglass.backdrop.drawBackdrop
import com.chronie.inventorymanager.liquidglass.components.GlassDropdownMenu
import com.chronie.inventorymanager.ui.theme.GlassTypography
import com.chronie.inventorymanager.ui.theme.getGlassColors
import java.text.SimpleDateFormat
import java.util.*
import java.util.Date
import android.widget.Toast

// 全局日期格式化函数
fun formatDate(date: Date?): String {
    if (date == null) return ""
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(date)
}

@Composable
fun AddItemScreen(
    itemId: String? = null,
    onItemAdded: (InventoryItem) -> Unit = {},
    onItemUpdated: (InventoryItem) -> Unit = {},
    editingItem: InventoryItem? = null,
    onBackPress: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: InventoryViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val isLightTheme = !isSystemInDarkTheme()
    val glassColorScheme = getGlassColors(isLightTheme)
    
    // 转换GlassColorScheme为GlassColors
    val colors = GlassColors(
        primary = glassColorScheme.primary,
        background = glassColorScheme.container,
        surface = glassColorScheme.cardContainer,
        text = glassColorScheme.text,
        border = glassColorScheme.border,
        error = glassColorScheme.error,
        container = glassColorScheme.container,
        textField = glassColorScheme.container,
        selectedContainer = glassColorScheme.selectedContainer,
        secondaryButton = glassColorScheme.secondary,
        disabledButton = glassColorScheme.textSecondary.copy(alpha = 0.5f),
        dialog = glassColorScheme.cardContainer,
        alpha = glassColorScheme.alpha
    )
    
    // 加载被编辑的物品
    var loadedItem by remember { mutableStateOf<InventoryItem?>(editingItem) }
    
    // 当itemId变化时，从ViewModel中查找并加载对应的物品
    LaunchedEffect(itemId, uiState.items) {
        if (itemId != null) {
            // 如果ViewModel中还没有数据，先加载
            if (uiState.items.isEmpty()) {
                viewModel.loadInventory()
            } else {
                // 从已加载的数据中查找对应的物品
                loadedItem = uiState.items.find { it.id == itemId } ?: editingItem
            }
        }
    }
    
    // 表单状态 - 使用loadedItem作为数据源
    var itemName by remember { mutableStateOf(loadedItem?.name ?: "") }
    var category by remember { mutableStateOf(loadedItem?.category ?: "") }
    var quantity by remember { mutableStateOf(loadedItem?.quantity?.toString() ?: "") }
    var unit by remember { mutableStateOf(loadedItem?.unit ?: "") }
    var minQuantity by remember { mutableStateOf(loadedItem?.minQuantity?.toString() ?: "") }
    var description by remember { mutableStateOf(loadedItem?.description ?: "") }
    var expiryDate by remember { mutableStateOf(loadedItem?.expiryDate?.let { formatDate(it) } ?: "") }
    
    // 当loadedItem变化时，更新表单状态
    LaunchedEffect(loadedItem) {
        loadedItem?.let {
            itemName = it.name
            category = it.category
            quantity = it.quantity.toString()
            unit = it.unit
            minQuantity = it.minQuantity.toString()
            description = it.description
            expiryDate = it.expiryDate?.let { date -> formatDate(date) } ?: ""
        }
    }
    
    // 分类选项 - 使用资源字符串支持国际化
    val categoryOptions = listOf(
        stringResource(R.string.categories_food) to "food",
        stringResource(R.string.categories_medicine) to "medicine",
        stringResource(R.string.categories_cleaning) to "cleaning",
        stringResource(R.string.categories_personal) to "personal",
        stringResource(R.string.categories_household) to "household",
        stringResource(R.string.categories_electronics) to "electronics"
    )
    
    // 分类下拉菜单状态
    var isCategoryExpanded by remember { mutableStateOf(false) }
    
    // 焦点请求
    val nameFocusRequester = FocusRequester()
    val quantityFocusRequester = FocusRequester()
    
    // 背景模糊效果
    val backdrop = rememberCanvasBackdrop {
        drawRoundRect(
            color = colors.container,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f)
        )
    }
    
    // 日期选择器状态
    var showDatePicker by remember { mutableStateOf(false) }
    
    // 处理日期选择
    fun handleDateSelected(year: Int, month: Int, day: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day)
        val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        expiryDate = formattedDate
        showDatePicker = false
    }
    
    // 表单验证
    fun isValidForm(): Boolean {
        return itemName.isNotBlank() && category.isNotBlank() && 
               quantity.isNotBlank() && unit.isNotBlank() && 
               minQuantity.isNotBlank() && quantity.toIntOrNull() != null && 
               minQuantity.toIntOrNull() != null && 
               unit.length <= 20 // 限制单位长度，防止过长输入
    }
    
    // 判断当前是否处于编辑模式
    val isEditingMode = (loadedItem ?: editingItem) != null
    
    // 处理保存
    fun handleSave() {
        if (isValidForm()) {
            val item = InventoryItem(
                id = (loadedItem ?: editingItem)?.id ?: UUID.randomUUID().toString(),
                name = itemName,
                category = category,
                quantity = quantity.toInt(),
                unit = unit,
                minQuantity = minQuantity.toInt(),
                description = description,
                expiryDate = if (expiryDate.isNotBlank()) {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(expiryDate)
                } else null
            )
            
            // 根据是编辑还是添加调用不同的 ViewModel API，并显示 Toast
            if (isEditingMode) {
                viewModel.updateItem(item) { success, message ->
                    if (success) {
                        Toast.makeText(context, context.getString(R.string.add_update) + "：成功", Toast.LENGTH_SHORT).show()
                        onItemUpdated(item)
                        // 更新成功后返回上一个页面
                        onBackPress()
                    } else {
                        Toast.makeText(context, "更新失败: ${message ?: "未知错误"}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                viewModel.addItem(item) { success, message ->
                    if (success) {
                        Toast.makeText(context, context.getString(R.string.add_save) + "：成功", Toast.LENGTH_SHORT).show()
                        onItemAdded(item)

                        // 重置表单
                        itemName = ""
                        category = ""
                        quantity = ""
                        unit = ""
                        minQuantity = ""
                        description = ""
                        expiryDate = ""
                    } else {
                        Toast.makeText(context, "添加失败: ${message ?: "未知错误"}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize().background(if (isLightTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A))) {
        // 主内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题由统一导航/外层管理，不在此处显示
            
            // 表单卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(24.dp) },
                        effects = {}
                    )
                    .alpha(colors.alpha)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 物品名称
                    GlassTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        placeholder = stringResource(R.string.add_name),
                        label = stringResource(R.string.add_name),
                        focusRequester = nameFocusRequester,
                        colors = colors
                    )
                    
                    // 分类选择
                    GlassDropdownMenu(
                        selectedItem = category,
                        items = categoryOptions.map { it.first },
                        onItemSelected = { displayName ->
                            val selected = categoryOptions.find { it.first == displayName }
                            category = selected?.second ?: ""
                        },
                        placeholder = stringResource(R.string.add_selectcategory),
                        colors = glassColorScheme
                    )
                    
                    // 数量和单位
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 数量
                        Column(modifier = Modifier.weight(1f)) {
                            GlassTextField(
                                value = quantity,
                                onValueChange = { 
                                    if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                        quantity = it
                                    }
                                },
                                placeholder = "0",
                                label = stringResource(R.string.add_quantity),
                                keyboardType = KeyboardType.Number,
                                focusRequester = quantityFocusRequester,
                                colors = colors
                            )
                        }
                        
                        // 单位 - 改为文本输入框支持自定义
                        Column(modifier = Modifier.weight(1f)) {
                            GlassTextField(
                                value = unit,
                                onValueChange = { unit = it },
                                placeholder = stringResource(R.string.inventory_unit),
                                label = stringResource(R.string.inventory_unit),
                                colors = colors
                            )
                        }
                    }
                    
                    // 最小库存
                    GlassTextField(
                        value = minQuantity,
                        onValueChange = { 
                            if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                minQuantity = it
                            }
                        },
                        placeholder = "0",
                        label = stringResource(R.string.add_minquantity),
                        keyboardType = KeyboardType.Number,
                        colors = colors
                    )
                    
                    // 描述
                    GlassTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = stringResource(R.string.add_description),
                        label = stringResource(R.string.add_description),
                        maxLines = 3,
                        colors = colors
                    )
                    
                    // 过期日期（点击任意位置打开选择器，不可直接输入）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    ) {
                        GlassTextField(
                            value = expiryDate,
                            onValueChange = { },
                            placeholder = stringResource(R.string.add_expirydate),
                            label = stringResource(R.string.add_expirydate),
                            enabled = false,
                            colors = colors,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.CalendarToday,
                                    contentDescription = null,
                                    tint = colors.text.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                    
                    // 按钮区域 - 两按钮等宽
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 取消按钮
                        Box(modifier = Modifier.weight(1f)) {
                            GlassButton(
                                onClick = {
                                    // 调用返回函数
                                    onBackPress()
                                },
                                label = stringResource(R.string.add_cancel),
                                isSecondary = true,
                                colors = colors,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // 保存按钮
                        Box(modifier = Modifier.weight(1f)) {
                            GlassButton(
                                onClick = { handleSave() },
                                label = if (editingItem != null) stringResource(R.string.add_update) else stringResource(R.string.add_save),
                                isEnabled = isValidForm(),
                                colors = colors,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            
            // 底部间距
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
    
    // 日期选择器对话框
    if (showDatePicker) {
        GlassDatePickerDialog(
            onDateSelected = ::handleDateSelected,
            onDismiss = { showDatePicker = false },
            colors = colors
        )
    }
}

// 玻璃态文本输入框
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    maxLines: Int = 1,
    focusRequester: FocusRequester? = null,
    colors: GlassColors,
    enabled: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column {
        Text(
            text = label,
            style = GlassTypography.bodySmall.copy(
                color = colors.text.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        val textFieldBackdrop = rememberCanvasBackdrop {
            drawRoundRect(
                color = colors.textField,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
            )
        }
        
        Box(
            modifier = Modifier
                .drawBackdrop(
                    backdrop = textFieldBackdrop,
                    shape = { RoundedCornerShape(12.dp) },
                    effects = {}
                )
                .alpha(colors.alpha)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                textStyle = GlassTypography.bodyMedium.copy(color = colors.text),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                maxLines = maxLines,
                decorationBox = { innerTextField ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = GlassTypography.bodyMedium.copy(
                                        color = colors.text.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                        if (trailingIcon != null) {
                            trailingIcon()
                        }
                    }
                }
            )
        }
    }
}

// 玻璃态日期选择器字段
@Composable
fun GlassDatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    label: String,
    showPicker: () -> Unit,
    colors: GlassColors
) {
    Column {
        Text(
            text = label,
            style = GlassTypography.bodySmall.copy(
                color = colors.text.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        val dateFieldBackdrop = rememberCanvasBackdrop {
            drawRoundRect(
                color = colors.textField,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
            )
        }
        
        Box(
            modifier = Modifier
                .drawBackdrop(
                    backdrop = dateFieldBackdrop,
                    shape = { RoundedCornerShape(12.dp) },
                    effects = {}
                )
                .alpha(colors.alpha)
                .clickable(onClick = showPicker)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (value.isEmpty()) placeholder else value,
                    style = GlassTypography.bodyMedium.copy(
                        color = if (value.isEmpty()) 
                            colors.text.copy(alpha = 0.5f) 
                            else colors.text
                    )
                )
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = colors.text.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// 玻璃态按钮
@Composable
fun GlassButton(
    onClick: () -> Unit,
    label: String,
    isSecondary: Boolean = false,
    isEnabled: Boolean = true,
    colors: GlassColors,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val buttonBackdrop = rememberCanvasBackdrop {
        drawRoundRect(
            color = if (isSecondary) 
                colors.secondaryButton 
                else if (isEnabled) 
                    colors.primary 
                    else colors.disabledButton,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
        )
    }
    
    Box(
        modifier = modifier.then(
            Modifier
                .drawBackdrop(
                    backdrop = buttonBackdrop,
                    shape = { RoundedCornerShape(12.dp) },
                    effects = {}
                )
                .alpha(if (isEnabled) colors.alpha else 0.6f)
                .clickable(
                    indication = LocalIndication.current,
                    interactionSource = remember { MutableInteractionSource() },
                    enabled = isEnabled,
                    onClick = onClick
                )
                .padding(vertical = 12.dp)
        ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = GlassTypography.bodyMedium.copy(
                color = if (isSecondary) colors.text else Color.White,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

// 玻璃态日期选择器对话框
@Composable
fun GlassDatePickerDialog(
    onDateSelected: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit,
    colors: GlassColors
) {
    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
    
    var selectedYear by remember { mutableStateOf(currentYear) }
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var selectedDay by remember { mutableStateOf(currentDay) }
    
    val daysInMonth = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance()
        cal.set(selectedYear, selectedMonth + 1, 0)
        cal.get(Calendar.DAY_OF_MONTH)
    }
    
    val dialogBackdrop = rememberCanvasBackdrop {
        drawRoundRect(
            color = colors.dialog,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f)
        )
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .drawBackdrop(
                    backdrop = dialogBackdrop,
                    shape = { RoundedCornerShape(24.dp) },
                    effects = {}
                )
                .alpha(colors.alpha)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.add_expirydate),
                    style = GlassTypography.headlineSmall.copy(
                        color = colors.text,
                        fontWeight = FontWeight.Bold
                    )
                )
                
                // 年份选择器
                Row(modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { selectedYear-- }) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = null,
                            tint = colors.text
                        )
                    }
                    Text(
                        text = selectedYear.toString(),
                        style = GlassTypography.headlineMedium.copy(color = colors.text),
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    IconButton(onClick = { selectedYear++ }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = colors.text
                        )
                    }
                }
                
                // 月份选择器
                Row(modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { 
                        selectedMonth = (selectedMonth - 1 + 12) % 12
                    }) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = null,
                            tint = colors.text
                        )
                    }
                    Text(
                        text = (selectedMonth + 1).toString(),
                        style = GlassTypography.headlineMedium.copy(color = colors.text),
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    IconButton(onClick = { 
                        selectedMonth = (selectedMonth + 1) % 12
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = colors.text
                        )
                    }
                }
                
                // 日期选择器
                Row(modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { 
                        if (selectedDay > 1) selectedDay--
                    }) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = null,
                            tint = colors.text
                        )
                    }
                    Text(
                        text = selectedDay.toString(),
                        style = GlassTypography.headlineMedium.copy(color = colors.text),
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    IconButton(onClick = { 
                        if (selectedDay < daysInMonth) selectedDay++
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = colors.text
                        )
                    }
                }
                
                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        GlassButton(
                            onClick = onDismiss,
                            label = stringResource(R.string.add_cancel),
                            isSecondary = true,
                            colors = colors
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        GlassButton(
                            onClick = { 
                                onDateSelected(selectedYear, selectedMonth, selectedDay)
                            },
                            label = stringResource(R.string.add_save),
                            colors = colors
                        )
                    }
                }
            }
        }
    }
}

// 玻璃态颜色数据类
@Stable
class GlassColors(
    val primary: Color,
    val background: Color,
    val surface: Color,
    val text: Color,
    val border: Color,
    val error: Color,
    val container: Color,
    val textField: Color,
    val selectedContainer: Color,
    val secondaryButton: Color,
    val disabledButton: Color,
    val dialog: Color,
    val alpha: Float
)
