package com.jbcbros.qbitremote.ui.home

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jbcbros.qbitremote.data.model.Torrent
import com.jbcbros.qbitremote.util.formatBytes
import com.jbcbros.qbitremote.util.formatSpeed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToUpload: () -> Unit,
    onNavigateToDetail: (String, String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("远程", color = Color.White)
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
                        Icon(Icons.Default.Add, contentDescription = "添加", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = { Text("搜索种子") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )
            FilterRow(uiState.filter, viewModel::setFilter)

            if (!uiState.hasConfig) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("请先配置服务器（设置页面）", textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(uiState.torrents, key = { it.hash }) { torrent ->
                        TorrentItem(torrent = torrent, onClick = {
                            onNavigateToDetail(torrent.hash, torrent.name)
                        })
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(currentFilter: FilterType, onFilterSelected: (FilterType) -> Unit) {
    val filters = listOf(FilterType.All, FilterType.Downloading, FilterType.Completed)
    val labels = mapOf(
        FilterType.All to "全部",
        FilterType.Downloading to "下载中",
        FilterType.Completed to "已完成"
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
private fun TorrentItem(torrent: Torrent, onClick: () -> Unit) {
    val stateLabel = mapOf(
        "error" to "出错",
        "missingFiles" to "缺失文件",
        "uploading" to "做种",
        "pausedUP" to "已暂停",
        "queuedUP" to "排队做种",
        "stalledUP" to "做种",
        "checkingUP" to "校验中",
        "forcedUP" to "强制做种",
        "allocating" to "分配空间",
        "downloading" to "下载中",
        "metaDL" to "下载元数据",
        "pausedDL" to "已暂停",
        "queuedDL" to "排队下载",
        "stalledDL" to "下载中",
        "checkingDL" to "校验中",
        "forcedDL" to "强制下载",
        "checkingResumeData" to "校验中",
        "moving" to "移动文件",
        "unknown" to "未知"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
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
