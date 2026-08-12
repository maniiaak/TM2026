package com.maniiaak.iluvmusic.screens.profile

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    navigateToAlbum: (Int) -> Unit = {},
    isOwnProfile: Boolean = false
) {
    val stats by viewModel.userStats.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    val listState = rememberLazyListState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val isFollowing by viewModel.isFollowing.collectAsState()
    val isFollowLoading by viewModel.isFollowLoading.collectAsState()

    if (stats == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = 100.dp)
    ) {

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(30.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stats!!.username,
                                style = MaterialTheme.typography.titleLarge
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = "${stats!!.reviewCount} reviews",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(Modifier.height(8.dp))

                            Row(
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                            ) {
                                Column {
                                    Text(
                                        text = stats!!.followingCount.toString(),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Following",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Column {
                                    Text(
                                        text = stats!!.followerCount.toString(),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Followers",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        if (!isOwnProfile) {
                            Button(
                                onClick = { viewModel.toggleFollow() },
                                enabled = !isFollowLoading,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                if (isFollowLoading) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text(if (isFollowing) "Unfollow" else "Follow")
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Recent Reviews",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium
            )
        }

        items(reviews) { review ->
            UserReviewCard(review, onReviewClick = navigateToAlbum)
        }

        item {
            if (isLoadingMore) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    LaunchedEffect(listState) {

        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }.collect { lastVisibleIndex ->

            if (
                lastVisibleIndex != null &&
                lastVisibleIndex >= reviews.size - 2
            ) {
                viewModel.loadMoreReviews()
            }
        }
    }
}