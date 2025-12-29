package com.example.smartlist.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.smartlist.ServiceLocator
import com.example.smartlist.data.db.ItemEntity
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.testTag
import android.util.Log

@Composable
fun ItemsScreen(listId: String, title: String, onBack: () -> Unit) {
    val repo = ServiceLocator.provideRepository()
    // Observe items
    val itemsFlow = repo.observeItems(listId)
    val itemsState by itemsFlow.collectAsState(initial = emptyList())

    var input by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Selection state (moved up so app bar can use it)
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Top app bar or contextual action bar when selectionMode is active
        if (selectionMode) {
            TopAppBar(
                title = { Text(text = "${selectedIds.size} selected") },
                navigationIcon = {
                    IconButton(onClick = {
                        // Cancel selection mode
                        selectionMode = false
                        selectedIds = setOf()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val idsToDelete = selectedIds.toList()
                        if (idsToDelete.isNotEmpty()) {
                            val itemsToRestore = itemsState.filter { it.id in selectedIds }
                            coroutineScope.launch(Dispatchers.IO) {
                                repo.deleteItemsSoft(idsToDelete)
                            }
                            // clear selection and exit selection mode
                            selectedIds = setOf()
                            selectionMode = false

                            // Show undo snackbar
                            coroutineScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Items deleted",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        repo.restoreItems(itemsToRestore)
                                    }
                                }
                            }
                        }
                    }, modifier = Modifier.testTag("delete_selected_button")) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                    }
                }
            )
        } else {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("<- Back")
                    }
                },
                actions = {
                    IconButton(onClick = { selectionMode = true }, modifier = Modifier.testTag("select_mode_button")) {
                        Icon(Icons.Default.Edit, contentDescription = "Enter selection mode")
                    }
                }
            )
        }

        // Add item
        TextField(value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("item_input"))
        Button(onClick = {
            if (input.isNotBlank()) {
                // Capture and trim the input before launching the coroutine so the value
                // isn't lost when we clear the UI state immediately after launching.
                val toAdd = input.trim()
                // Log the raw and trimmed values for debugging
                Log.d("ItemsScreen", "add button clicked with input=\"$input\" trimmed=\"$toAdd\"")
                coroutineScope.launch(Dispatchers.IO) {
                    repo.addItem(listId, toAdd)
                }
                input = ""
            }
        }, modifier = Modifier.padding(top = 8.dp).testTag("add_item_button")) {
            Text("Add item")
        }

        // Snackbar host
        SnackbarHost(hostState = snackbarHostState)

        LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
            items(itemsState) { item: ItemEntity ->
                ItemRow(item,
                    modifier = Modifier.testTag("item_${'$'}{item.id}"),
                    onDelete = {
                        // single-item delete (same as before)
                        val deletedItem = item
                        coroutineScope.launch(Dispatchers.IO) {
                            repo.deleteItemSoft(deletedItem.id)
                        }
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Item deleted",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                coroutineScope.launch(Dispatchers.IO) {
                                    repo.restoreItem(deletedItem)
                                }
                            }
                        }
                    },
                    selectable = selectionMode,
                    selected = item.id in selectedIds,
                    onSelect = {
                        selectedIds = if (item.id in selectedIds) selectedIds - item.id else selectedIds + item.id
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemRow(
    item: ItemEntity,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
    selectable: Boolean = false,
    selected: Boolean = false,
    onSelect: (() -> Unit)? = null
) {
    Card(modifier = modifier.padding(vertical = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Row {
                if (selectable) {
                    Checkbox(checked = selected, onCheckedChange = { onSelect?.invoke() }, modifier = Modifier.testTag("checkbox_item_${'$'}{item.id}"))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text = item.text, modifier = Modifier.padding(start = 8.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { onDelete?.invoke() }, modifier = Modifier.testTag("delete_item_${'$'}{item.id}")) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete item",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
