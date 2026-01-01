package com.example.smartlist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun AddItemDialog(onAdd: (String) -> Unit, onCancel: () -> Unit, itemsVm: ItemsViewModel) {
    val text = remember { mutableStateOf("") }
    val suggestions by itemsVm.suggestions.collectAsState()

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
            Surface(elevation = 8.dp, shape = MaterialTheme.shapes.medium) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)) {
                    OutlinedTextField(
                        value = text.value,
                        onValueChange = {
                            text.value = it
                            itemsVm.setAddQuery(it)
                        },
                        label = { Text("Item name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (suggestions.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.padding(top = 8.dp).heightIn(max = 160.dp)) {
                            items(suggestions) { s ->
                                Row(modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        text.value = s
                                        itemsVm.setAddQuery(s)
                                    }
                                    .padding(8.dp)) {
                                    Text(text = s)
                                }
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
                        Button(onClick = onCancel, modifier = Modifier.padding(8.dp)) { Text("Cancel") }
                        Button(onClick = { onAdd(text.value) }, modifier = Modifier.padding(8.dp)) { Text("Add") }
                    }
                }
            }
        }
    }
}
