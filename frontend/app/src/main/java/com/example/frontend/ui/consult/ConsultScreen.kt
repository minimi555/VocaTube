package com.example.frontend.ui.consult

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.frontend.data.remote.SchoolItem
import com.example.frontend.data.remote.SchoolSearchHistoryItem
import com.example.frontend.data.remote.SourceItem

/**
 * 学习咨询版块：双 Tab。
 * - 备考咨询：RAG 问答（CET4/CET6/IELTS/TOEFL/SAT/考研备考方法）
 * - 学校查询：Agent 搜索 QS 前 150 学校官网的英语要求，附历史记录与学校列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultScreen(
    modifier: Modifier = Modifier,
    vm: ConsultViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()

    Column(modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = state.activeTab.ordinal) {
            Tab(
                selected = state.activeTab == ConsultTab.TestPrep,
                onClick = { vm.switchTab(ConsultTab.TestPrep) },
                text = { Text("备考咨询") },
            )
            Tab(
                selected = state.activeTab == ConsultTab.SchoolSearch,
                onClick = { vm.switchTab(ConsultTab.SchoolSearch) },
                text = { Text("学校查询") },
            )
        }

        when (state.activeTab) {
            ConsultTab.TestPrep -> TestPrepTab(state, vm)
            ConsultTab.SchoolSearch -> SchoolSearchTab(state, vm)
        }
    }
}

// ---- 备考咨询 Tab ---- //

@Composable
private fun TestPrepTab(state: ConsultUiState, vm: ConsultViewModel) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        QueryRow(
            value = state.prepQuery,
            onValueChange = vm::onPrepQueryChange,
            onSubmit = vm::askPrep,
            label = "如：雅思口语如何备考",
            buttonText = "提问",
            loading = state.prepLoading,
        )

        Spacer(Modifier.height(12.dp))

        when {
            state.prepLoading -> LoadingHint("正在查询知识库…")
            state.prepError != null -> ErrorText(state.prepError)
            state.prepAnswer != null -> AnswerCard(state.prepAnswer, state.prepSources)
        }
    }
}

// ---- 学校查询 Tab ---- //

@Composable
private fun SchoolSearchTab(state: ConsultUiState, vm: ConsultViewModel) {
    LaunchedEffect(Unit) { vm.loadSchools() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        QueryRow(
            value = state.schoolQuery,
            onValueChange = vm::onSchoolQueryChange,
            onSubmit = vm::searchSchool,
            label = "如：帝国理工数据科学硕士托福要求",
            buttonText = "搜索",
            loading = state.schoolLoading,
        )

        TextButton(onClick = vm::toggleHistory) {
            Text(if (state.showHistory) "收起历史记录" else "📋 历史记录")
        }

        if (state.showHistory) {
            HistoryPanel(state.history)
            Spacer(Modifier.height(12.dp))
        }

        when {
            state.schoolLoading -> LoadingHint("正在搜索学校官网，可能需要 10-30 秒…")
            state.schoolError != null -> ErrorText(state.schoolError)
            state.schoolAnswer != null -> AnswerCard(state.schoolAnswer, sources = emptyList())
        }

        Spacer(Modifier.height(12.dp))
        SchoolListSection(state.schools)
    }
}

// ---- 通用组件 ---- //

@Composable
private fun QueryRow(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    label: String,
    buttonText: String,
    loading: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        )
        Spacer(Modifier.width(8.dp))
        Button(onClick = onSubmit, enabled = !loading) {
            Text(buttonText)
        }
    }
}

@Composable
private fun LoadingHint(hint: String) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CircularProgressIndicator()
        Text(
            hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ErrorText(message: String) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyLarge,
    )
}

/** 回答卡片：正文可选中复制（Agent 回答常含 URL），下方可选展示参考来源。 */
@Composable
private fun AnswerCard(answer: String, sources: List<SourceItem>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SelectionContainer {
                Text(answer, style = MaterialTheme.typography.bodyLarge)
            }
            if (sources.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    "参考来源",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                sources.forEach { s ->
                    val label = listOfNotNull(
                        s.source?.takeIf { it.isNotBlank() },
                        s.section?.takeIf { it.isNotBlank() },
                    ).joinToString(" / ")
                    if (label.isNotEmpty()) {
                        Text(
                            "• $label",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryPanel(items: List<SchoolSearchHistoryItem>) {
    Card(Modifier.fillMaxWidth()) {
        if (items.isEmpty()) {
            Text(
                "暂无搜索记录",
                Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                Modifier.heightIn(max = 280.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { item -> HistoryRow(item) }
            }
        }
    }
}

@Composable
private fun HistoryRow(item: SchoolSearchHistoryItem) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        TextButton(onClick = { expanded = !expanded }) {
            Text(item.question, style = MaterialTheme.typography.bodyLarge)
        }
        // ISO 时间 "2026-07-19T10:30:00" 直接取 "日期 时:分" 展示，避免引入日期库
        Text(
            item.createdAt.replace("T", " ").take(16),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (expanded) {
            SelectionContainer {
                Text(
                    item.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(Modifier.padding(top = 8.dp))
    }
}

/** QS 前 150 学校列表，默认折叠。 */
@Composable
private fun SchoolListSection(schools: List<SchoolItem>) {
    if (schools.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }

    TextButton(onClick = { expanded = !expanded }) {
        Text(if (expanded) "收起学校列表" else "🏫 QS 前 150 学校列表（${schools.size}）")
    }
    if (expanded) {
        Card(Modifier.fillMaxWidth()) {
            LazyColumn(
                Modifier.heightIn(max = 320.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(schools, key = { it.id }) { school ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            "${school.qsRank}. ${school.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            school.domain,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
