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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.size
import androidx.lifecycle.ViewModelProvider
import com.example.smartlist.data.ListNameEntity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
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
    // Use the activity as the ViewModelStoreOwner so ItemsScreen and MainScreen share
    // the same ListViewModel instance.
    val activity = LocalContext.current as androidx.activity.ComponentActivity
    val listVm: ListViewModel = viewModel(activity)

    val items by itemsVm.items.collectAsState()
    val query by itemsVm.query.collectAsState()

    // Observe the current list name from DB so UI updates after rename
    val listDao = AppDatabase.getInstance(context).listNameDao()
    val currentList by listDao.getById(listId).collectAsState(initial = ListNameEntity(id = listId, name = ""))
    val displayedName = currentList?.name ?: ""


    val showDialog = remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var showStateDialog by remember { mutableStateOf(false) }
    var selectedState by remember { mutableStateOf("PRECHECK") }
    // Legend is shown below the app bar; remove the info icon and keep legend visible
    val showLegend = true
    val masterDesc = stringResource(id = com.example.smartlist.R.string.legend_master_desc)
    // ...existing code...
    // Track whether this composable is currently in the RESUMED lifecycle state. If it's
    // not resumed (e.g. it's in the nav back stack but not visible), avoid showing
    // modal dialogs — this prevents dialogs from appearing over other screens.
    val lifecycleOwner = LocalLifecycleOwner.current
    var isActive by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            val nowActive = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            isActive = nowActive
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // per-item rename state
    var showItemRenameDialog by remember { mutableStateOf(false) }
    var editingItemId by remember { mutableStateOf<Long?>(null) }
    var editingItemText by remember { mutableStateOf("") }
    val scaffoldState = rememberScaffoldState()

    // confirmation snackbar removed; we rely on AlertDialog confirm button only

    // Lazy list state so we can programmatically scroll to the top when new items arrive
    val listState = rememberLazyListState()

    // Collect events from ItemsViewModel and ListViewModel
    LaunchedEffect(itemsVm, listVm) {
        launch {
            itemsVm.events.collect { ev ->
                when (ev) {
                    is UiEvent.ShowConfirm -> { /* not used from itemsVm */ }
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
                    // ignore ShowConfirm here; confirmations are handled centrally by MainScreen
                    is UiEvent.ShowConfirm -> { /* intentionally ignored here */ }
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
                            // If the snackbar's undoInfo signals an "open_list" action, navigate to it.
                            if (ev.undoInfo.kind == "open_list") {
                                try {
                                    navController.navigate("items/${ev.undoInfo.id}")
                                } catch (_: Exception) {
                                    // swallow navigation errors
                                }
                            } else {
                                listVm.handleUndo(ev.undoInfo)
                            }
                        }
                    }
                    is UiEvent.ScrollToTop -> { /* ignore: handled by itemsVm events */ }
                }
            }
        }
    }

    Scaffold(scaffoldState = scaffoldState, topBar = {
        TopAppBar(title = {
            // show list name and an info toggle to expand/collapse legend
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(displayedName)
            }
        }, actions = {
            // Show master/template and cloned-state pills in the TopAppBar actions so they align with the AppBar row
            if (currentList?.isTemplate == true) {
                val masterColor = Color(0xFF006A66)
                Surface(color = masterColor, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(end = 8.dp).semantics { contentDescription = masterDesc }) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(6.dp))
                        Text(text = "M", color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            if (currentList?.isCloned == true && currentList?.isTemplate != true) {
                val stateValue = currentList?.state ?: ListStateManager.PRECHECK
                val info = ListStateManager.getStateInfo(stateValue)
                val label = stringResource(id = info.labelRes)
                val firstLetter = label.firstOrNull()?.uppercaseChar()?.toString() ?: ""
                Surface(color = info.color, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(end = 8.dp).clickable {
                    selectedState = stateValue
                    showStateDialog = true
                }.semantics { contentDescription = label }) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                        Icon(imageVector = info.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(6.dp))
                        Text(text = firstLetter, color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            // Template toggle moved into the overflow menu (see menu items below)
            IconButton(onClick = { menuExpanded = true }) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                // Toggle template flag moved here so the setting is available in the 3-dots menu
                DropdownMenuItem(onClick = {
                    menuExpanded = false
                    val currentlyTemplate = currentList?.isTemplate == true
                    listVm.setTemplate(listId, !currentlyTemplate)
                }) {
                    Text(if (currentList?.isTemplate == true) "Unmark as template" else "Mark as template")
                }
                // Rename is disabled for template lists
                DropdownMenuItem(onClick = {
                    menuExpanded = false
                    renameText = displayedName
                    showRenameDialog = true
                }, enabled = !(currentList?.isTemplate == true)) {
                    Text("Rename")
                }
                // State menu only for cloned lists
                DropdownMenuItem(onClick = {
                    menuExpanded = false
                    // initialise dialog state to current value
                    selectedState = currentList?.state ?: "PRECHECK"
                    showStateDialog = true
                }, enabled = (currentList?.isCloned == true && currentList?.isTemplate != true)) {
                    Text(stringResource(id = com.example.smartlist.R.string.set_state))
                }
                DropdownMenuItem(onClick = {
                    menuExpanded = false
                    // request a confirmation before deleting the list
                    listVm.requestDeleteConfirmation(listId)
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
        // Only show add FAB when adding is allowed by list state (cloned lists: not allowed in CLOSED/ARCHIVED)
        val state = currentList?.state ?: "PRECHECK"
        val canAdd = !(currentList?.isCloned == true && (state == "CLOSED" || state == "ARCHIVED"))
        if (canAdd) {
            FloatingActionButton(onClick = { showDialog.value = true }) { Icon(imageVector = Icons.Default.Add, contentDescription = "Add item") }
        }
    }) { inner ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(inner)) {

                // Render the legend between the top bar and the content so it does not overlap
                // legend area left intentionally empty; pills are shown in the TopAppBar title row for cloned/master lists
                AnimatedVisibility(visible = showLegend, enter = fadeIn(), exit = fadeOut()) {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(6.dp))
                }

            val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
            OutlinedTextField(
                value = query,
                onValueChange = { itemsVm.setQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                label = { Text("Search items") },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            itemsVm.setQuery("")
                            keyboardController?.hide()
                        }) {
                            Icon(imageVector = androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                }
            )

                LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
                items(items, key = { it.id }) { item ->
                    val interactionSource = remember { MutableInteractionSource() }

                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItemPlacement()
                            .combinedClickable(
                                onClick = { /* single tap: no-op here */ },
                                onDoubleClick = {
                                    // only trigger toggle if marking is allowed by list state
                                    val state = currentList?.state ?: "PRECHECK"
                                    val markingAllowed = if (currentList?.isTemplate == true) false
                                    else if (currentList?.isCloned == true) state == "WORKING"
                                    else true

                                    if (markingAllowed) itemsVm.toggleStrike(item.id)
                                },
                                interactionSource = interactionSource,
                                indication = null
                            )
                            .padding(12.dp)
                    ) {
                        val markingAllowed = if (currentList?.isTemplate == true) false
                        else if (currentList?.isCloned == true) (currentList?.state == "WORKING")
                        else true

                        Checkbox(
                            checked = item.isStruck,
                            onCheckedChange = { _ -> if (markingAllowed) itemsVm.toggleStrike(item.id) },
                            modifier = Modifier.padding(end = 12.dp),
                            enabled = markingAllowed
                        )

                        Text(
                            text = item.content,
                            modifier = Modifier.weight(1f),
                            style = if (item.isStruck) MaterialTheme.typography.body1.copy(textDecoration = TextDecoration.LineThrough) else MaterialTheme.typography.body1
                        )

                        // inline trash button: allowed for non-cloned lists, or cloned lists in PRECHECK only
                        val canDelete = if (currentList?.isCloned == true) (currentList?.state == "PRECHECK") else true
                        IconButton(onClick = { if (canDelete) itemsVm.deleteItem(item.id) }, enabled = canDelete) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete item")
                        }

                        // rename icon placed next to trash
                        // rename is allowed when not archived/closed for cloned lists
                        val canRename = if (currentList?.isCloned == true) (currentList?.state == "PRECHECK" || currentList?.state == "WORKING") else true
                        IconButton(onClick = {
                            if (!canRename) return@IconButton
                            editingItemId = item.id
                            editingItemText = item.content
                            showItemRenameDialog = true
                        }, enabled = canRename) {
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

        if (showStateDialog) {
            AlertDialog(onDismissRequest = { showStateDialog = false }, title = { Text(stringResource(id = com.example.smartlist.R.string.set_state_title)) }, text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf(
                        "PRECHECK" to com.example.smartlist.R.string.state_precheck,
                        "WORKING" to com.example.smartlist.R.string.state_working,
                        "CLOSED" to com.example.smartlist.R.string.state_closed,
                        "ARCHIVED" to com.example.smartlist.R.string.state_archived
                    )
                    options.forEach { (value, resId) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            androidx.compose.material.RadioButton(selected = (selectedState == value), onClick = { selectedState = value })
                            Text(text = stringResource(id = resId), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }, confirmButton = {
                    TextButton(onClick = {
                    // If user chose ARCHIVED, request a confirmation via ViewModel (one-shot event)
                    if (selectedState == "ARCHIVED") {
                        listVm.requestArchiveConfirmation(listId)
                    } else {
                        listVm.setState(listId, selectedState)
                    }
                    showStateDialog = false
                }) { Text(stringResource(id = com.example.smartlist.R.string.save)) }
            }, dismissButton = {
                TextButton(onClick = { showStateDialog = false }) { Text(stringResource(id = com.example.smartlist.R.string.cancel)) }
            })
            // confirmation UI moved to MainScreen (central host)
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
