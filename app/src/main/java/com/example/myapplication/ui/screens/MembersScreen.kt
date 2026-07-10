package com.example.myapplication.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    onBack: () -> Unit,
    viewModel: MiniAppViewModel = com.example.myapplication.LocalMiniAppViewModel.current
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("MEMBER") }

    val members = remember {
        mutableStateListOf(
            MemberItem("Juan Dela Cruz", "juan@company.com", "SUPER_ADMIN", "ACTIVE"),
            MemberItem("Maria Clara", "maria@company.com", "ADMIN", "ACTIVE"),
            MemberItem("Dev User", "dev@company.com", "DEVELOPER", "ACTIVE")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Team Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SwiftPayBackground)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = SwiftPayPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Rounded.PersonAdd, null) },
                text = { Text("Add Member") }
            )
        },
        containerColor = SwiftPayBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            items(members) { member ->
                MemberCard(member) {
                    members.remove(member)
                    viewModel.onDeleteMemberRequest(member.email)
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Invite New Member") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth())
                    
                    Text("Role", style = MaterialTheme.typography.labelSmall)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ADMIN", "MEMBER", "DEVELOPER").forEach { r ->
                            FilterChip(
                                selected = role == r,
                                onClick = { role = r },
                                label = { Text(r) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank() && email.isNotBlank()) {
                        members.add(MemberItem(name, email, role, "ACTIVE"))
                        viewModel.onAddMemberRequest(name, email, role)
                        showAddDialog = false
                    }
                }) {
                    Text("Send Invite", color = SwiftPayPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

data class MemberItem(val name: String, val email: String, val role: String, val status: String)

@Composable
fun MemberCard(member: MemberItem, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SwiftPaySurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, SwiftPayBorder)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).background(SwiftPayPrimary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(member.name.take(1), fontWeight = FontWeight.Bold, color = SwiftPayPrimary)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(member.name, fontWeight = FontWeight.Bold, color = SwiftPayTextPrimary)
                Text(member.email, style = MaterialTheme.typography.bodySmall, color = SwiftPayTextSecondary)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(if (member.status == "ACTIVE") SwiftPaySuccess else Color.Gray, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(member.role, style = MaterialTheme.typography.labelSmall, color = SwiftPayPrimary, fontWeight = FontWeight.Black)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = Color(0xFFE91E63))
            }
        }
    }
}
