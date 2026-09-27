package com.editog.novaagent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.editog.novaagent.data.model.ActionPendingConfirmation
import com.editog.novaagent.data.model.ContactRowChoice

@Composable
fun ConfirmationDialog(
    pendingAction: ActionPendingConfirmation,
    onConfirm: (ContactRowChoice?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRow by remember { mutableStateOf<ContactRowChoice?>(pendingAction.candidateRows.firstOrNull()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF131622),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFF00F0FF), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (pendingAction.actionType == "CALL") "Confirm Call" else "Confirm Message",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (pendingAction.candidateRows.size > 1) {
                    Text(
                        text = "Multiple numbers found for \"${pendingAction.targetName}\". Select a row number to proceed:",
                        color = Color(0xFF9CA3AF),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                    ) {
                        items(pendingAction.candidateRows) { choice ->
                            val isSelected = selectedRow?.rowNumber == choice.rowNumber
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(
                                        if (isSelected) Color(0xFF00F0FF).copy(alpha = 0.2f) else Color(0xFF1B1E2E),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF00F0FF) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedRow = choice }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Row ${choice.rowNumber}:",
                                    color = Color(0xFF00F0FF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = choice.phoneNumber,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = choice.type,
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Do you want to make this action to ${pendingAction.targetName} (${pendingAction.targetNumber})?",
                        color = Color(0xFFE0E2EC),
                        fontSize = 14.sp
                    )
                }

                if (!pendingAction.messageContent.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0B0D14), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Message: \"${pendingAction.messageContent}\"",
                            color = Color(0xFF00FF66),
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5555)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("No")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { onConfirm(selectedRow) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF), contentColor = Color.Black),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Yes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
