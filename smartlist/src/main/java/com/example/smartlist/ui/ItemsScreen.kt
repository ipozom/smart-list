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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel

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

    val showDialog = remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text(listName) }) }, floatingActionButton = {
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
    }
}
