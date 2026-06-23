package com.jbcbros.qbitremote.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.nickname,
                onValueChange = viewModel::updateNickname,
                label = { Text("昵称") },
                placeholder = { Text("服务器名") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.host,
                onValueChange = viewModel::updateHost,
                label = { Text("地址") },
                placeholder = { Text("qBittorrent 地址") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.port,
                onValueChange = viewModel::updatePort,
                label = { Text("端口") },
                placeholder = { Text("WebUI 端口") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("启用 SSL", modifier = Modifier.weight(1f))
                Switch(checked = uiState.ssl, onCheckedChange = viewModel::updateSsl)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.username,
                onValueChange = viewModel::updateUsername,
                label = { Text("用户名") },
                placeholder = { Text("输入用户名") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::updatePassword,
                label = { Text("密码") },
                placeholder = { Text("输入密码") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.testAndSave {} },
                enabled = !uiState.isTesting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isTesting) "测试中…" else "保存")
            }

            if (uiState.testResult != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.testResult ?: "",
                    color = if (uiState.saved) Color(0xFF1E6738) else Color.Red
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.height(8.dp))
                Text("qBitRemote", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
