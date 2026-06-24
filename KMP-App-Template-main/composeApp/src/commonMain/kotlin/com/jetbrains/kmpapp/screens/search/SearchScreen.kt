package com.jetbrains.kmpapp.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onAlbumFound: (Int) -> Unit,
    onError: (String) -> Unit,
    viewModel: SearchViewModel = koinViewModel()
) {

    var query by remember { mutableStateOf("") }

    val uiState by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Spotify") }
            )
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
                leadingIcon = {
                    Icon(Icons.Default.Search, null)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { query = "" }
                        ) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (query.isNotBlank()) {
                        viewModel.searchAlbum(query)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Find Album")
            }

            Spacer(Modifier.height(24.dp))

            when (val s = uiState) {

                SearchState.Idle -> {}

                SearchState.Loading -> {

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is SearchState.Error -> {

                    Text(
                        text = s.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                is SearchState.Success -> {

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {

                                if (s.albumId != null) {

                                    onAlbumFound(s.albumId)

                                } else {

                                    viewModel.importAlbum(
                                        query = s.spotifyId,
                                        onSuccess = { importedId ->
                                            onAlbumFound(importedId)
                                        },
                                        onError = { error ->
                                            onError(error)
                                        }
                                    )
                                }
                            }
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            AsyncImage(
                                model = s.coverImage,
                                contentDescription = s.title,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )

                            Spacer(Modifier.width(16.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text = s.title,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    text = s.artist,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text = "Tap to open album",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Immutable
sealed class SearchState {

    object Idle : SearchState()

    object Loading : SearchState()

    data class Success(
        val albumId: Int?,
        val title: String,
        val artist: String,
        val coverImage: String?,
        val source: String,
        val spotifyId: String
    ) : SearchState()

    data class Error(
        val message: String
    ) : SearchState()
}