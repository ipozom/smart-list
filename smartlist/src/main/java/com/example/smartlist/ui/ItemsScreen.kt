package com.example.smartlist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import com.example.smartlist.data.ListNameEntity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.setValue
import com.example.smartlist.data.AppDatabase

@Composable
fun ItemsScreen(listId: Long, listName: String, navController: NavController) {
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

    val items by itemsVm.items.collectAsState()
    val query by itemsVm.query.collectAsState()

    // Observe the current list name from DB so UI updates after rename
    val listDao = AppDatabase.getInstance(context).listNameDao()
    val currentList by listDao.getById(listId).collectAsState(initial = ListNameEntity(id = listId, name = listName))
    val displayedName = currentList?.name ?: listName

    val showDialog = remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(listName) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(topBar = {
        TopAppBar(title = { Text(displayedName) }, actions = {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(onClick = {
                    menuExpanded = false
                    renameText = listName
                    showRenameDialog = true
                }) {
                    Text("Rename")
                }
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

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(items, key = { it.id }) { item ->
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* future item actions */ }
                        .padding(16.dp)) {
                        Text(text = item.content)
                    }
                }
            }
        }

        if (showDialog.value) {
            AddNameDialog(onAdd = {
                itemsVm.add(it)
                showDialog.value = false
            }, onCancel = { showDialog.value = false })
        }

        if (showRenameDialog) {
            AlertDialog(onDismissRequest = { showRenameDialog = false }, title = { Text("Rename list") }, text = {
                TextField(value = renameText, onValueChange = { renameText = it }, modifier = Modifier.fillMaxWidth())
            }, confirmButton = {
                TextButton(onClick = {
                    // perform update
                    coroutineScope.launch {
                        withContext(Dispatchers.IO) {
                            AppDatabase.getInstance(context).listNameDao().updateName(listId, renameText.trim())
                        }
                    }
                    showRenameDialog = false
                }) { Text("Save") }
            }, dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            })
        }
    }
}
