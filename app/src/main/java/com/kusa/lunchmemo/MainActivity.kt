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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.Switch
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kusa.lunchmemo.data.AppSettingsEntity
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
                    
                    // AlarmManagerによる予約は時刻設定更新時のみ行うようにし、
                    // ここでの毎回の予約は避ける（NotificationReceiverでも行われるため）
                    // ただし初回起動時などのために設定が存在する場合は一度だけ実行される
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
                        Text("🍽️", fontSize = 24.sp)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            "Lunch Memo",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showTimePicker = true }) {
                        Text("🔔", fontSize = 20.sp)
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
            TimePickerDialog(
                settings = currentSettings,
                onDismiss = { showTimePicker = false },
                onTimeSelected = { hour, minute ->
                    viewModel.updateNotificationTime(hour, minute)
                    NotificationScheduler.scheduleDailyNotification(context, hour, minute)
                    showTimePicker = false
                },
                onAlphanumericToggle = { enabled ->
                    viewModel.updateAlphanumericOnly(enabled)
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
fun TimePickerDialog(
    settings: AppSettingsEntity,
    onDismiss: () -> Unit,
    onTimeSelected: (Int, Int) -> Unit,
    onAlphanumericToggle: (Boolean) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = settings.notificationHour,
        initialMinute = settings.notificationMinute,
        is24Hour = true
    )

    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "設定",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                Text(
                    "通知時刻",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                )
                TimePicker(state = timePickerState)

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "英数字のみ入力",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "ONにすると記号・英数字以外を入力できません",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.alphanumericOnly,
                        onCheckedChange = onAlphanumericToggle
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    androidx.compose.material3.TextButton(onClick = onDismiss) {
                        Text("閉じる")
                    }
                    androidx.compose.material3.TextButton(
                        onClick = { onTimeSelected(timePickerState.hour, timePickerState.minute) }
                    ) {
                        Text("時刻を保存")
                    }
                }
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
            alphanumericOnly = settings?.alphanumericOnly ?: false,
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
    alphanumericOnly: Boolean,
    onMemoChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(memo)) }

    // 外部（ViewModel/DB）からの変更を同期
    LaunchedEffect(memo) {
        if (textFieldValue.text != memo) {
            textFieldValue = textFieldValue.copy(text = memo)
        }
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
            .height(500.dp), // フォント拡大に合わせて少し高く
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = cardElevation),
        colors = CardDefaults.elevatedCardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
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

            Spacer(modifier = Modifier.height(32.dp))

            // コンテンツ部分
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isEditable) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
                        else Color.Transparent
                    )
                    .padding(if (isEditable) 8.dp else 0.dp)
            ) {
                if (isEditable) {
                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            val filteredValue = if (alphanumericOnly) {
                                val filteredText = newValue.text.filter { it.isLetterOrDigit() || it.isWhitespace() }
                                if (filteredText != newValue.text) {
                                    // 制限に抵触した場合は、制限後の文字で構成し直し
                                    newValue.copy(text = filteredText, composition = null)
                                } else {
                                    newValue
                                }
                            } else {
                                newValue
                            }
                            
                            textFieldValue = filteredValue
                            onMemoChange(filteredValue.text)
                        },
                        modifier = Modifier.fillMaxSize(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (alphanumericOnly) KeyboardType.Ascii else KeyboardType.Text
                        ),
                        placeholder = { 
                            Text(
                                "What's for lunch?",
                                style = MaterialTheme.typography.headlineSmall, // プレースホルダーも大きく
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            ) 
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.headlineMedium.copy( // 入力文字を大きく
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Start
                        )
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = memo.ifEmpty { "No memo" },
                            style = MaterialTheme.typography.headlineMedium.copy( // 表示文字も大きく
                                fontStyle = if (memo.isEmpty()) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                                color = if (memo.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) 
                                        else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Light
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            if (isEditable) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.2f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        .align(Alignment.CenterHorizontally)
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
