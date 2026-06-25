package com.jetbrains.kmpapp.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel
) {
    val stats by viewModel.userStats.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        stats?.let { user ->

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "👤",
                        fontSize = 64.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = user.username,
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Spacer(Modifier.height(24.dp))

                    HorizontalDivider()

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${user.reviewCount}",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text("Reviews")
                        }
                    }
                }
            }
        } ?: CircularProgressIndicator()
    }
}