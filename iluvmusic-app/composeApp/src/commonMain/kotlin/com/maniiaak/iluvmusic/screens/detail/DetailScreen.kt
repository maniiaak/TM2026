package com.maniiaak.iluvmusic.screens.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.maniiaak.iluvmusic.data.MuseumObject
import com.maniiaak.iluvmusic.data.Review
import com.maniiaak.iluvmusic.data.SessionManager
import com.maniiaak.iluvmusic.screens.StarRow
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
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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

    // Snackbar host state for cross-platform notifications
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun showToast(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    fun handleSaveNote(note: String, rating: Float?) {
        if (rating == null) {
            showToast("Please enter a rating")
            return
        }
        if (userId == 0) {
            showToast("User not logged in")
            return
        }
        if (viewModel.reviewState.value is ReviewState.Loading) return

        viewModel.saveReview(rating, note, objectId, userId)

        viewModel.loadReviews(objectId)

        showToast("Review submitted!")
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
            navigateToUserProfile = navigateToUserProfile,
            snackbarHostState = snackbarHostState
        )


        NoteDialog(
            isOpen = showNoteDialog,
            onDismiss = { showNoteDialog = false },
            onSave = { note, rating -> handleSaveNote(note, rating) }

        )
    } else {
        // Show loading or empty state while obj loads
        println("⏳ Waiting for album object to load...")
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
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
    totalRatings: Int,
    averageRating: Double,
    modifier: Modifier = Modifier,
    navigateToUserProfile: (Int) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onShowNoteDialog,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Note & Rating")
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            // ---------- HERO HEADER ----------
            HeroHeader(obj = obj, onBackClick = onBackClick)

            // ---------- CONTENT ----------
            SelectionContainer {
                Column(Modifier.padding(horizontal = 20.dp)) {

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = obj.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "${obj.artistDisplayName ?: "Unknown artist"} · ${obj.objectDate ?: "Unknown"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    // Info badges row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoChip(
                            label = stringResource(Res.string.label_type),
                            value = obj.type ?: "Album"
                        )
                        InfoChip(
                            label = stringResource(Res.string.label_length),
                            value = obj.length ?: "0:00"
                        )
                        InfoChip(
                            label = stringResource(Res.string.label_tracks),
                            value = obj.tracks?.toString() ?: "0"
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // Rating summary card
                    RatingSummary(
                        averageRating = averageRating,
                        totalRatings = totalRatings
                    )

                    Spacer(Modifier.height(28.dp))

                    Text(
                        text = "Reviews",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp) // reserve space
                    ) {
                        when {
                            isLoading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }

                            reviews.isEmpty() -> {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No reviews yet",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Be the first to review this item!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
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
private fun HeroHeader(
    obj: MuseumObject,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        // Backdrop image
        AsyncImage(
            model = obj.coverImage,
            contentDescription = obj.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Scrim gradient so the back button + overlap card stay legible
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.35f),
                            0.55f to Color.Black.copy(alpha = 0.05f),
                            1.0f to MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        // Back button
        IconButton(
            onClick = onBackClick,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.35f),
                contentColor = Color.White
            ),
            modifier = Modifier
                .statusBarsPadding()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(12.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back))
        }

        // Overlapping cover thumbnail
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp)
                .offset(y = 36.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            AsyncImage(
                model = obj.coverImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }

    Spacer(Modifier.height(36.dp))
}

@Composable
private fun InfoChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RatingSummary(
    averageRating: Double,
    totalRatings: Int
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatOneDecimal(averageRating),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.width(14.dp))

            Column {
                StarRow(rating = averageRating)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${stringResource(Res.string.label_total_ratings)}: $totalRatings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatOneDecimal(value: Double): String {
    val roundedTenths = (value * 10).roundToInt()
    val whole = roundedTenths / 10
    val tenths = kotlin.math.abs(roundedTenths % 10)
    return "$whole.$tenths"
}