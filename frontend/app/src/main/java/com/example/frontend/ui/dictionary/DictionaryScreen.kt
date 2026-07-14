package com.example.frontend.ui.dictionary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.frontend.CodeMap
import com.example.frontend.data.remote.PhraseItem
import com.example.frontend.data.remote.SentenceItem
import com.example.frontend.data.remote.TranslationItem
import com.example.frontend.data.remote.WordDetail

/**
 * 查词版块（功能1）：输入单词 → 查询后端 → 展示释义/短语/例句/所属词库，
 * 支持英音/美音朗读、加入生词本。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(
    modifier: Modifier = Modifier,
    vm: DictionaryViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::onQueryChange,
                label = { Text("输入单词，例如 refuse") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { vm.search() }
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { vm.search() }, enabled = !state.loading) {
                Text("查询")
            }
        }

        Spacer(Modifier.height(12.dp))

        when {
            state.loading -> {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            state.detail != null -> {
                WordDetailContent(
                    detail = state.detail!!,
                    inWordbook = state.inWordbook,
                    onSpeakUk = vm::speakUk,
                    onSpeakUs = vm::speakUs,
                    onAddToWordbook = vm::addToWordbook,
                )
            }
        }
    }
}

@Composable
private fun WordDetailContent(
    detail: WordDetail,
    inWordbook: Boolean,
    onSpeakUk: () -> Unit,
    onSpeakUs: () -> Unit,
    onAddToWordbook: () -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                text = detail.word,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onSpeakUk) { Text("🇬🇧 英音") }
                OutlinedButton(onClick = onSpeakUs) { Text("🇺🇸 美音") }
                Button(onClick = onAddToWordbook, enabled = !inWordbook) {
                    Text(if (inWordbook) "已在生词本" else "加入生词本")
                }
            }
        }

        if (detail.categories.isNotEmpty()) {
            item { SectionTitle("所属词库") }
            item {
                Text(detail.categories.joinToString("、") { CodeMap.label(it) })
            }
        }

        if (detail.translations.isNotEmpty()) {
            item { SectionTitle("释义") }
            items(detail.translations) { t -> TranslationRow(t) }
        }

        if (detail.phrases.isNotEmpty()) {
            item { SectionTitle("短语") }
            items(detail.phrases) { p -> PhraseRow(p) }
        }

        if (detail.sentences.isNotEmpty()) {
            item { SectionTitle("例句") }
            items(detail.sentences) { s -> SentenceRow(s) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun TranslationRow(t: TranslationItem) {
    Row {
        if (!t.type.isNullOrBlank()) {
            Text(
                text = "${t.type}. ",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(text = t.translation, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PhraseRow(p: PhraseItem) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(p.phrase, style = MaterialTheme.typography.bodyLarge)
            Text(
                p.translation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SentenceRow(s: SentenceItem) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(s.sentence, style = MaterialTheme.typography.bodyLarge)
            Text(
                s.translation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
