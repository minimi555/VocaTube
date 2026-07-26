package com.example.frontend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.frontend.ui.consult.ConsultScreen
import com.example.frontend.ui.dictionary.DictionaryScreen
import com.example.frontend.ui.nav.Section
import com.example.frontend.ui.theme.FrontendTheme
import com.example.frontend.ui.videolearn.VideoLearnScreen
import com.example.frontend.ui.wordbook.WordbookScreen

/**
 * VocaTube 主 Activity：底部导航 4 大版块（查词 / 视频学习 / 生词本 / 学习咨询），
 * NavHost 切换对应 Screen。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FrontendTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route ?: Section.START.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            FloatingBottomNav(
                currentRoute = currentRoute,
                onNavigate = { section ->
                    navController.navigate(section.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Section.START.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Section.Dictionary.route) { DictionaryScreen() }
            composable(Section.VideoLearn.route) { VideoLearnScreen() }
            composable(Section.Wordbook.route) { WordbookScreen() }
            composable(Section.Consult.route) { ConsultScreen() }
        }
    }
}

/**
 * 悬浮胶囊底部导航栏：整条 Surface 圆角悬浮于屏幕底部边缘之上，
 * 选中项的图标带实心圆角高亮背景。
 */
@Composable
private fun FloatingBottomNav(
    currentRoute: String,
    onNavigate: (Section) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Section.entries.forEach { section ->
                NavPillItem(
                    section = section,
                    selected = currentRoute == section.route,
                    onClick = { onNavigate(section) },
                )
            }
        }
    }
}

@Composable
private fun NavPillItem(
    section: Section,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val labelColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val iconTint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Icon(section.icon, contentDescription = section.label, tint = iconTint)
        }
        Text(
            text = section.label,
            color = labelColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
