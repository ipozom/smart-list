package com.example.smartlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smartlist.ui.ItemsScreen
import com.example.smartlist.ui.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppNavHost()
        }
    }
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    // activity-scoped ViewModel to observe global UI events (confirmations, snackbars)
    val activity = LocalContext.current as androidx.activity.ComponentActivity
    val listVm: com.example.smartlist.ui.ListViewModel = androidx.lifecycle.viewmodel.compose.viewModel(activity)
    var pendingConfirm by remember { androidx.compose.runtime.mutableStateOf<com.example.smartlist.ui.UiEvent.ShowConfirm?>(null) }

    // collect confirmation requests centrally so dialogs overlay the whole NavHost
    androidx.compose.runtime.LaunchedEffect(listVm) {
        listVm.events.collect { ev ->
            if (ev is com.example.smartlist.ui.UiEvent.ShowConfirm) pendingConfirm = ev
        }
    }

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(navController)
        }

        composable(
            "items/{listId}",
            arguments = listOf(
                navArgument("listId") { type = NavType.LongType }
            )
        ) { backStack ->
            val listId = backStack.arguments?.getLong("listId") ?: 0L
            ItemsScreen(listId = listId, navController = navController)
        }
    }

    // centralized confirm dialog host (outside the NavHost so it overlays all screens)
    val confirm = pendingConfirm
    if (confirm != null) {
        androidx.compose.material.AlertDialog(onDismissRequest = { pendingConfirm = null }, title = { androidx.compose.material.Text(confirm.title) }, text = {
            androidx.compose.material.Text(confirm.message)
        }, confirmButton = {
            androidx.compose.material.TextButton(onClick = {
                when (confirm.kind) {
                    "archive" -> listVm.setState(confirm.id, com.example.smartlist.ui.ListStateManager.ARCHIVED)
                    "delete" -> listVm.deleteList(confirm.id)
                    else -> {
                        // extend with other kinds as needed
                    }
                }
                pendingConfirm = null
            }) { androidx.compose.material.Text(confirm.confirmLabel) }
        }, dismissButton = {
            androidx.compose.material.TextButton(onClick = { pendingConfirm = null }) { androidx.compose.material.Text(confirm.cancelLabel) }
        })
    }
}
