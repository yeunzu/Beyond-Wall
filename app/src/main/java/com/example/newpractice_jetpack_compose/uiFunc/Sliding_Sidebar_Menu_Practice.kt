package com.example.newpractice_jetpack_compose.uiFunc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController

import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidebarDemo(navController: NavHostController) {
    var showSidebar by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
//    val dragOffset = remember { mutableStateOf(0f) }

    val drawerState = rememberDrawerState(DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Menu", fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                HorizontalDivider()
                Text(
                    text = "Settings",
                    textAlign = TextAlign.Left,
                    modifier = Modifier
                        .clickable {
                            println("Menu 1 clicked")
                            navController.navigate("menu1")
                        }
                        .padding(16.dp)
                        .fillMaxWidth()
                )
                Text(
                    text = "Menu 2",
                    textAlign = TextAlign.Left,
                    modifier = Modifier
                        .clickable {
                            println("Menu 2 clicked")
                            navController.navigate("menu2")
                        }
                        .padding(16.dp)
                        .fillMaxWidth()
                )
            }
        },
        content = {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("My App") },
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch { drawerState.open() }
                            }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                // Main content
                Box(modifier = Modifier.padding(paddingValues).navigationBarsPadding()) {
                    DefaultHomeScreenView(navController = navController)
                }
            }
        }
    )

    // Overlay when sideber is open
    if (showSidebar) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable {
                    showSidebar = false
                }
                .navigationBarsPadding()
        ) {
            Text(
                text = "Menu",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            HorizontalDivider()
            Button(
                onClick = {
                    navController.navigate("menu1")
                    showSidebar = false
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Settings")
            }
            Button(
                onClick = {
                    navController.navigate("menu2")
                    showSidebar = false
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Menu2")
            }
            Spacer(Modifier)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview() {
    val navController = rememberNavController()
    SidebarDemo(navController = navController)
}