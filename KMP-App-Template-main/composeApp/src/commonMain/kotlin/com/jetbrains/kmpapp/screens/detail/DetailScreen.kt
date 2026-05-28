package com.jetbrains.kmpapp.screens.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.jetbrains.kmpapp.data.MuseumObject
import com.jetbrains.kmpapp.data.Review
import com.jetbrains.kmpapp.data.SessionManager
import com.jetbrains.kmpapp.screens.EmptyScreenContent
import kmp_app_template.composeapp.generated.resources.Res
import kmp_app_template.composeapp.generated.resources.back
import kmp_app_template.composeapp.generated.resources.label_artist
import kmp_app_template.composeapp.generated.resources.label_date
import kmp_app_template.composeapp.generated.resources.label_length
import kmp_app_template.composeapp.generated.resources.label_title
import kmp_app_template.composeapp.generated.resources.label_tracks
import kmp_app_template.composeapp.generated.resources.label_type
import kmp_app_template.composeapp.generated.resources.label_total_ratings
import kmp_app_template.composeapp.generated.resources.label_rating
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect

@Composable
fun DetailScreen(
    objectId: Int,
    navigateBack: () -> Unit,
    viewModel: DetailViewModel = koinViewModel(),
    sessionManager: SessionManager = koinInject()
) {
    LaunchedEffect(objectId) {
        viewModel.loadReviews(objectId)
    }

    val reviews by viewModel.reviews.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingReviews.collectAsStateWithLifecycle()
    val userId by sessionManager.userId.collectAsStateWithLifecycle(initialValue = 0)

    var showNoteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun handleSaveNote(note: String, rating: Float?) {
        if (rating == null) {
            Toast.makeText(context, "Please enter a rating", Toast.LENGTH_SHORT).show()
            return
        }
        if (userId == 0) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        if (viewModel.reviewState.value is ReviewState.Loading) return

        viewModel.saveReview(rating, note, objectId, userId)
        Toast.makeText(context, "Review submitted!", Toast.LENGTH_SHORT).show()
        showNoteDialog = false
    }

    val obj by viewModel.getObject(objectId).collectAsStateWithLifecycle(initialValue = null)

    AnimatedContent(obj != null) { objectAvailable ->
        if (objectAvailable) {
            ObjectDetails(
                obj = obj!!,
                onBackClick = navigateBack,
                onShowNoteDialog = { showNoteDialog = true },
                reviews = reviews,
                isLoading = isLoading,
                handleSaveNote = ::handleSaveNote
            )

            NoteDialog(
                isOpen = showNoteDialog,
                onDismiss = { showNoteDialog = false },
                onSave = { note, rating -> handleSaveNote(note, rating) }
            )
        } else {
            EmptyScreenContent(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ObjectDetails(
    obj: MuseumObject,
    onBackClick: () -> Unit,
    onShowNoteDialog: () -> Unit,
    reviews: List<Review>,
    isLoading: Boolean,
    handleSaveNote: (String, Float?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(obj.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onShowNoteDialog,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note & Rating")
            }
        },
        modifier = modifier.windowInsetsPadding(WindowInsets.systemBars),
    ) { paddingValues ->
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
        ) {
            AsyncImage(
                model = obj.coverImage,
                contentDescription = obj.title,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.LightGray)
            )

            SelectionContainer {
                Column(Modifier.padding(16.dp)) {
                    Text(obj.title, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))

                    LabeledInfo(stringResource(Res.string.label_artist), obj.artistDisplayName ?: "Unknown")
                    LabeledInfo(stringResource(Res.string.label_date), obj.objectDate ?: "Unknown")
                    LabeledInfo(stringResource(Res.string.label_type), obj.type ?: "Album")
                    LabeledInfo(stringResource(Res.string.label_length), obj.length ?: "0:00")
                    LabeledInfo(stringResource(Res.string.label_tracks), obj.tracks ?: "0")

                    LabeledInfo(stringResource(Res.string.label_total_ratings), (obj.totalRatings ?: 0).toString())
                    LabeledInfo(stringResource(Res.string.label_rating), (obj.rating ?: 0.0).toString())

                    Spacer(Modifier.height(16.dp))

                    if (isLoading) {
                        androidx.compose.material3.CircularProgressIndicator()
                    } else if (reviews.isEmpty()) {
                        Text(
                            text = "No reviews yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        ReviewsList(reviews = reviews)
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledInfo(
    label: String,
    data: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(vertical = 4.dp)) {
        Text(
            buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("$label: ")
                }
                append(data)
            },
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ReviewsList(reviews: List<Review>) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            text = "User Reviews",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        reviews.forEach { review ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = review.username,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Rating: ${review.rating}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = review.content,
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = review.createdAt,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}