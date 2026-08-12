package com.maniiaak.iluvmusic.screens.detail

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.maniiaak.iluvmusic.data.MuseumObject
import com.maniiaak.iluvmusic.data.Review
import com.maniiaak.iluvmusic.data.SessionManager
import kmp_app_template.composeapp.generated.resources.Res
import kmp_app_template.composeapp.generated.resources.back
import kmp_app_template.composeapp.generated.resources.label_artist
import kmp_app_template.composeapp.generated.resources.label_date
import kmp_app_template.composeapp.generated.resources.label_length
import kmp_app_template.composeapp.generated.resources.label_rating
import kmp_app_template.composeapp.generated.resources.label_total_ratings
import kmp_app_template.composeapp.generated.resources.label_tracks
import kmp_app_template.composeapp.generated.resources.label_type
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DetailScreen(
    objectId: Int,
    navigateBack: () -> Unit,
    navigateToUserProfile: (Int) -> Unit,
    viewModel: DetailViewModel = koinViewModel(),
    sessionManager: SessionManager = koinInject()
) {
    println("DetailScreen Composing for Album ID: $objectId")

    LaunchedEffect(objectId) {
        println("LaunchedEffect triggered for Album ID: $objectId")
        viewModel.loadReviews(objectId)
    }

    val reviews by viewModel.reviews.collectAsStateWithLifecycle()
    val totalRatings by viewModel.totalRatings.collectAsStateWithLifecycle()
    val averageRating by viewModel.averageRating.collectAsStateWithLifecycle()
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

        viewModel.loadReviews(objectId)

        Toast.makeText(context, "Review submitted!", Toast.LENGTH_SHORT).show()
        showNoteDialog = false
    }

    // Get the album object
    val obj by viewModel.getObject(objectId).collectAsStateWithLifecycle(initialValue = null)

    // RENDER LOGIC
    if (obj != null) {
        ObjectDetails(
            obj = obj!!,
            onBackClick = navigateBack,
            onShowNoteDialog = { showNoteDialog = true },
            reviews = reviews,
            isLoading = isLoading,
            handleSaveNote = ::handleSaveNote,
            totalRatings = totalRatings,
            averageRating = averageRating,
            navigateToUserProfile = navigateToUserProfile
        )


        NoteDialog(
            isOpen = showNoteDialog,
            onDismiss = { showNoteDialog = false },
            onSave = { note, rating -> handleSaveNote(note, rating) }

        )
    } else {
        // Show loading or empty state while obj loads
        println("⏳ Waiting for album object to load...")
        androidx.compose.material3.CircularProgressIndicator(
            modifier = Modifier.fillMaxSize()
        )
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
    totalRatings: Int,
    averageRating: Double,
    modifier: Modifier = Modifier,
    navigateToUserProfile: (Int) -> Unit
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
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.offset(y = (-50).dp)
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
                modifier = Modifier
                    .size(300.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .align(Alignment.CenterHorizontally)
            )

            SelectionContainer {
                Column(Modifier.padding(16.dp)) {
                    Text(obj.title, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))

                    LabeledInfo(stringResource(Res.string.label_artist), obj.artistDisplayName ?: "Unknown")
                    LabeledInfo(stringResource(Res.string.label_date), obj.objectDate ?: "Unknown")
                    LabeledInfo(stringResource(Res.string.label_type), obj.type ?: "Album")
                    LabeledInfo(stringResource(Res.string.label_length), obj.length ?: "0:00")
                    LabeledInfo(stringResource(Res.string.label_tracks), obj.tracks?.toString() ?: "0")
                    LabeledInfo(stringResource(Res.string.label_total_ratings), totalRatings.toString())
                    LabeledInfo(stringResource(Res.string.label_rating), averageRating.toString())

                    Spacer(Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp) // reserve space
                    ) {
                        when {
                            isLoading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }

                            reviews.isEmpty() -> {
                                Text(
                                    text = "Be the first to review this item!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }

                            else -> {
                                ReviewsList(reviews = reviews, navigateToUserProfile = navigateToUserProfile)
                            }
                        }
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
fun ReviewsList(reviews: List<Review>, navigateToUserProfile: (Int) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(bottom=80.dp)
    ) {
        reviews.forEach { review ->
            val cardModifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = review.userId != null) { review.userId?.let { navigateToUserProfile(it) } }

            Card(
                modifier = cardModifier,

                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = review.username,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(Modifier.weight(1f))

                        review.rating?.let {
                            Text(
                                text = "⭐ $it",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = review.content,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = review.createdAt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}