package com.example.smartlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
}
