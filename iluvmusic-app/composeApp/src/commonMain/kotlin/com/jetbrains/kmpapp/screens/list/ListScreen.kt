package com.jetbrains.kmpapp.screens.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
// removed grid-based imports; using LazyRow for categories
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.jetbrains.kmpapp.data.MuseumObject
import com.jetbrains.kmpapp.screens.EmptyScreenContent
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    navigateToDetails: (objectId: Int) -> Unit,
    navigateToCategory: (category: String) -> Unit,
    onLogout: suspend () -> Unit,
    viewModel: ListViewModel = koinViewModel()
) {
    val scope = rememberCoroutineScope()
    val popularThisWeek by viewModel.popularThisWeek.collectAsStateWithLifecycle()
    val newlyByFriends by viewModel.newlyReviewedByFriends.collectAsStateWithLifecycle()
    val popularWithFriends by viewModel.popularWithFriends.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Albums") },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                onLogout()
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (popularThisWeek.isEmpty() && newlyByFriends.isEmpty() && popularWithFriends.isEmpty()) {
            EmptyScreenContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(8.dp)
            ) {
                // Always show popular this week
                CategoryRow(
                    title = "Popular this week",
                    items = popularThisWeek,
                    onClick = navigateToDetails,
                    onSeeMore = { navigateToCategory("popular_this_week") }
                )
                Spacer(Modifier.height(12.dp))

                // Show newly reviewed by friends only if user has friends
                if (newlyByFriends.isNotEmpty()) {
                    CategoryRow(
                        title = "Newly reviewed by friends",
                        items = newlyByFriends,
                        onClick = navigateToDetails,
                        onSeeMore = { navigateToCategory("newly_reviewed_by_friends") }
                    )
                    Spacer(Modifier.height(12.dp))
                } else {
                    EmptyFriendCategoryPlaceholder()
                    Spacer(Modifier.height(12.dp))
                }

                // Show popular with friends only if user has friends
                if (popularWithFriends.isNotEmpty()) {
                    CategoryRow(
                        title = "Popular with friends",
                        items = popularWithFriends,
                        onClick = navigateToDetails,
                        onSeeMore = { navigateToCategory("popular_with_friends") }
                    )
                } else {
                    EmptyFriendCategoryPlaceholder()
                }
            }
        }
    }
}

@Composable
private fun ObjectFrame(
    obj: MuseumObject,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = obj.coverImage,
            contentDescription = obj.title,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                //.background(androidx.compose.ui.graphics.Color.LightGray)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.height(2.dp))

        Text(obj.title, style = MaterialTheme.typography.titleSmall)
        Text(obj.artistDisplayName ?: "Unknown", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun HorizontalAlbumItem(
    obj: MuseumObject,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(120.dp)
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = obj.coverImage,
            contentDescription = obj.title,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                //.background(androidx.compose.ui.graphics.Color.LightGray)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(Modifier.height(4.dp))

        Text(obj.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
        Text(obj.artistDisplayName ?: "Unknown", style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}

@Composable
private fun CategoryRow(
    title: String,
    items: List<MuseumObject>,
    onClick: (Int) -> Unit,
    onSeeMore: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "See more",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onSeeMore() }
            )
        }
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            items(items) { item ->
                HorizontalAlbumItem(obj = item, onClick = { onClick(item.objectID) })
            }
        }
    }
}

@Composable
private fun EmptyFriendCategoryPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Start following some users to get recommendations!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
