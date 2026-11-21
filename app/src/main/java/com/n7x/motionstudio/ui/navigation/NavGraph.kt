package com.n7x.motionstudio.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.n7x.motionstudio.N7XApp
import com.n7x.motionstudio.ui.editor.EditorScreen
import com.n7x.motionstudio.ui.editor.EditorViewModel
import com.n7x.motionstudio.ui.editor.EditorViewModelFactory
import com.n7x.motionstudio.ui.home.HomeScreen
import com.n7x.motionstudio.ui.home.HomeViewModel
import com.n7x.motionstudio.ui.home.HomeViewModelFactory

@Composable
fun N7XNavGraph(app: N7XApp) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            val vm: HomeViewModel = viewModel(
                factory = HomeViewModelFactory(app.database.projectDao(), app.filesDir)
            )
            HomeScreen(vm, onOpenProject = { id -> nav.navigate("editor/$id") })
        }
        composable(
            route = "editor/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            val vm: EditorViewModel = viewModel(factory = EditorViewModelFactory(app, id))
            EditorScreen(vm, onBack = { nav.popBackStack() })
        }
    }
}
