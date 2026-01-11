package com.example.smartlist.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.testTag
import com.example.smartlist.ui.viewmodel.ListsViewModel
import kotlinx.coroutines.launch
import android.util.Log

@Composable
fun ListsScreen(
    viewModel: ListsViewModel = viewModel(),
    onListClick: (listId: String, title: String) -> Unit = { _, _ -> }
) {
    // Observe lists from ViewModel/Repository instead of static sample data
    val listsState by viewModel.lists.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = {
            // create a simple timestamped list name for quick testing
            scope.launch {
                viewModel.createList("New list")
            }
        }, modifier = Modifier.testTag("new_list_button")) {
            Text("+ New List")
        }

        LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
            items(listsState) { listEntity ->
                // Debug log to trace lists emissions during tests
                Log.d("ListsScreen", "rendering list row id=${listEntity.id} title=${listEntity.title}")
                // tag each row with its title for testing (titles are short/plain)
                ListRow(
                    title = listEntity.title,
                    onClick = { onListClick(listEntity.id, listEntity.title) },
                    onEditStart = { viewModel.startEdit(listEntity) },
                    modifier = Modifier.testTag("list_row_${'$'}{listEntity.title}")
                )
            }
        }
    }

    // Edit dialog for renaming a list (managed by ListsViewModel)
    val editingList by viewModel.editingList.collectAsState()
    val editingTitle by viewModel.editingTitle.collectAsState()

    if (editingList != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelEdit() },
            title = { Text("Rename list") },
            text = {
                // Keep the rename input compact: full width, single line
                TextField(
                    value = editingTitle,
                    onValueChange = { viewModel.updateEditingTitle(it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    maxLines = 1
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.saveEdit() }) { Text("Save") }
            },
            dismissButton = {
                Button(onClick = { viewModel.cancelEdit() }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListRow(title: String, onClick: () -> Unit, onEditStart: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Card(modifier = modifier
        .padding(vertical = 6.dp)) {
        Row(modifier = Modifier
            .clickable { onClick() }
            .padding(12.dp)) {
            Text(text = title, modifier = Modifier.padding(end = 8.dp))
            IconButton(onClick = { onEditStart?.invoke() }, modifier = Modifier.testTag("edit_list_${'$'}{title}")) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit list")
            }
        }
    }
}
