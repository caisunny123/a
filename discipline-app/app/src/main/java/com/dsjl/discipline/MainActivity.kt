package com.dsjl.discipline

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dsjl.discipline.ui.SettingsScreen
import com.dsjl.discipline.ui.StatsScreen
import com.dsjl.discipline.ui.TodayScreen
import com.dsjl.discipline.ui.theme.DisciplineTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        setContent {
            DisciplineTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    MainTabs()
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }
    }
}

@Composable
private fun MainTabs() {
    val vm: MainViewModel = viewModel()
    var selected by remember { mutableStateOf("today") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selected == "today",
                    onClick = { selected = "today" },
                    icon = { Icon(Icons.Outlined.Today, contentDescription = null) },
                    label = { Text("今日") }
                )
                NavigationBarItem(
                    selected = selected == "stats",
                    onClick = { selected = "stats" },
                    icon = { Icon(Icons.Outlined.BarChart, contentDescription = null) },
                    label = { Text("统计") }
                )
                NavigationBarItem(
                    selected = selected == "settings",
                    onClick = { selected = "settings" },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    label = { Text("设置") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selected) {
                "today" -> TodayScreen(vm = vm)
                "stats" -> StatsScreen(vm = vm)
                "settings" -> SettingsScreen(vm = vm)
            }
        }
    }
}
