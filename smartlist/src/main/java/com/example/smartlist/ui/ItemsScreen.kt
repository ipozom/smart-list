package com.example.smartlist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
// animateItemPlacement is an experimental API provided by the foundation lazy package; resolved by the compiler if available
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.AlertDialog
import androidx.compose.material.TextButton
// ...existing code...
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.Checkbox
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material.TopAppBar
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import com.example.smartlist.data.ListNameEntity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel

// ...existing code...
import androidx.compose.runtime.setValue
import com.example.smartlist.data.AppDatabase

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemsScreen(listId: Long, navController: NavController) {
    val context = LocalContext.current.applicationContext as android.app.Application

    // Provide a factory that knows how to create ItemsViewModel(listId)
    class ItemsViewModelFactory(private val application: android.app.Application, private val listId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ItemsViewModel::class.java)) {
                return ItemsViewModel(application, listId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    val itemsVm: ItemsViewModel = viewModel(factory = ItemsViewModelFactory(context, listId))
    val listVm: ListViewModel = viewModel()

    val items by itemsVm.items.collectAsState()
    val query by itemsVm.query.collectAsState()

    // Observe the current list name from DB so UI updates after rename
    val listDao = AppDatabase.getInstance(context).listNameDao()
    val currentList by listDao.getById(listId).collectAsState(initial = ListNameEntity(id = listId, name = ""))
    val displayedName = currentList?.name ?: ""

    // Debug: show an ephemeral snackbar once when this list screen is opened so we can
    // verify whether the list is a template or a clone on-device. This is temporary.
    val debugInfoShown = remember { mutableStateOf(false) }

    val showDialog = remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    // per-item rename state
    var showItemRenameDialog by remember { mutableStateOf(false) }
    var editingItemId by remember { mutableStateOf<Long?>(null) }
    var editingItemText by remember { mutableStateOf("") }
    val scaffoldState = rememberScaffoldState()

    // Show the debug snackbar after scaffoldState is available. Use a local copy of currentList
    // to avoid smart-cast issues on properties with custom getters.
    LaunchedEffect(currentList) {
        val list = currentList
        if (!debugInfoShown.value && list != null) {
            val info = "isTemplate=${list.isTemplate} isCloned=${list.isCloned}"
            scaffoldState.snackbarHostState.showSnackbar(info)
            debugInfoShown.value = true
        }
    }

    // Lazy list state so we can programmatically scroll to the top when new items arrive
    val listState = rememberLazyListState()

    // Collect events from ItemsViewModel and ListViewModel
    LaunchedEffect(itemsVm, listVm) {
        launch {
            itemsVm.events.collect { ev ->
                when (ev) {
                    is UiEvent.ShowSnackbar -> {
                        val result = scaffoldState.snackbarHostState.showSnackbar(ev.message, ev.actionLabel ?: "")
                        if (result == androidx.compose.material.SnackbarResult.ActionPerformed && ev.undoInfo != null) {
                            itemsVm.handleUndo(ev.undoInfo)
                        }
                    }
                    is UiEvent.ScrollToTop -> listState.animateScrollToItem(0)
                }
            }
        }
        launch {
            listVm.events.collect { ev ->
                when (ev) {
                    is UiEvent.ShowSnackbar -> {
                        // If the list was deleted, show the snackbar here first so the user sees it
                        if (ev.undoInfo?.kind == "list_delete") {
                            val result = scaffoldState.snackbarHostState.showSnackbar(ev.message, ev.actionLabel ?: "")
                            if (result == androidx.compose.material.SnackbarResult.ActionPerformed && ev.undoInfo != null) {
                                listVm.handleUndo(ev.undoInfo)
                                // stay on this screen after undo
                            } else {
                                // proceed to navigate back after showing snackbar
                                navController.popBackStack()
                            }
                            return@collect
                        }

                        val result = scaffoldState.snackbarHostState.showSnackbar(ev.message, ev.actionLabel ?: "")
                        if (result == androidx.compose.material.SnackbarResult.ActionPerformed && ev.undoInfo != null) {
                            listVm.handleUndo(ev.undoInfo)
                        }
                    }
                    is UiEvent.ScrollToTop -> { /* ignore: handled by itemsVm events */ }
                }
            }
        }
    }

    Scaffold(scaffoldState = scaffoldState, topBar = {
        TopAppBar(title = {
            // show list name and template toggle
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(displayedName)
            }
        }, actions = {
            // template toggle shown in actions area
            val isTemplate = currentList?.isTemplate == true
            androidx.compose.material.Switch(checked = isTemplate, onCheckedChange = { checked ->
                // toggle template flag via ViewModel
                listVm.setTemplate(listId, checked)
            })
            IconButton(onClick = { menuExpanded = true }) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                // Rename is disabled for template lists
                DropdownMenuItem(onClick = {
                    menuExpanded = false
                    renameText = displayedName
                    showRenameDialog = true
                }, enabled = !(currentList?.isTemplate == true)) {
                    Text("Rename")
                }
                DropdownMenuItem(onClick = {
                    menuExpanded = false
                    // delete the list (ViewModel will emit snackbar with undo)
                    listVm.deleteList(listId)
                }, enabled = !(currentList?.isTemplate == true)) {
                    Text("Delete")
                }
                // Clone (only available for template lists)
                DropdownMenuItem(onClick = {
                    menuExpanded = false
                    listVm.cloneList(listId)
                }, enabled = (currentList?.isTemplate == true)) { Text("Clone list") }
            }
        })
    }, floatingActionButton = {
        FloatingActionButton(onClick = { showDialog.value = true }) { Icon(imageVector = Icons.Default.Add, contentDescription = "Add item") }
    }) { inner ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(inner)) {

            OutlinedTextField(value = query, onValueChange = { itemsVm.setQuery(it) }, modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp), label = { Text("Search items") })

            LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
                items(items, key = { it.id }) { item ->
                    val isCloned = currentList?.isCloned == true

                    val interactionSource = remember { MutableInteractionSource() }

                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItemPlacement()
                            .combinedClickable(
                                onClick = { /* single tap: no-op here */ },
                                onDoubleClick = {
                                    // delegate to ViewModel which will allow toggling on clones and non-templates
                                    itemsVm.toggleStrike(item.id)
                                },
                                interactionSource = interactionSource,
                                indication = null
                            )
                            .padding(12.dp)
                    ) {
                        Checkbox(
                            checked = item.isStruck,
                            onCheckedChange = { _ -> itemsVm.toggleStrike(item.id) },
                            modifier = Modifier.padding(end = 12.dp),
                            enabled = !(currentList?.isTemplate == true)
                        )

                        Text(
                            text = item.content,
                            modifier = Modifier.weight(1f),
                            style = if (item.isStruck) MaterialTheme.typography.body1.copy(textDecoration = TextDecoration.LineThrough) else MaterialTheme.typography.body1
                        )

                        // inline trash button for cloned lists (accessibility + discoverability)
                        if (isCloned) {
                            IconButton(onClick = { itemsVm.deleteItem(item.id) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete item")
                            }
                        }

                        // rename icon placed next to trash
                        IconButton(onClick = {
                            editingItemId = item.id
                            editingItemText = item.content
                            showItemRenameDialog = true
                        }) {
                            Icon(imageVector = androidx.compose.material.icons.Icons.Default.Edit, contentDescription = "Rename item")
                        }
                    }
                }
            }
        }

        if (showDialog.value) {
            AddItemDialog(onAdd = {
                // delegate add and snackbar to ViewModel events to avoid duplicate snackbars
                itemsVm.add(it)
                itemsVm.setAddQuery("")
                showDialog.value = false
            }, onCancel = {
                itemsVm.setAddQuery("")
                showDialog.value = false
            }, itemsVm = itemsVm)
        }

        if (showRenameDialog) {
            AlertDialog(onDismissRequest = { showRenameDialog = false }, title = { Text("Rename list") }, text = {
                TextField(value = renameText, onValueChange = { renameText = it }, modifier = Modifier.fillMaxWidth())
            }, confirmButton = {
                TextButton(onClick = {
                    val newName = renameText.trim()
                    listVm.renameList(listId, newName)
                    showRenameDialog = false
                }) { Text("Save") }
            }, dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            })
        }

        if (showItemRenameDialog && editingItemId != null) {
            AlertDialog(onDismissRequest = { showItemRenameDialog = false }, title = { Text("Rename item") }, text = {
                TextField(value = editingItemText, onValueChange = { editingItemText = it }, modifier = Modifier.fillMaxWidth())
            }, confirmButton = {
                TextButton(onClick = {
                    val newContent = editingItemText.trim()
                    if (editingItemId == null) return@TextButton

                    // delegate validation, update and snackbar emission to the ViewModel
                    itemsVm.renameItem(editingItemId!!, newContent)
                    showItemRenameDialog = false
                }) { Text("Save") }
            }, dismissButton = {
                TextButton(onClick = { showItemRenameDialog = false }) { Text("Cancel") }
            })
        }
    }
}
