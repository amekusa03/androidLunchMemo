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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kusa.lunchmemo.data.LunchMemoViewModel
import com.kusa.lunchmemo.notification.NotificationScheduler
import com.kusa.lunchmemo.ui.theme.LunchMemoTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LunchMemoTheme {
                val context = LocalContext.current
                
                // 通知権限の要求 (Android 13以上)
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        NotificationScheduler.scheduleDailyNotification(context)
                    }
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            NotificationScheduler.scheduleDailyNotification(context)
                        }
                    } else {
                        NotificationScheduler.scheduleDailyNotification(context)
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LunchMemoScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun LunchMemoScreen(
    modifier: Modifier = Modifier,
    viewModel: LunchMemoViewModel = viewModel()
) {
    // ViewModelからメモの状態を取得
    val memos by viewModel.memos.collectAsState()

    // 起動時に古いメモを削除
    LaunchedEffect(Unit) {
        viewModel.cleanOldMemos()
    }

    LunchMemoContent(
        modifier = modifier,
        memos = memos,
        onMemoChange = { date, memo -> viewModel.saveMemo(date, memo) }
    )
}

@Composable
fun LunchMemoContent(
    memos: Map<LocalDate, String>,
    onMemoChange: (LocalDate, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val pageCount = 2000
    val initialPage = pageCount / 2
    val pagerState = rememberPagerState(pageCount = { pageCount }, initialPage = initialPage)
    val today = LocalDate.now()

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ランチメモ",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp),
            fontWeight = FontWeight.Bold
        )

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 48.dp),
            pageSpacing = 16.dp,
            modifier = Modifier.weight(1f)
        ) { page ->
            val date = today.plusDays((page - initialPage).toLong())
            val isSelected = pagerState.currentPage == page
            
            // ページの距離に応じてカードを縮小させるアニメーション効果
            val pageOffset = (
                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    ).absoluteValue

            LunchMemoCard(
                date = date,
                memo = memos[date] ?: "",
                isEditable = isSelected,
                onMemoChange = { onMemoChange(date, it) },
                modifier = Modifier.graphicsLayer {
                    alpha = lerp(
                        start = 0.5f,
                        stop = 1f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    )
                    scaleY = lerp(
                        start = 0.85f,
                        stop = 1f,
                        fraction = 1f - pageOffset.coerceIn(0f, 1f)
                    )
                }
            )
        }
    }
}

@Composable
fun LunchMemoCard(
    date: LocalDate,
    memo: String,
    isEditable: Boolean,
    onMemoChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("M/d (E)")
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEditable) MaterialTheme.colorScheme.surfaceVariant 
                            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = date.format(dateFormatter),
                style = MaterialTheme.typography.titleLarge,
                color = if (isEditable) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (date == LocalDate.now()) "今日" else "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            if (isEditable) {
                OutlinedTextField(
                    value = memo,
                    onValueChange = onMemoChange,
                    label = { Text("ランチの内容をメモ") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .weight(1f),
                    placeholder = { Text("例: A定食（ハンバーグ）") }
                )
            } else {
                Text(
                    text = memo.ifEmpty { "メモなし" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                        .weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
            memos = mapOf(LocalDate.now() to "今日のランチ"),
            onMemoChange = { _, _ -> }
        )
    }
}
