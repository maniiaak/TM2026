package com.maniiaak.iluvmusic.screens.profile

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, navigateToAlbum: (Int) -> Unit = {}, isOwnProfile: Boolean = false) {
    val stats by viewModel.userStats.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    val listState = rememberLazyListState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val isFollowing by viewModel.isFollowing.collectAsState()
    val isFollowLoading by viewModel.isFollowLoading.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

    if (stats == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    if (showSettings && isOwnProfile) {
        ProfileSettingsDialog(
            currentImageUrl = stats!!.profileImageUrl,
            onDismiss = { showSettings = false },
            onSave = { url -> viewModel.updateProfileImage(url); showSettings = false }
        )
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Card(Modifier.fillMaxWidth().padding(30.dp), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (stats!!.profileImageUrl.isNullOrBlank()) {
                            Icon(Icons.Default.AccountCircle, "Default profile picture", Modifier.size(88.dp))
                        } else {
                            AsyncImage(
                                model = stats!!.profileImageUrl,
                                contentDescription = "Profile picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(88.dp).clip(CircleShape)
                            )
                        }
                        Spacer(Modifier.size(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stats!!.username, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "@${stats!!.handle.orEmpty()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("${stats!!.reviewCount} reviews", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (isOwnProfile) {
                            IconButton(onClick = { showSettings = true }) {
                                Icon(Icons.Default.Settings, contentDescription = "Profile settings")
                            }
                        } else {
                            Button(onClick = { viewModel.toggleFollow() }, enabled = !isFollowLoading) {
                                if (isFollowLoading) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                                else Text(if (isFollowing) "Unfollow" else "Follow")
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column { Text(stats!!.followingCount.toString(), style = MaterialTheme.typography.titleMedium); Text("Following", style = MaterialTheme.typography.bodySmall) }
                        Column { Text(stats!!.followerCount.toString(), style = MaterialTheme.typography.titleMedium); Text("Followers", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
        item { Text("Recent Reviews", Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium) }
        items(reviews) { review -> UserReviewCard(review, onReviewClick = navigateToAlbum) }
        item { if (isLoadingMore) Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }.collect { lastVisibleIndex ->
            if (lastVisibleIndex != null && lastVisibleIndex >= reviews.size - 2) viewModel.loadMoreReviews()
        }
    }
}

@Composable
private fun ProfileSettingsDialog(currentImageUrl: String?, onDismiss: () -> Unit, onSave: (String?) -> Unit) {
    var imageUrl by remember(currentImageUrl) { mutableStateOf(currentImageUrl.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profile settings") },
        text = {
            Column {
                Text("Add a profile picture using an image URL.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Profile picture URL") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(imageUrl.trim().ifBlank { null }) }) { Text("Save") } },
        dismissButton = {
            Row {
                if (!currentImageUrl.isNullOrBlank()) TextButton(onClick = { onSave(null) }) { Text("Remove") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
