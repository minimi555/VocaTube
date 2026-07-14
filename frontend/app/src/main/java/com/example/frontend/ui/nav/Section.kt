package com.example.frontend.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 底部导航的 4 大版块，从左到右：查词 / 视频学习 / 生词本 / 学习咨询。
 * route 用于 NavHost，label 是中文标签，icon 是底部导航图标。
 */
enum class Section(val route: String, val label: String, val icon: ImageVector) {
    Dictionary("dictionary", "查词", Icons.Filled.Search),
    VideoLearn("video_learn", "视频学习", Icons.Filled.PlayArrow),
    Wordbook("wordbook", "生词本", Icons.Filled.Star),
    Consult("consult", "学习咨询", Icons.Filled.Email);

    companion object {
        val START = Dictionary
    }
}
