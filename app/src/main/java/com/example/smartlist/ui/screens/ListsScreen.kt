package com.example.smartlist.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
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
                ListRow(title = listEntity.title, onClick = { onListClick(listEntity.id, listEntity.title) }, modifier = Modifier.testTag("list_row_${'$'}{listEntity.title}"))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListRow(title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier
        .padding(vertical = 6.dp)
        .clickable { onClick() }) {
        Text(text = title, modifier = Modifier.padding(16.dp))
    }
}
