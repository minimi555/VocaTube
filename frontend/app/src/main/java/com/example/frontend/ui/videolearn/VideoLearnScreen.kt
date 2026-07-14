package com.example.frontend.ui.videolearn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.frontend.data.subtitle.Subtitles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 视频学习版块。左侧抽屉列出所有视频 title；点击某个 title 关闭抽屉并在主屏
 * 上半部分播放视频，叠加双语字幕（英文在上、中文在下）。主屏下半部分留空，
 * 预留后续「根据英文字幕出题」功能。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoLearnScreen(
    modifier: Modifier = Modifier,
    vm: VideoLearnViewModel = viewModel(),
) {
    val listState by vm.listState.collectAsStateWithLifecycle()
    val playback by vm.playback.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Open)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "视频列表",
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold,
                )
                HorizontalDivider()
                when (val s = listState) {
                    is VideoListState.Loading ->
                        Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    is VideoListState.Error ->
                        Text(
                            s.message,
                            modifier = Modifier.padding(16.dp),
                            color = Color.Red,
                        )
                    is VideoListState.Success ->
                        LazyColumn {
                            items(s.videos) { video ->
                                Text(
                                    text = video.title,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            vm.selectVideo(video.id)
                                            scope.launch { drawerState.close() }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                )
                                HorizontalDivider()
                            }
                        }
                }
            }
        },
    ) {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = { Text(playback?.title ?: "视频学习") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "打开视频列表")
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(Modifier.fillMaxSize().padding(innerPadding)) {
                // 上半部分：视频 + 双语字幕叠加
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    val pb = playback
                    if (pb == null) {
                        Text("从左侧列表选择一个视频开始学习")
                    } else {
                        VideoPlayer(
                            videoUrl = pb.videoUrl,
                            enSubtitles = remember(pb.id, pb.enCues) { Subtitles(pb.enCues) },
                            zhSubtitles = remember(pb.id, pb.zhCues) { Subtitles(pb.zhCues) },
                        )
                    }
                }
                HorizontalDivider()
                // 下半部分：留空（后续按英文字幕出题）
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("练习区（开发中）", color = Color.Gray)
                }
            }
        }
    }
}

/**
 * ExoPlayer 播放器 + 底部双语字幕叠加。英文在上，中文在下。
 * 用轮询 player.currentPosition 的方式驱动字幕更新。
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun VideoPlayer(
    videoUrl: String,
    enSubtitles: Subtitles,
    zhSubtitles: Subtitles,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = false
        }
    }

    // 切换视频时更新播放源
    LaunchedEffect(videoUrl) {
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUrl))
        exoPlayer.prepare()
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    // 轮询播放位置，驱动字幕
    var positionMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(exoPlayer) {
        while (true) {
            positionMs = exoPlayer.currentPosition
            delay(200)
        }
    }

    val enText = enSubtitles.textAt(positionMs)
    val zhText = zhSubtitles.textAt(positionMs)

    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        // 字幕叠加在视频底部
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 48.dp, start = 12.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (!enText.isNullOrBlank()) {
                Text(
                    text = enText,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(Color(0xCC000000))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            if (!zhText.isNullOrBlank()) {
                Text(
                    text = zhText,
                    color = Color(0xFFFFF176),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(Color(0xCC000000))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}
