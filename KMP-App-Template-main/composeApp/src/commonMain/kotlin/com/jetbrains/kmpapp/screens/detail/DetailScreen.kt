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
import com.jetbrains.kmpapp.screens.EmptyScreenContent
import kmp_app_template.composeapp.generated.resources.Res
import kmp_app_template.composeapp.generated.resources.back
import kmp_app_template.composeapp.generated.resources.label_artist
import kmp_app_template.composeapp.generated.resources.label_date
import kmp_app_template.composeapp.generated.resources.label_length
import kmp_app_template.composeapp.generated.resources.label_title
import kmp_app_template.composeapp.generated.resources.label_tracks
import kmp_app_template.composeapp.generated.resources.label_type
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import android.widget.Toast

@Composable
fun DetailScreen(
    objectId: Int,
    navigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<DetailViewModel>()

    // State for the dialog
    var showNoteDialog by remember { mutableStateOf(false) }

    // Context for Toast
    val context = LocalContext.current

    // Handler for saving
    fun handleSaveNote(note: String, rating: Float?) {
        val ratingDisplay = rating?.let { "Rating: $it/5 | " } ?: ""
        Toast.makeText(context, "${ratingDisplay}Note saved!", Toast.LENGTH_SHORT).show()
        // TODO: Connect this to your ViewModel/Repository to save permanently
    }

    val obj by viewModel.getObject(objectId).collectAsStateWithLifecycle(initialValue = null)

    AnimatedContent(obj != null) { objectAvailable ->
        if (objectAvailable) {
            ObjectDetails(
                obj = obj!!,
                onBackClick = navigateBack,
                onShowNoteDialog = { showNoteDialog = true }
            )

            // Render the Dialog
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
                // No actions here anymore, the FAB handles the "Add" action
            )
        },
        floatingActionButton = {
            // NEW: Prominent Floating Action Button
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

                    LabeledInfo(stringResource(Res.string.label_artist), obj.artistDisplayName)
                    LabeledInfo(stringResource(Res.string.label_date), obj.objectDate)
                    LabeledInfo(stringResource(Res.string.label_type), obj.type)
                    LabeledInfo(stringResource(Res.string.label_length), obj.length)
                    LabeledInfo(stringResource(Res.string.label_tracks), obj.tracks)
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