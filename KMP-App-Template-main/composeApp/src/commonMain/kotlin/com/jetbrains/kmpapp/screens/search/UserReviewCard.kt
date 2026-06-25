package com.jetbrains.kmpapp.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jetbrains.kmpapp.data.UserReview

@Composable
fun UserReviewCard(review: UserReview) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp)
        ) {
            AsyncImage(
                model = review.coverImageUrl,
                contentDescription = review.title,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = review.title,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = review.artistName,
                    style = MaterialTheme.typography.bodySmall
                )

                Text("★".repeat(review.rating.toInt()))

                Text(
                    text = review.content,
                    maxLines = 2
                )
            }
        }
    }
}