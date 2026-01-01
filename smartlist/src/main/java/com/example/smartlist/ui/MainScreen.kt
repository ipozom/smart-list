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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import android.net.Uri
import androidx.core.net.UriCompat
import androidx.core.net.toUri


@Composable
fun MainScreen(navController: NavController) {
    val viewModel: ListViewModel = viewModel()
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
        // Collect StateFlow values as Compose State so UI updates when ViewModel emits
        val query by viewModel.query.collectAsState()
        val items by viewModel.items.collectAsState()

        // Keep dialog state at top of screen so FAB can toggle it
        val showDialog = remember { mutableStateOf(false) }

        Scaffold(
            scaffoldState = scaffoldState,
            floatingActionButton = {
                FloatingActionButton(onClick = { showDialog.value = true }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                }
            },
            floatingActionButtonPosition = FabPosition.End,
        ) { innerPadding ->
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)) {

                // Top search field
                SearchBar(
                    value = query,
                    onValueChange = { viewModel.setQuery(it) }
                )

                // List
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items, key = { it.id }) { item ->
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Navigate to items screen for this list
                                // Navigate passing ONLY the id; ItemsScreen will read the name from the DB
                                navController.navigate("items/${item.id}")
                            }
                            .padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = item.name)
                        }
                    }
                }
            }

            if (showDialog.value) {
                AddNameDialog(onAdd = {
                    viewModel.add(it)
                    showDialog.value = false
                    coroutineScope.launch {
                        scaffoldState.snackbarHostState.showSnackbar("List added")
                    }
                }, onCancel = { showDialog.value = false })
            }
        }
    }
}

@Composable
private fun SearchBar(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        label = { Text("Search lists") }
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
