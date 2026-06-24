package com.jbcbros.qbitremote.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jbcbros.qbitremote.R
import com.jbcbros.qbitremote.util.formatBytes
import com.jbcbros.qbitremote.util.formatSpeed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorrentDetailScreen(
    hash: String,
    name: String,
    onNavigateBack: () -> Unit,
    viewModel: TorrentDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteFiles by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(hash) {
        viewModel.loadTorrent(hash)
    }

    DisposableEffect(hash) {
        viewModel.startPolling(hash)
        onDispose {
            viewModel.stopPolling()
        }
    }

    LaunchedEffect(uiState.actionMessage) {
        if (uiState.actionMessage != null) {
            if (uiState.actionSuccess) {
                vibrate(context)
            }
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(uiState.connectionError) {
        if (uiState.connectionError != null) {
            snackbarHostState.showSnackbar(uiState.connectionError)
            viewModel.clearConnectionError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_torrent_detail)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_cancel))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { padding ->
        val torrent = uiState.torrent
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SectionTitle(stringResource(R.string.label_actions))
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(onClick = { viewModel.pauseTorrent(hash) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.action_pause))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.resumeTorrent(hash) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.action_resume))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.recheckTorrent(hash) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.action_recheck))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.action_delete_torrent))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle(stringResource(R.string.label_basic_info))
            InfoCard(
                stringResource(R.string.label_name) to (torrent?.name ?: "-"),
                stringResource(R.string.label_state) to (torrent?.state ?: "-"),
                stringResource(R.string.label_size) to (torrent?.total_size?.let { "${formatBytes(it)}" } ?: "-"),
                stringResource(R.string.label_seeders) to (torrent?.num_complete?.toString() ?: "-"),
                stringResource(R.string.label_category) to (torrent?.category ?: "-")
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle(stringResource(R.string.label_current_speed))
            InfoCard(
                stringResource(R.string.label_upload) to (torrent?.let { formatSpeed(it.upspeed) } ?: "-"),
                stringResource(R.string.label_download) to (torrent?.let { formatSpeed(it.dlspeed) } ?: "-")
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle(stringResource(R.string.label_download_info))
            InfoCard(
                stringResource(R.string.label_downloaded) to (torrent?.let { formatBytes(it.downloaded) } ?: "-"),
                stringResource(R.string.label_uploaded) to (torrent?.let { formatBytes(it.uploaded) } ?: "-"),
                stringResource(R.string.label_ratio) to (torrent?.let { "%.2f".format(it.ratio) } ?: "-"),
                stringResource(R.string.label_seeding_time) to (torrent?.seeding_time?.let {
                    val hours = it / 3600
                    val minutes = (it % 3600) / 60
                    val seconds = it % 60
                    "%02d:%02d:%02d".format(hours, minutes, seconds)
                } ?: "-")
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle(stringResource(R.string.label_storage))
            InfoCard(stringResource(R.string.label_path) to (torrent?.save_path ?: "-"))

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.dialog_delete_torrent_title)) },
            text = {
                Column {
                    Text(name)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = deleteFiles,
                            onCheckedChange = { deleteFiles = it }
                        )
                        Text(stringResource(R.string.label_delete_files))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteTorrent(hash, deleteFiles, onNavigateBack)
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        modifier = Modifier.padding(horizontal = 30.dp, vertical = 8.dp)
    )
}

@Composable
private fun InfoCard(vararg items: Pair<String, String>) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = label, fontSize = 17.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = value, fontSize = 17.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

private fun vibrate(context: android.content.Context) {
    val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        vibrator.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(50)
    }
}
