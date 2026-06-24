@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.jbcbros.qbitremote.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.jbcbros.qbitremote.R
import com.jbcbros.qbitremote.data.model.Torrent
import com.jbcbros.qbitremote.util.formatBytes
import com.jbcbros.qbitremote.util.formatSpeed

@Composable
fun HomeScreen(
    onNavigateToUpload: () -> Unit,
    onNavigateToDetail: (String, String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedTorrent by remember { mutableStateOf<Torrent?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage = uiState.snackbarMessage
    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            snackbarHostState.showSnackbar(snackbarMessage)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.title_remote), color = Color.White)
                        Text(
                            text = "↑${formatSpeed(uiState.transferInfo.up_info_speed)}  ↓${formatSpeed(uiState.transferInfo.dl_info_speed)}",
                            color = Color(0xFFdbeafe),
                            fontSize = 12.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2f6fed),
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = onNavigateToUpload) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add), tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = { Text(stringResource(R.string.hint_search_torrents)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.hint_search_torrents)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )
            FilterRow(uiState.filter, viewModel::setFilter)
            CategoryRow(uiState.categories, uiState.selectedCategory, viewModel::setCategory)

            if (!uiState.hasConfig) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.hint_configure_server), textAlign = TextAlign.Center)
                }
            } else if (uiState.torrents.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.hint_empty_torrents), textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(uiState.torrents, key = { it.hash }) { torrent ->
                        TorrentItem(
                            torrent = torrent,
                            onClick = { onNavigateToDetail(torrent.hash, torrent.name) },
                            onLongClick = {
                                selectedTorrent = torrent
                                showBottomSheet = true
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showBottomSheet && selectedTorrent != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = selectedTorrent!!.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.action_pause)) },
                    leadingContent = { Icon(Icons.Default.Pause, contentDescription = stringResource(R.string.action_pause)) },
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            viewModel.pauseTorrent(selectedTorrent!!.hash)
                            showBottomSheet = false
                        }
                    )
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.action_resume)) },
                    leadingContent = { Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.action_resume)) },
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            viewModel.resumeTorrent(selectedTorrent!!.hash)
                            showBottomSheet = false
                        }
                    )
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.action_delete)) },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), tint = Color.Red) },
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            showBottomSheet = false
                            showDeleteDialog = true
                        }
                    )
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showDeleteDialog && selectedTorrent != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.dialog_delete_torrent_title)) },
            text = { Text(selectedTorrent!!.name, maxLines = 2) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTorrent(selectedTorrent!!.hash)
                    showDeleteDialog = false
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun CategoryRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val allLabel = stringResource(R.string.filter_all)
    val allCategories = listOf("" to allLabel) + categories.map { it to it }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = if (selectedCategory.isBlank()) allLabel else selectedCategory,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.label_category)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            allCategories.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onCategorySelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FilterRow(currentFilter: FilterType, onFilterSelected: (FilterType) -> Unit) {
    val filters = listOf(FilterType.All, FilterType.Downloading, FilterType.Completed)
    val labels = mapOf(
        FilterType.All to stringResource(R.string.filter_all),
        FilterType.Downloading to stringResource(R.string.filter_downloading),
        FilterType.Completed to stringResource(R.string.filter_completed)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        filters.forEach { filter ->
            FilterChip(
                selected = currentFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(labels[filter] ?: "") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TorrentItem(torrent: Torrent, onClick: () -> Unit, onLongClick: () -> Unit) {
    val stateLabel = rememberStateLabels()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Text(
            text = torrent.name,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { torrent.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = Color(0xFF42A5F5)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stateLabel[torrent.state] ?: torrent.state, fontSize = 13.sp)
            Text(text = "↑ ${formatBytes(torrent.uploaded)} ↓ ${formatBytes(torrent.downloaded)}", fontSize = 13.sp)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "↑ ${formatSpeed(torrent.upspeed)} ↓ ${formatSpeed(torrent.dlspeed)}", fontSize = 13.sp)
            Text(text = "${String.format("%.2f", torrent.ratio)}", fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth())
    }
}

@Composable
private fun rememberStateLabels(): Map<String, String> = mapOf(
    "error" to stringResource(R.string.state_error),
    "missingFiles" to stringResource(R.string.state_missing_files),
    "uploading" to stringResource(R.string.state_uploading),
    "pausedUP" to stringResource(R.string.state_paused_up),
    "queuedUP" to stringResource(R.string.state_queued_up),
    "stalledUP" to stringResource(R.string.state_stalled_up),
    "checkingUP" to stringResource(R.string.state_checking_up),
    "forcedUP" to stringResource(R.string.state_forced_up),
    "allocating" to stringResource(R.string.state_allocating),
    "downloading" to stringResource(R.string.state_downloading),
    "metaDL" to stringResource(R.string.state_meta_dl),
    "pausedDL" to stringResource(R.string.state_paused_dl),
    "queuedDL" to stringResource(R.string.state_queued_dl),
    "stalledDL" to stringResource(R.string.state_stalled_dl),
    "checkingDL" to stringResource(R.string.state_checking_dl),
    "forcedDL" to stringResource(R.string.state_forced_dl),
    "checkingResumeData" to stringResource(R.string.state_checking_resume_data),
    "moving" to stringResource(R.string.state_moving),
    "unknown" to stringResource(R.string.state_unknown)
)
