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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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

    LaunchedEffect(hash) {
        viewModel.loadTorrent(hash)
    }

    LaunchedEffect(uiState.actionMessage) {
        if (uiState.actionMessage != null) {
            // Toast or Snackbar could be shown here
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("种子详情") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
            SectionTitle("操作")
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(onClick = { viewModel.pauseTorrent(hash) }, modifier = Modifier.fillMaxWidth()) {
                        Text("暂停")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.resumeTorrent(hash) }, modifier = Modifier.fillMaxWidth()) {
                        Text("恢复")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.recheckTorrent(hash) }, modifier = Modifier.fillMaxWidth()) {
                        Text("重新校验")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("删除种子")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle("基本信息")
            InfoCard(
                "名称" to (torrent?.name ?: "-"),
                "状态" to (torrent?.state ?: "-"),
                "大小" to (torrent?.total_size?.let { "${formatBytes(it)}" } ?: "-"),
                "种子数" to (torrent?.num_complete?.toString() ?: "-"),
                "分类" to (torrent?.category ?: "-")
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle("当前速度")
            InfoCard(
                "上传" to (torrent?.let { formatSpeed(it.upspeed) } ?: "-"),
                "下载" to (torrent?.let { formatSpeed(it.dlspeed) } ?: "-")
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle("下载信息")
            InfoCard(
                "已下载" to (torrent?.let { formatBytes(it.downloaded) } ?: "-"),
                "已上传" to (torrent?.let { formatBytes(it.uploaded) } ?: "-"),
                "比率" to (torrent?.let { "%.2f".format(it.ratio) } ?: "-"),
                "做种时间" to (torrent?.seeding_time?.let {
                    val hours = it / 3600
                    val minutes = (it % 3600) / 60
                    val seconds = it % 60
                    "%02d:%02d:%02d".format(hours, minutes, seconds)
                } ?: "-")
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle("存储")
            InfoCard("路径" to (torrent?.save_path ?: "-"))

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除种子?") },
            text = { Text(name) },
            confirmButton = {
                Button(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteTorrent(hash, onNavigateBack)
                }) {
                    Text("删除")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text("取消")
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
