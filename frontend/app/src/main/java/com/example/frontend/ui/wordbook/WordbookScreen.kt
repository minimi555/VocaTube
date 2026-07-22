package com.example.frontend.ui.wordbook

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * 生词本版块：手动加词、看列表、删词。纯本地存储，不连后端。
 */
@Composable
fun WordbookScreen(
    modifier: Modifier = Modifier,
    vm: WordbookViewModel = viewModel()
) {
    val words by vm.words.collectAsStateWithLifecycle()
    val currentBook by vm.currentBook.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    var showBookPicker by remember { mutableStateOf(false) }
    val books = remember { listOf("CET4", "CET6", "TOFEL", "IELTS", "SAT", "kaoyan") }

    fun submit() {
        val w = input.trim()
        if (w.isNotEmpty()) {
            vm.add(w)
            input = ""
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("生词本", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = { showBookPicker = true }) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "选择单词书")
            }
        }

        if (currentBook != null) {
            Text(
                text = "当前单词书：$currentBook",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("手动添加单词") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { submit() }),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done)
            )
            OutlinedButton(onClick = { submit() }) { Text("添加") }
        }

        if (words.isEmpty()) {
            Text(
                "还没有生词，在上方输入框添加。",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(words, key = { it }) { word ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(word, style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { vm.remove(word) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "删除")
                            }
                        }
                    }
                }
            }
        }

        if (showBookPicker) {
            AlertDialog(
                onDismissRequest = { showBookPicker = false },
                title = { Text("选择当前学习的单词书") },
                text = {
                    Column {
                        books.forEach { book ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        vm.selectBook(book)
                                        showBookPicker = false
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentBook == book,
                                    onClick = {
                                        vm.selectBook(book)
                                        showBookPicker = false
                                    }
                                )
                                Text(
                                    text = book,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBookPicker = false }) {
                        Text("关闭")
                    }
                }
            )
        }
    }
}
