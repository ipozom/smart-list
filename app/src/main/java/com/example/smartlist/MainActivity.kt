package com.example.smartlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.smartlist.ui.screens.ItemsScreen
import com.example.smartlist.ui.screens.ListsScreen
import com.example.smartlist.ui.theme.SmartListTheme

class MainActivity : ComponentActivity() {

    // ViewModel has a no-arg constructor (fetches repository from ServiceLocator internally)
    private val viewModel: com.example.smartlist.ui.viewmodel.ListsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartListTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppContent()
                }
            }
        }
    }
}

sealed class Screen {
    object Lists : Screen()
    data class Items(val listId: String, val title: String) : Screen()
}

@Composable
fun AppContent() {
    var screen by remember { mutableStateOf<Screen>(Screen.Lists) }

    when (val s = screen) {
        is Screen.Lists -> ListsScreen(onListClick = { id, title ->
            screen = Screen.Items(id, title)
        })
        is Screen.Items -> ItemsScreen(listId = s.listId, title = s.title, onBack = {
            screen = Screen.Lists
        })
    }
}
