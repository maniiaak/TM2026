package com.maniiaak.iluvmusic.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    navigateToAlbum: (Int) -> Unit = {},
    isOwnProfile: Boolean = false,
    navigateBack: (() -> Unit)? = null
) {
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
            ProfileHeader(
                username = stats!!.username,
                handle = stats!!.handle,
                profileImageUrl = stats!!.profileImageUrl,
                // TODO: wire this to stats!!.bannerImageUrl once banner support is added to UserStats / the backend.
                bannerImageUrl = null,
                reviewCount = stats!!.reviewCount,
                followingCount = stats!!.followingCount,
                followerCount = stats!!.followerCount,
                isOwnProfile = isOwnProfile,
                isFollowing = isFollowing,
                isFollowLoading = isFollowLoading,
                onSettingsClick = { showSettings = true },
                onFollowClick = { viewModel.toggleFollow() },
                onBackClick = navigateBack
            )
        }
        item {
            Text(
                "Recent Reviews",
                Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        itemsIndexed(reviews, key = { index, review -> "${review.albumId}_$index" }) { _, review ->
            UserReviewCard(review, onReviewClick = navigateToAlbum)
        }
        item { if (isLoadingMore) Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if (!isLoadingMore && reviews.isNotEmpty() && lastVisibleIndex != null && lastVisibleIndex >= reviews.size - 2) {
                    viewModel.loadMoreReviews()
                }
            }
    }
}

@Composable
private fun ProfileHeader(
    username: String,
    handle: String?,
    profileImageUrl: String?,
    bannerImageUrl: String?,
    reviewCount: Int,
    followingCount: Int,
    followerCount: Int,
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    isFollowLoading: Boolean,
    onSettingsClick: () -> Unit,
    onFollowClick: () -> Unit,
    onBackClick: (() -> Unit)?
) {
    Column(Modifier.fillMaxWidth()) {

        // ---------- BANNER ----------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            if (bannerImageUrl.isNullOrBlank()) {
                // Placeholder gradient until banner uploads are supported.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
                                )
                            )
                        )
                )
            } else {
                AsyncImage(
                    model = bannerImageUrl,
                    contentDescription = "Profile banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Subtle scrim so a back button (if any) stays legible on top of a photo banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent)
                        )
                    )
            )

            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.35f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(12.dp)
                        .align(Alignment.TopStart)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }

            if (isOwnProfile) {
                IconButton(
                    onClick = onSettingsClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.35f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(12.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Profile settings")
                }
            }
        }

        // ---------- AVATAR + NAME (overlaps banner) ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .offset(y = (-36).dp),
            verticalAlignment = Alignment.Bottom
        ) {
            ProfileAvatar(imageUrl = profileImageUrl, size = 88.dp)

            Spacer(Modifier.width(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 4.dp)
            ) {
                Text(
                    text = username,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!handle.isNullOrBlank()) {
                    Text(
                        text = "@$handle",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // ---------- STATS ----------
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .offset(y = (-24).dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(value = reviewCount, label = "Reviews", modifier = Modifier.weight(1f))
                StatDivider()
                StatItem(value = followingCount, label = "Following", modifier = Modifier.weight(1f))
                StatDivider()
                StatItem(value = followerCount, label = "Followers", modifier = Modifier.weight(1f))
            }
        }

        // ---------- FOLLOW / UNFOLLOW ----------
        if (!isOwnProfile) {
            val followModifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .offset(y = (-16).dp)

            if (isFollowing) {
                OutlinedButton(
                    onClick = onFollowClick,
                    enabled = !isFollowLoading,
                    shape = RoundedCornerShape(20.dp),
                    modifier = followModifier
                ) {
                    FollowButtonContent(isFollowLoading, "Unfollow")
                }
            } else {
                Button(
                    onClick = onFollowClick,
                    enabled = !isFollowLoading,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = followModifier
                ) {
                    FollowButtonContent(isFollowLoading, "Follow")
                }
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun FollowButtonContent(isLoading: Boolean, label: String) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary
        )
    } else {
        Text(label)
    }
}

@Composable
private fun ProfileAvatar(imageUrl: String?, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(3.dp, MaterialTheme.colorScheme.background, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNullOrBlank()) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = "Default profile picture",
                modifier = Modifier.size(size),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Profile picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun StatItem(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .padding(vertical = 14.dp)
            .height(28.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
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
                // NOTE: add a matching "Banner image URL" field here once banner uploads
                // are wired up on the viewmodel/backend side.
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