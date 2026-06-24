package com.jetbrains.kmpapp.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onAlbumFound: (Int) -> Unit,
    onError: (String) -> Unit,
    viewModel: SearchViewModel = koinViewModel()
) {
    var query by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val uiState by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Search Spotify") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Enter Album Name") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, null) }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSearching,
                supportingText = {
                    if (uiState is SearchState.Error) {
                        Text(
                            text = (uiState as SearchState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (query.isNotBlank()) {
                        isSearching = true
                        scope.launch {
                            viewModel.searchAndSync(query)
                            isSearching = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = query.isNotBlank() && !isSearching
            ) {
                Text(if (isSearching) "Searching..." else "Find Album")
            }

            Spacer(Modifier.height(32.dp))

            when (val s = uiState) {
                is SearchState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is SearchState.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Album Found!", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("Title: ${s.albumTitle}", style = MaterialTheme.typography.bodyLarge)
                            Text("Artist: ${s.artistName}", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(8.dp))
                            TextButton(
                                onClick = { onAlbumFound(s.albumId) }
                            ) {
                                Text("Open Details")
                            }
                        }
                    }
                }
                is SearchState.Error -> {
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                }
                is SearchState.NotFound -> {
                    Text("No match found in Spotify.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                is SearchState.Idle -> {}
            }
        }
    }
}

@Immutable
sealed class SearchState {
    object Idle : SearchState()
    object Loading : SearchState()
    data class Success(val albumId: Int, val albumTitle: String, val artistName: String) : SearchState()
    data class Error(val message: String) : SearchState()
    object NotFound : SearchState()
}