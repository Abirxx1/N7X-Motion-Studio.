package com.n7x.motionstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.n7x.motionstudio.ui.navigation.N7XNavGraph
import com.n7x.motionstudio.ui.theme.N7XTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val dao = (application as N7XApp).database.projectDao()
        setContent {
            N7XTheme {
                N7XNavGraph(dao)
            }
        }
    }
}
