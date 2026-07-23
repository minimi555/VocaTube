package com.example.frontend.ui.videolearn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend.data.remote.QuizClozeResult
import com.example.frontend.data.remote.QuizReadingQuestion
import com.example.frontend.data.remote.QuizReadingResult
import com.example.frontend.data.remote.QuizScore

/**
 * 视频学习页下半部分的练习区 UI。
 *
 * 根据 [quizState] 分支显示：生成按钮、生成中、作答中、批改中、结果、错误。
 */
@Composable
fun QuizSection(
    quizState: QuizUiState,
    currentBook: String?,
    hasVideo: Boolean,
    hasSubtitles: Boolean,
    onGenerate: () -> Unit,
    onSubmit: (clozeAnswers: Map<String, String>, readingAnswers: Map<String, String>) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 选词填空答案 — key="1".."10", value=用户输入。
    val clozeInputs = remember { mutableStateMapOf<String, String>() }
    // 阅读理解答案 — key="1","2", value="A"/"B"/"C"/"D"。
    val readingSelections = remember { mutableStateMapOf<String, String>() }

    // 进入新的作答态时清空旧答案。
    LaunchedEffect(quizState) {
        if (quizState is QuizUiState.Answering) {
            clozeInputs.clear()
            readingSelections.clear()
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (quizState) {
            is QuizUiState.Idle -> IdleCard(
                currentBook = currentBook,
                hasVideo = hasVideo,
                hasSubtitles = hasSubtitles,
                onGenerate = onGenerate,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )

            is QuizUiState.Generating -> ProgressColumn("正在生成题目，请稍候（约30-60秒）...")
            is QuizUiState.Grading -> ProgressColumn("正在批改...")

            is QuizUiState.Answering -> AnsweringContent(
                state = quizState,
                clozeInputs = clozeInputs,
                readingSelections = readingSelections,
                onSubmit = onSubmit,
                modifier = Modifier.fillMaxSize(),
            )

            is QuizUiState.Result -> ResultContent(
                score = quizState.score,
                clozeResults = quizState.clozeResults,
                readingResults = quizState.readingResults,
                onReset = onReset,
                modifier = Modifier.fillMaxSize(),
            )

            is QuizUiState.Error -> ErrorColumn(message = quizState.message, onReset = onReset)
        }
    }
}

@Composable
private fun IdleCard(
    currentBook: String?,
    hasVideo: Boolean,
    hasSubtitles: Boolean,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canGenerate = hasVideo && hasSubtitles && currentBook != null
    val hint = when {
        !hasVideo -> "请先选择视频"
        !hasSubtitles -> "该视频无英文字幕"
        currentBook == null -> "请先在生词本选择单词书"
        else -> "当前单词书：$currentBook"
    }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "根据视频字幕生成练习题",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = if (canGenerate) MaterialTheme.colorScheme.primary else Color.Gray,
            )
            Button(
                onClick = onGenerate,
                enabled = canGenerate,
            ) {
                Text("生成练习")
            }
        }
    }
}

@Composable
private fun ProgressColumn(text: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator()
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ErrorColumn(message: String, onReset: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(16.dp),
    ) {
        Text(message, color = Color.Red, textAlign = TextAlign.Center)
        Button(onClick = onReset) { Text("重试") }
    }
}

@Composable
private fun AnsweringContent(
    state: QuizUiState.Answering,
    clozeInputs: MutableMap<String, String>,
    readingSelections: MutableMap<String, String>,
    onSubmit: (Map<String, String>, Map<String, String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clozeCount = state.clozeCount.coerceAtLeast(0)
    val allClozeAnswered = (1..clozeCount).all { i ->
        clozeInputs[i.toString()].orEmpty().isNotBlank()
    }
    val allReadingAnswered = state.readingQuestions.all { q ->
        readingSelections[q.index.toString()].orEmpty().isNotBlank()
    }
    val canSubmit = allClozeAnswered && allReadingAnswered

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "一、选词填空",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        item {
            ClozePassageText(passage = state.clozePassage)
        }

        items(clozeCount) { idx ->
            val number = (idx + 1).toString()
            OutlinedTextField(
                value = clozeInputs[number] ?: "",
                onValueChange = { clozeInputs[number] = it },
                label = { Text("第 ${idx + 1} 空") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

        item {
            Text(
                text = "二、阅读理解",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        item {
            Text(
                text = state.readingPassage,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
            )
        }

        state.readingQuestions.forEach { question ->
            item {
                ReadingQuestionCard(
                    question = question,
                    selected = readingSelections[question.index.toString()],
                    onSelected = { readingSelections[question.index.toString()] = it },
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onSubmit(clozeInputs.toMap(), readingSelections.toMap()) },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("提交答案")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ClozePassageText(passage: String) {
    val parts = remember(passage) { passage.split("____") }

    if (parts.size <= 1) {
        Text(text = passage, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
        return
    }

    // 将段落与 "____" 拼接后整段显示；高亮由阅读时自行定位空位。
    val textBuilder = StringBuilder()
    parts.forEachIndexed { index, part ->
        textBuilder.append(part)
        if (index < parts.size - 1) {
            textBuilder.append("____")
        }
    }
    Text(
        text = textBuilder.toString(),
        style = MaterialTheme.typography.bodyMedium,
        lineHeight = 22.sp,
    )
}

@Composable
private fun ReadingQuestionCard(
    question: QuizReadingQuestion,
    selected: String?,
    onSelected: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "${question.index}. ${question.question}",
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            listOf("A", "B", "C", "D").forEach { key ->
                val optionText = question.options[key]
                if (!optionText.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected == key,
                                onClick = { onSelected(key) },
                                role = Role.RadioButton,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected == key,
                            onClick = null,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "$key. $optionText", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultContent(
    score: QuizScore,
    clozeResults: List<QuizClozeResult>,
    readingResults: List<QuizReadingResult>,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScoreCard(score = score)
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "选词填空批改",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        clozeResults.forEach { result ->
            item {
                ClozeReviewItem(result = result)
            }
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "阅读理解批改",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        readingResults.forEach { result ->
            item {
                ReadingReviewItem(result = result)
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("再来一次")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ScoreCard(score: QuizScore) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "总分：${score.totalCorrect} / ${score.totalQuestions}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "选词填空：${score.clozeCorrect} / ${score.clozeTotal}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "阅读理解：${score.readingCorrect} / ${score.readingTotal}",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun ClozeReviewItem(result: QuizClozeResult) {
    val color = if (result.isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
    val mark = if (result.isCorrect) "✓" else "✗"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "$mark 第 ${result.index} 空",
                color = color,
                fontWeight = FontWeight.Bold,
            )
            Text(text = "答案：${result.correctAnswer}", fontWeight = FontWeight.Medium)
            if (!result.isCorrect) {
                Text(text = "你的答案：${result.userAnswer}", color = Color.Gray)
            }
            if (!result.explanation.isNullOrBlank()) {
                Text(text = "解析：${result.explanation}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ReadingReviewItem(result: QuizReadingResult) {
    val color = if (result.isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
    val mark = if (result.isCorrect) "✓" else "✗"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "$mark 第 ${result.index} 题",
                color = color,
                fontWeight = FontWeight.Bold,
            )
            Text(text = "正确答案：${result.correctAnswer}", fontWeight = FontWeight.Medium)
            if (!result.isCorrect) {
                Text(text = "你的答案：${result.userAnswer}", color = Color.Gray)
            }
            if (!result.explanation.isNullOrBlank()) {
                Text(text = "解析：${result.explanation}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
