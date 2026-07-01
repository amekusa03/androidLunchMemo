package com.kusa.lunchmemo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kusa.lunchmemo.data.AppSettingsEntity
import com.kusa.lunchmemo.data.ComponentConfig
import com.kusa.lunchmemo.data.ComponentType
import com.kusa.lunchmemo.data.LunchMemoViewModel
import com.kusa.lunchmemo.notification.NotificationScheduler
import com.kusa.lunchmemo.ui.theme.LunchMemoTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.absoluteValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LunchMemoTheme {
                val context = LocalContext.current
                val viewModel: LunchMemoViewModel = viewModel()
                val settings by viewModel.settings.collectAsState()
                
                // 通知権限の要求 (Android 13以上)
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        settings?.let {
                            NotificationScheduler.scheduleDailyNotification(
                                context, it.notificationHour, it.notificationMinute
                            )
                        }
                    }
                }

                LaunchedEffect(settings) {
                    val currentSettings = settings ?: return@LaunchedEffect
                    android.util.Log.d("NotifScheduler", "LaunchedEffect fired: hour=${currentSettings.notificationHour}, minute=${currentSettings.notificationMinute}")
                    // 通知権限の確認と要求 (Android 13以上)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    
                    // アプリ起動時や設定変更時にアラームを確実にセットする
                    NotificationScheduler.scheduleDailyNotification(
                        context, currentSettings.notificationHour, currentSettings.notificationMinute
                    )
                }

                LunchMemoScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LunchMemoScreen(
    viewModel: LunchMemoViewModel = viewModel()
) {
    val memos by viewModel.memos.collectAsState()
    val settings by viewModel.settings.collectAsState()
    var showTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.cleanOldMemos()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🍱", fontSize = 24.sp)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            "Lunch Memo 2",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showTimePicker = true }) {
                        Text("⚙️", fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        if (showTimePicker) {
            val currentSettings = settings ?: AppSettingsEntity()
            SettingsDialog(
                settings = currentSettings,
                onDismiss = { showTimePicker = false },
                onTimeSelected = { hour, minute ->
                    viewModel.updateNotificationTime(hour, minute)
                    NotificationScheduler.scheduleDailyNotification(context, hour, minute)
                },
                onAlphanumericToggle = { enabled ->
                    viewModel.updateAlphanumericOnly(enabled)
                },
                onComponentUpdate = { index, config ->
                    viewModel.updateComponentConfig(index, config)
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            LunchMemoContent(
                memos = memos,
                settings = settings,
                onMemoChange = { date, memo -> viewModel.saveMemo(date, memo) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    settings: AppSettingsEntity,
    onDismiss: () -> Unit,
    onTimeSelected: (Int, Int) -> Unit,
    onAlphanumericToggle: (Boolean) -> Unit,
    onComponentUpdate: (Int, ComponentConfig) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = settings.notificationHour,
        initialMinute = settings.notificationMinute,
        is24Hour = true
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "設定",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // 通知時刻
                Text(
                    "通知時刻",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 8.dp)
                )
                TimePicker(state = timePickerState)
                
                Button(
                    onClick = { onTimeSelected(timePickerState.hour, timePickerState.minute) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text("通知時刻を保存")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // 各コンポーネントの設定
                ComponentSettingSection(1, ComponentConfig.deserialize(settings.component1ConfigJson), onComponentUpdate)
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                ComponentSettingSection(2, ComponentConfig.deserialize(settings.component2ConfigJson), onComponentUpdate)
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                ComponentSettingSection(3, ComponentConfig.deserialize(settings.component3ConfigJson), onComponentUpdate)

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("閉じる")
                }
            }
        }
    }
}

@Composable
fun ComponentSettingSection(
    index: Int,
    config: ComponentConfig,
    onUpdate: (Int, ComponentConfig) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "要素 $index",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            ComponentType.values().forEach { type ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                    RadioButton(
                        selected = config.type == type,
                        onClick = { onUpdate(index, config.copy(type = type)) }
                    )
                    Text(text = when(type) {
                        ComponentType.SELECTION -> "選択"
                        ComponentType.NUMERIC -> "数字"
                        ComponentType.TEXT -> "文字"
                    }, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        when (config.type) {
            ComponentType.SELECTION -> {
                var optionsText by remember(config) { mutableStateOf(config.options.joinToString(",")) }
                OutlinedTextField(
                    value = optionsText,
                    onValueChange = { 
                        optionsText = it
                        val newOptions = it.split(",").map { s -> s.trim() }.filter { s -> s.isNotEmpty() }
                        onUpdate(index, config.copy(options = newOptions))
                    },
                    label = { Text("選択肢 (カンマ区切り)") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodySmall
                )
            }
            ComponentType.NUMERIC -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("桁数制限 (1-4): ", style = MaterialTheme.typography.bodySmall)
                    (1..4).forEach { digit ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = config.digitLimit == digit,
                                onClick = { onUpdate(index, config.copy(digitLimit = digit)) }
                            )
                            Text(digit.toString(), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            ComponentType.TEXT -> {
                Text("自由な文字入力が可能です", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun LunchMemoContent(
    memos: Map<LocalDate, String>,
    settings: AppSettingsEntity?,
    onMemoChange: (LocalDate, String) -> Unit
) {
    val pageCount = 2000
    val initialPage = pageCount / 2
    val pagerState = rememberPagerState(pageCount = { pageCount }, initialPage = initialPage)
    val today = LocalDate.now()

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 44.dp),
        pageSpacing = 16.dp,
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) { page ->
        val date = today.plusDays((page - initialPage).toLong())
        val isSelected = pagerState.currentPage == page
        
        val pageOffset = (
                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                ).absoluteValue

        LunchMemoCard(
            date = date,
            memo = memos[date] ?: "",
            isEditable = isSelected,
            settings = settings,
            onMemoChange = { onMemoChange(date, it) },
            modifier = Modifier.graphicsLayer {
                val fraction = 1f - pageOffset.coerceIn(0f, 1f)
                alpha = lerp(start = 0.5f, stop = 1f, fraction = fraction)
                scaleX = lerp(start = 0.85f, stop = 1f, fraction = fraction)
                scaleY = lerp(start = 0.85f, stop = 1f, fraction = fraction)
            }
        )
    }
}

@Composable
fun LunchMemoCard(
    date: LocalDate,
    memo: String,
    isEditable: Boolean,
    settings: AppSettingsEntity?,
    onMemoChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val config1 = ComponentConfig.deserialize(settings?.component1ConfigJson ?: "")
    val config2 = ComponentConfig.deserialize(settings?.component2ConfigJson ?: "")
    val config3 = ComponentConfig.deserialize(settings?.component3ConfigJson ?: "")

    // Parse existing memo into parts (very basic parsing: split by space)
    // In a real app, you might want to save these parts separately in the DB.
    // For now, we assume the format is "Part1 Part2 Part3"
    val parts = remember(memo) { 
        val p = memo.split(" ")
        mutableStateListOf(
            if (p.isNotEmpty()) p[0] else "",
            if (p.size > 1) p[1] else "",
            if (p.size > 2) p.subList(2, p.size).joinToString(" ") else ""
        )
    }

    fun updateMemo() {
        onMemoChange(parts.joinToString(" ").trim())
    }

    val isToday = date == LocalDate.now()
    val monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
    val dayFormatter = DateTimeFormatter.ofPattern("d", Locale.getDefault())
    val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())

    val cardElevation by animateDpAsState(if (isEditable) 12.dp else 2.dp, label = "elevation")
    val cardColor by animateColorAsState(
        if (isEditable) MaterialTheme.colorScheme.surface 
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        label = "color"
    )

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(550.dp),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = cardElevation),
        colors = CardDefaults.elevatedCardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ヘッダー部分
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = date.format(monthFormatter).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = date.format(dayFormatter),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = date.format(dayOfWeekFormatter),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (isToday) {
                    SuggestionChip(
                        onClick = { },
                        label = { Text("Today", fontWeight = FontWeight.Bold) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = null,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // コンテンツ部分
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (isEditable) {
                    ComponentInput(1, config1, parts[0]) { parts[0] = it; updateMemo() }
                    Spacer(modifier = Modifier.height(12.dp))
                    ComponentInput(2, config2, parts[1]) { parts[1] = it; updateMemo() }
                    Spacer(modifier = Modifier.height(12.dp))
                    ComponentInput(3, config3, parts[2]) { parts[2] = it; updateMemo() }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = memo.ifEmpty { "No memo" },
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontStyle = if (memo.isEmpty()) FontStyle.Italic else FontStyle.Normal,
                                color = if (memo.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) 
                                        else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Light
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComponentInput(
    index: Int,
    config: ComponentConfig,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "要素 $index",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
        
        when (config.type) {
            ComponentType.SELECTION -> {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    config.options.forEach { option ->
                        FilterChip(
                            selected = value == option,
                            onClick = { onValueChange(option) },
                            label = { Text(option) }
                        )
                    }
                }
            }
            ComponentType.NUMERIC -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = { 
                        if (it.length <= config.digitLimit && it.all { c -> c.isDigit() }) {
                            onValueChange(it)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("${config.digitLimit}桁の数字") },
                    singleLine = true
                )
            }
            ComponentType.TEXT -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = { onValueChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("メモを入力") }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LunchMemoScreenPreview() {
    LunchMemoTheme {
        LunchMemoContent(
            memos = mapOf(LocalDate.now() to "Delicious lunch"),
            settings = AppSettingsEntity(),
            onMemoChange = { _, _ -> }
        )
    }
}
