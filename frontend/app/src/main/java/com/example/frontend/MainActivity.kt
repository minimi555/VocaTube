package com.example.frontend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
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
        bottomBar = {
            BottomNav(
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

@Composable
private fun BottomNav(
    currentRoute: String,
    onNavigate: (Section) -> Unit
) {
    NavigationBar {
        Section.entries.forEach { section ->
            NavigationBarItem(
                selected = currentRoute == section.route,
                onClick = { onNavigate(section) },
                icon = { Icon(section.icon, contentDescription = section.label) },
                label = { Text(section.label) },
            )
        }
    }
}
