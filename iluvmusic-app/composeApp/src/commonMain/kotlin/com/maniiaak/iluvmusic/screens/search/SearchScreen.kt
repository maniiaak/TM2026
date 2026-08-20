package com.maniiaak.iluvmusic.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.maniiaak.iluvmusic.data.SearchResult
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Search Spotify") }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            val keyboardController = LocalSoftwareKeyboardController.current

            fun performSearch() {
                if (query.isNotBlank()) {
                    viewModel.searchAlbum(query)
                }
                keyboardController?.hide()
            }

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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { performSearch() },
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

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = 8.dp
                        )
                    ) {

                        items(

                            items = s.albums,
                            key = { album ->
                                album.albumId ?: album.spotifyId ?: album.title
                            }
                        ) { album ->

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                                    .clickable {

                                        if (album.exists) {

                                            onAlbumFound(album.albumId!!)

                                        } else {

                                            viewModel.importAlbum(
                                                query = album.spotifyId!!,
                                                onSuccess = { importedId ->
                                                    onAlbumFound(importedId)
                                                },
                                                onError = onError
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
                                        model = album.coverImage,
                                        contentDescription = album.title,
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )

                                    Spacer(Modifier.width(16.dp))

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {

                                        Text(
                                            text = album.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 2
                                        )

                                        Text(
                                            text = album.artist,
                                            style = MaterialTheme.typography.bodyMedium
                                        )

                                        Spacer(Modifier.height(4.dp))

                                        Text(
                                            text = if (album.exists)
                                                "Already in library"
                                            else
                                                "Import from Spotify",
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
    }
}

@Immutable
sealed class SearchState {

    object Idle : SearchState()

    object Loading : SearchState()

    data class Success(
        val albums: List<SearchResult>
    ) : SearchState()

    data class Error(
        val message: String
    ) : SearchState()
}