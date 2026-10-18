package eu.kanade.tachiyomi.ui.browse.extension

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.extension.anime.api.AnimeExtensionRepos

/**
 * Lets the user add and remove anime extension repositories.
 */
class AnimeExtensionReposScreen : Screen() {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        var repos by remember { mutableStateOf(AnimeExtensionRepos.getRepos()) }
        var showAddDialog by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "Anime Extension Repos") },
                    navigationIcon = {
                        IconButton(onClick = navigator::pop) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add repository")
                }
            },
        ) { padding ->
            if (repos.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(padding)
                        .padding(24.dp),
                ) {
                    Text(
                        text = "No repositories yet. Tap + to add a repository URL.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                LazyColumn(contentPadding = padding) {
                    items(repos, key = { it }) { url ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = url,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = {
                                    AnimeExtensionRepos.removeRepo(url)
                                    repos = AnimeExtensionRepos.getRepos()
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Remove repository",
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddRepoDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { url ->
                    if (AnimeExtensionRepos.addRepo(url)) {
                        repos = AnimeExtensionRepos.getRepos()
                        showAddDialog = false
                        true
                    } else {
                        false
                    }
                },
            )
        }
    }
}

@Composable
private fun AddRepoDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Boolean,
) {
    var text by remember { mutableStateOf("") }
    var isInvalid by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add repository") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    isInvalid = false
                },
                singleLine = true,
                isError = isInvalid,
                label = { Text(text = "Repository URL") },
                supportingText = {
                    if (isInvalid) {
                        Text(text = "Enter a valid http:// or https:// URL")
                    }
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!onAdd(text)) isInvalid = true
                },
            ) {
                Text(text = "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        },
    )
}
