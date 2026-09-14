package com.example.fitapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitapp.data.BodyMapping
import com.example.fitapp.data.ExerciseCatalog
import com.example.fitapp.ui.FitAppMain
import com.example.fitapp.ui.theme.FitAppTheme
import com.example.fitapp.viewmodel.FitAppViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FitAppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ExerciseCatalog.initialize(applicationContext)
        BodyMapping.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            val useMaterialYou by viewModel.useMaterialYou.collectAsStateWithLifecycle()
            val accentColorKey by viewModel.accentColorKey.collectAsStateWithLifecycle()

            FitAppTheme(
                darkTheme = isDarkTheme,
                useMaterialYou = useMaterialYou,
                accentKey = accentColorKey
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FitAppMain(viewModel = viewModel)
                }
            }
        }
    }
}
