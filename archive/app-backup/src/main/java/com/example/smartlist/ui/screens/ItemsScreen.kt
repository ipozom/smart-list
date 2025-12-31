package com.example.smartlist.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import com.example.smartlist.ui.viewmodel.ListsViewModel
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
// use Modifier.weight inside Row scope without explicit import
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.material3.Surface
import androidx.compose.material3.Card
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.layout.Box
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.example.smartlist.data.db.ItemEntity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartlist.ui.viewmodel.ItemsViewModel
import android.util.Log
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemsScreen(listId: String, title: String, onBack: () -> Unit) {
    val viewModel: ItemsViewModel = viewModel()
    val itemsState by viewModel.items(listId).collectAsState()

    // input is now a persistent field in the main window (top area)
    var input by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    // Search query for dynamic filtering
    var searchQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Selection state (moved up so app bar can use it)
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
    // Manage focus and keyboard manually when dialogs open/close.
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

        // Also clear focus/hide keyboard whenever an add-dialog is closed to avoid
        // leaving a TextField focused (which may show a blinking cursor).
        LaunchedEffect(showAddDialog) {
            Log.d("ItemsScreen", "LaunchedEffect(showAddDialog) -> showAddDialog=$showAddDialog")
            if (!showAddDialog) {
                Log.d("ItemsScreen", "Dialog closed: clearing focus and hiding keyboard")
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
            }
        }

        // Defensive one-time startup clear: some devices/IMEs briefly attach a
        // platform cursor during activity/window startup. Clear focus after a
        // short delay so any transient focus from the system is removed.
        LaunchedEffect(Unit) {
            // small delay to let the window and Compose settle (tunable)
            // Do a few repeated clears during the initial startup window so
            // transient IME attachments (that happen very early on some OEMs)
            // are removed. We keep this short to avoid interfering with
            // deliberate focus requests (dialogs request focus later).
            try {
                val attempts = 6
                val intervalMs = 75L
                repeat(attempts) {
                    delay(intervalMs)
                    Log.d("ItemsScreen", "Startup LaunchedEffect: clearing focus and hiding keyboard (attempt=${it + 1})")
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                }
            } catch (_: Exception) {
            }
        }
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
                            viewModel.deleteItems(idsToDelete)
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
                                    viewModel.restoreItems(itemsToRestore)
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
                title = { Text(text = title) }
            )

            // Search preview (non-focusable). Tapping it opens a modal dialog that
            // contains the real TextField. This prevents any platform caret from
            // appearing at app start because there is no TextField in the main UI.
            var showSearchDialog by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                    .padding(12.dp)
                    .clickable { showSearchDialog = true }
            ) {
                Text(
                    text = if (searchQuery.isBlank()) "Search items..." else searchQuery,
                    color = if (searchQuery.isBlank()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )
            }

            if (showSearchDialog) {
                // Dialog-local state for editing
                val dialogFocusRequester = remember { FocusRequester() }
                var dialogText by remember { mutableStateOf(searchQuery) }
                val keyboardController = LocalSoftwareKeyboardController.current
                // Capture context in composable scope so we don't call LocalContext
                // from inside a suspend/LaunchedEffect block (that's invalid).
                val ctx = LocalContext.current

                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showSearchDialog = false },
                    title = { Text("Search") },
                    text = {
                        androidx.compose.foundation.layout.Column {
                            TextField(
                                value = dialogText,
                                onValueChange = { dialogText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(dialogFocusRequester),
                                singleLine = true
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            searchQuery = dialogText
                            showSearchDialog = false
                            keyboardController?.hide()
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        Button(onClick = {
                            showSearchDialog = false
                            keyboardController?.hide()
                        }) { Text("Cancel") }
                    }
                )

                // Request focus when the dialog appears
                LaunchedEffect(Unit) {
                    try {
                        // Previously we cleared an Activity-level IME block here.
                        // In the minimal/clean app that block may not exist, so
                        // avoid referencing the Activity API directly to keep this
                        // composable decoupled from Activity internals.
                        dialogFocusRequester.requestFocus()
                        keyboardController?.show()
                    } catch (_: Exception) {
                    }
                }
            }
        }

            // Note: persistent add-field removed from main window. Use FAB (bottom-right) to open add dialog.

            // Snackbar host
            SnackbarHost(hostState = snackbarHostState)

            // Apply dynamic filtering based on searchQuery
            val filteredItems = if (searchQuery.isBlank()) itemsState else itemsState.filter { it.text.contains(searchQuery, ignoreCase = true) }

            LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
            items(filteredItems) { item: ItemEntity ->
                Log.d("ItemsScreen", "rendering item row id=${item.id} text='${item.text}'")
                ItemRow(item,
                    modifier = Modifier.testTag("item_${'$'}{item.id}"),
                    onDelete = {
                        // single-item delete (via VM)
                        val deletedItem = item
                        viewModel.deleteItem(deletedItem.id)
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Item deleted",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.restoreItem(deletedItem)
                            }
                        }
                    },
                    onEditStart = { viewModel.startEdit(item) },
                    selectable = selectionMode,
                    selected = item.id in selectedIds,
                    onSelect = {
                        selectedIds = if (item.id in selectedIds) selectedIds - item.id else selectedIds + item.id
                    }
                )
            }
            }
        }
        // Floating action button to open centered add dialog (bottom-right)
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.BottomEnd)
                .testTag("add_item_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add item")
        }

        // Centered add dialog
        if (showAddDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("New item") },
                text = {
                    androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { focusState ->
                                    Log.d("ItemsScreen", "addDialog_input focus=${focusState.isFocused}")
                                }
                                .testTag("add_dialog_input"),
                            singleLine = true
                        )
                        IconButton(onClick = {
                            if (input.isNotBlank()) {
                                val toAdd = input.trim()
                                Log.d("ItemsScreen", "add dialog save clicked with input=\"${'$'}input\" trimmed=\"${'$'}toAdd\"")
                                viewModel.addItem(listId, toAdd)
                                input = ""
                                showAddDialog = false
                            }
                        }, modifier = Modifier.testTag("add_dialog_save")) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Save item")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {}
            )
        }
    }

    // Edit dialog (moved to ItemsScreen and managed by ViewModel)
    val editingItem by viewModel.editingItem.collectAsState()
    val editingText by viewModel.editingText.collectAsState()

    if (editingItem != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.cancelEdit() },
            title = { Text("Edit item") },
            text = {
                TextField(value = editingText, onValueChange = { viewModel.updateEditingText(it) }, modifier = Modifier.fillMaxWidth().onFocusChanged { Log.d("ItemsScreen", "edit_dialog_input focus=${it.isFocused}") })
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
fun ItemRow(
    item: ItemEntity,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
    onEditStart: (() -> Unit)? = null,
    selectable: Boolean = false,
    selected: Boolean = false,
    onSelect: (() -> Unit)? = null
) {
    // Make rows non-focusable to avoid system focus traversal and stray carets
    Card(modifier = modifier.padding(vertical = 6.dp).focusable(false).onFocusChanged { Log.d("ItemsScreen", "card focus for ${item.id} = ${it.isFocused}") }) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp).focusable(false).onFocusChanged { Log.d("ItemsScreen", "row focus for ${item.id} = ${it.isFocused}") }, horizontalArrangement = Arrangement.SpaceBetween) {
            Row {
                if (selectable) {
                    // Keep checkbox interactive but avoid it grabbing focus for visual caret issues
                    Checkbox(checked = selected, onCheckedChange = { onSelect?.invoke() }, modifier = Modifier.testTag("checkbox_item_${'$'}{item.id}").focusable(false).onFocusChanged { Log.d("ItemsScreen", "checkbox focus for ${item.id} = ${it.isFocused}") })
                    Spacer(modifier = Modifier.width(8.dp))
                }
                // Text is not interactive; mark non-focusable explicitly to prevent focus cycling
                Text(text = item.text, modifier = Modifier.padding(start = 8.dp).focusable(false).onFocusChanged { Log.d("ItemsScreen", "text focus for ${item.id} = ${it.isFocused}") })
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row {
                IconButton(onClick = { onEditStart?.invoke() }, modifier = Modifier.testTag("edit_item_${'$'}{item.id}")) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit item",
                        modifier = Modifier.size(20.dp)
                    )
                }
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
}
