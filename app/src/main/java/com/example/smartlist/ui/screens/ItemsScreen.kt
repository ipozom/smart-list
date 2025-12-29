package com.example.smartlist.ui.screens

import androidx.compose.foundation.layout.Column
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header with back
        Column(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onBack) {
                Text("<- Back")
            }
            Text(text = title, modifier = Modifier.padding(top = 8.dp))
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

        LazyColumn(modifier = Modifier.padding(top = 12.dp)) {
            items(itemsState) { item: ItemEntity ->
                ItemRow(item, modifier = Modifier.testTag("item_${'$'}{item.id}"))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemRow(item: ItemEntity, modifier: Modifier = Modifier) {
    Card(modifier = modifier.padding(vertical = 6.dp)) {
        Text(text = item.text, modifier = Modifier.padding(16.dp))
    }
}
