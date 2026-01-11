package com.example.smartlist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.FabPosition
import androidx.compose.material.Scaffold
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
// keep icon set minimal; use text-based pills to avoid missing icon symbols across platforms
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material.IconButton
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.AlertDialog
import androidx.compose.material.TextButton
import androidx.compose.runtime.remember
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Switch
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.IconButton
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
// stringResource already imported above
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import android.net.Uri
import androidx.core.net.UriCompat
import androidx.core.net.toUri


@Composable
fun MainScreen(navController: NavController) {
    // Use the activity as the ViewModelStoreOwner so MainScreen and ItemsScreen
    // share the same ListViewModel instance (Nav back stack entries create separate instances).
    val activity = LocalContext.current as androidx.activity.ComponentActivity
    val viewModel: ListViewModel = viewModel(activity)
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
    // confirmations are handled at the AppNavHost level so MainScreen doesn't
    // own a dialog host (NavHost shows one screen at a time)
        // Collect StateFlow values as Compose State so UI updates when ViewModel emits
        val query by viewModel.query.collectAsState()
        val items by viewModel.items.collectAsState()

    // Keep dialog state at top of screen so FAB can toggle it
    val showDialog = remember { mutableStateOf(false) }
    val showLegend = remember { mutableStateOf(false) }

        Scaffold(
            scaffoldState = scaffoldState,
            floatingActionButton = {
                FloatingActionButton(onClick = { showDialog.value = true }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                }
            },
            floatingActionButtonPosition = FabPosition.End,
        ) { innerPadding ->
            val listState = rememberLazyListState()

            Column(modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)) {

                // Top search field and legend info
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // Search bar takes remaining space; add small padding so it expands correctly
                    Box(modifier = Modifier.weight(1f).padding(start = 8.dp, end = 8.dp)) {
                        SearchBar(
                            value = query,
                            onValueChange = { viewModel.setQuery(it) },
                            onClear = { viewModel.setQuery("") }
                        )
                    }

                    // Overflow menu: show archived toggle + legend
                    val showArchived by viewModel.showArchived.collectAsState()
                    var menuExpanded by remember { mutableStateOf(false) }

                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.padding(end = 8.dp)) {
                        Icon(imageVector = Icons.Filled.MoreVert, contentDescription = stringResource(id = com.example.smartlist.R.string.more))
                    }

                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(onClick = { /* handled by Switch below */ }) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(text = stringResource(id = com.example.smartlist.R.string.show_archived))
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                                androidx.compose.material.Switch(checked = showArchived, onCheckedChange = {
                                    viewModel.setShowArchived(it)
                                })
                            }
                        }
                        DropdownMenuItem(onClick = { menuExpanded = false; showLegend.value = true }) {
                            Text(text = stringResource(id = com.example.smartlist.R.string.legend_title))
                        }
                        // removed debug menu item that dumped lists to logcat
                    }
                }

                // List
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(items, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Navigate to items screen for this list
                                    // Navigate passing ONLY the id; ItemsScreen will read the name from the DB
                                    navController.navigate("items/${item.id}")
                                }
                                .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {

                            // Left-side pill: MASTER or state for cloned lists
                            if (item.isTemplate) {
                                Surface(color = Color(0xFF006A66), shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(end = 8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Icon(imageVector = Icons.Filled.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.size(6.dp))
                                        Text(text = "MASTER", color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            } else if (item.isCloned) {
                                val stateInfo = ListStateManager.getStateInfo(item.state)
                                Surface(color = stateInfo.color, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(end = 8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Icon(imageVector = stateInfo.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.size(6.dp))
                                        Text(text = stringResource(id = stateInfo.labelRes), color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }

                            Text(text = item.name, modifier = Modifier.weight(1f))

                            // Right-side pill showing number of items in the list
                            // Right-side pill: show only total for master/template lists; show total/marked for others
                            if (item.isTemplate) {
                                Surface(
                                    color = Color(0xFFE0E0E0),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .semantics {
                                            contentDescription = "${item.itemCount} items"
                                        }
                                ) {
                                    Text(
                                        text = "${item.itemCount}",
                                        color = Color.Black,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    color = Color(0xFFE0E0E0),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .semantics {
                                            contentDescription = "${item.itemCount} total, ${item.markedCount} marked"
                                        }
                                ) {
                                    Text(
                                        text = "${item.itemCount}/${item.markedCount}",
                                        color = Color.Black,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // If this is an archived cloned list and the user opted to show archived,
                            // offer a quick unarchive button for discoverability.
                            if (item.isCloned && item.state == ListStateManager.ARCHIVED) {
                                IconButton(onClick = { viewModel.unarchiveList(item.id) }, modifier = Modifier.padding(start = 8.dp)) {
                                    Icon(imageVector = Icons.Filled.Unarchive, contentDescription = stringResource(id = com.example.smartlist.R.string.unarchive))
                                }
                            }
                        }
                    }
                }
            }

                // collect UI events from ViewModel and show snackbars
                LaunchedEffect(viewModel) {
                    viewModel.events.collect { ev ->
                        when (ev) {
                            is UiEvent.ShowConfirm -> { /* handled by AppNavHost */ }
                            is UiEvent.ShowSnackbar -> {
                                // If the event carries a special undoInfo.kind == "show_archived"
                                // and the user taps the action, toggle showArchived so the
                                // archived row becomes visible immediately.
                                val result = scaffoldState.snackbarHostState.showSnackbar(ev.message, ev.actionLabel ?: "")
                                if (result == androidx.compose.material.SnackbarResult.ActionPerformed && ev.undoInfo != null) {
                                    when (ev.undoInfo.kind) {
                                        "open_list" -> {
                                            try {
                                                navController.navigate("items/${ev.undoInfo.id}")
                                            } catch (_: Exception) {
                                                // swallow navigation errors
                                            }
                                        }
                                        "show_archived" -> {
                                            // enable showing archived lists and scroll to the
                                            // archived item's position once the list updates.
                                            viewModel.setShowArchived(true)
                                            try {
                                                // give the flow a moment to emit the updated list
                                                kotlinx.coroutines.delay(200)
                                                val targetId = ev.undoInfo.id
                                                val idx = items.indexOfFirst { it.id == targetId }
                                                if (idx >= 0) {
                                                    listState.animateScrollToItem(idx)
                                                } else {
                                                    // fallback to top if we can't find it
                                                    listState.animateScrollToItem(0)
                                                }
                                            } catch (_: Exception) { }
                                        }
                                        else -> {
                                            viewModel.handleUndo(ev.undoInfo)
                                        }
                                    }
                                }
                            }
                            is UiEvent.ScrollToTop -> listState.animateScrollToItem(0)
                        }
                    }
                }

                if (showDialog.value) {
                    AddNameDialog(onAdd = {
                        viewModel.add(it)
                        showDialog.value = false
                    }, onCancel = { showDialog.value = false })
                }
                    if (showLegend.value) {
                        AlertDialog(onDismissRequest = { showLegend.value = false }, title = { Text(stringResource(id = com.example.smartlist.R.string.legend_title)) }, text = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Master
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                                    Surface(color = Color(0xFF006A66), shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(end = 8.dp)) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.padding(8.dp).size(18.dp)
                                        )
                                    }
                                    Column { Text(text = "MASTER", fontSize = 14.sp); Text(text = stringResource(id = com.example.smartlist.R.string.legend_master_desc), fontSize = 12.sp) }
                                }
                                // Precheck
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                                    Surface(color = Color(0xFF90A4AE), shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(end = 8.dp)) {
                                        Icon(
                                            imageVector = Icons.Filled.HourglassEmpty,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.padding(8.dp).size(18.dp)
                                        )
                                    }
                                    Column { Text(text = stringResource(id = com.example.smartlist.R.string.state_precheck), fontSize = 14.sp); Text(text = stringResource(id = com.example.smartlist.R.string.legend_precheck_desc), fontSize = 12.sp) }
                                }
                                // Working
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                                    Surface(color = Color(0xFF4CAF50), shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(end = 8.dp)) {
                                        Icon(
                                            imageVector = Icons.Filled.Work,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.padding(8.dp).size(18.dp)
                                        )
                                    }
                                    Column { Text(text = stringResource(id = com.example.smartlist.R.string.state_working), fontSize = 14.sp); Text(text = stringResource(id = com.example.smartlist.R.string.legend_working_desc), fontSize = 12.sp) }
                                }
                                // Closed
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                                    Surface(color = Color(0xFFFFA726), shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(end = 8.dp)) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.padding(8.dp).size(18.dp)
                                        )
                                    }
                                    Column { Text(text = stringResource(id = com.example.smartlist.R.string.state_closed), fontSize = 14.sp); Text(text = stringResource(id = com.example.smartlist.R.string.legend_closed_desc), fontSize = 12.sp) }
                                }
                                // Archived
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                                    Surface(color = Color(0xFF616161), shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(end = 8.dp)) {
                                        Icon(
                                            imageVector = Icons.Filled.Archive,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.padding(8.dp).size(18.dp)
                                        )
                                    }
                                    Column { Text(text = stringResource(id = com.example.smartlist.R.string.state_archived), fontSize = 14.sp); Text(text = stringResource(id = com.example.smartlist.R.string.legend_archived_desc), fontSize = 12.sp) }
                                }
                            }
                        }, confirmButton = {
                            TextButton(onClick = { showLegend.value = false }) { Text(stringResource(id = com.example.smartlist.R.string.cancel)) }
                        }, dismissButton = {})
                    }

                // confirmations are presented centrally by the AppNavHost
        }
    }
}
                

@Composable
private fun SearchBar(value: String, onValueChange: (String) -> Unit, onClear: () -> Unit) {
    val keyboardController = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth(),
        label = { Text(stringResource(id = com.example.smartlist.R.string.search_lists)) },
        singleLine = true,
        maxLines = 1,
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = {
                    onClear()
                    keyboardController?.hide()
                }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                }
            }
        }
    )
}

@Composable
fun AddNameDialog(onAdd: (String) -> Unit, onCancel: () -> Unit, labelText: String = "List name") {
    val text = remember { mutableStateOf("") }

    // Use a Dialog to center the content vertically in the window
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        // Outer Box to control dialog width and padding
        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
            Surface(elevation = 8.dp, shape = MaterialTheme.shapes.medium) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)) {
                    OutlinedTextField(
                        value = text.value,
                        onValueChange = { text.value = it },
                        label = { Text(labelText) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
                        Button(onClick = onCancel, modifier = Modifier.padding(8.dp)) { Text("Cancel") }
                        Button(onClick = { onAdd(text.value) }, modifier = Modifier.padding(8.dp)) { Text("Add") }
                    }
                }
            }
        }
    }
}
