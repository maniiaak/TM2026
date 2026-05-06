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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import kmp_app_template.composeapp.generated.resources.label_type
import kmp_app_template.composeapp.generated.resources.label_tracks
import kmp_app_template.composeapp.generated.resources.label_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun DetailScreen(
    objectId: Int,
    navigateBack: () -> Unit,
) {
    val viewModel = koinViewModel<DetailViewModel>()

    var showNoteDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    fun handleSaveNote(note: String) {
        // TODO: Save this note to your database/storage later
        Toast.makeText(context, "Note saved: $note", Toast.LENGTH_SHORT).show()
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
                onSave = { note -> handleSaveNote(note) }
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
    onShowNoteDialog: () -> Unit, // New parameter
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text(obj.title) }, // Added title back for clarity
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back))
                    }
                },
                actions = {
                    // Add the button here
                    IconButton(onClick = onShowNoteDialog) {
                        Icon(Icons.Default.Edit, contentDescription = "Add Note")
                    }
                }
            )
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
                Column(Modifier.padding(12.dp)) {
                    Text(obj.title, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(6.dp))
                    LabeledInfo(stringResource(Res.string.label_title), obj.title)
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
        Spacer(Modifier.height(6.dp))
        Text(
            buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("$label: ")
                }
                append(data)
            }
        )
    }
}