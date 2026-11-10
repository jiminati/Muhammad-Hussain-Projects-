package com.vlektra.batterydoctor.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vlektra.batterydoctor.AppState
import com.vlektra.batterydoctor.report.ReportBuilder
import com.vlektra.batterydoctor.ui.theme.Vlk

@Composable
fun ReportScreen(state: AppState) {
    val snapshot = state.snapshot
    val diagnosis = state.diagnosis
    if (snapshot == null || diagnosis == null) {
        EmptyState("Nothing to report", "Diagnose a pack first.")
        return
    }

    val context = LocalContext.current
    var showFull by remember { mutableStateOf(false) }

    val shortMessage = ReportBuilder.whatsAppMessage(state.job, snapshot, diagnosis)
    val fullReport = ReportBuilder.fullReport(
        state.job, snapshot, state.history, state.parameters, diagnosis
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        Column {
            Text("Report", style = MaterialTheme.typography.headlineSmall, color = Vlk.Text)
            Text(
                "Fill in the job details, then send.",
                color = Vlk.TextDim,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Card {
            SectionLabel("Job card")
            Field("Chassis number", state.job.chassisNumber) {
                state.job = state.job.copy(chassisNumber = it)
            }
            Field("Battery ID", state.job.batteryId) {
                state.job = state.job.copy(batteryId = it)
            }
            Field("Customer name", state.job.customerName) {
                state.job = state.job.copy(customerName = it)
            }
            Field("Customer phone", state.job.customerPhone, KeyboardType.Phone) {
                state.job = state.job.copy(customerPhone = it)
            }
            Field("Model", state.job.model) {
                state.job = state.job.copy(model = it)
            }
            Field("City", state.job.city) {
                state.job = state.job.copy(city = it)
            }
            Field("Technician", state.job.technician) {
                state.job = state.job.copy(technician = it)
            }
            Field("Complaint", state.job.complaint) {
                state.job = state.job.copy(complaint = it)
            }
        }

        SectionLabel(if (showFull) "Full report" else "Short message")

        Box(
            Modifier
                .fillMaxWidth()
                .background(Vlk.Surface, RoundedCornerShape(12.dp))
                .border(1.dp, Vlk.Line, RoundedCornerShape(12.dp))
                .padding(13.dp)
        ) {
            Text(
                text = if (showFull) fullReport else shortMessage,
                color = Vlk.Text,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp,
                lineHeight = 15.sp,
                modifier = if (showFull)
                    Modifier.horizontalScroll(rememberScrollState())
                else Modifier
            )
        }

        OutlinedButton(
            onClick = { showFull = !showFull },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(11.dp)
        ) {
            Text(
                if (showFull) "Show short message" else "Show full report",
                color = Vlk.Text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }

        Button(
            onClick = {
                val body = if (showFull) fullReport else shortMessage
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "VLK Battery Doctor — ${diagnosis.worst.verdict}")
                    putExtra(Intent.EXTRA_TEXT, body)
                }
                context.startActivity(Intent.createChooser(send, "Send report"))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(13.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Vlk.Accent,
                contentColor = Color.White
            )
        ) {
            Text("Send report", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    keyboard: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 12.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = Vlk.Text
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Vlk.Accent,
            unfocusedBorderColor = Vlk.Line,
            focusedLabelColor = Vlk.Accent,
            unfocusedLabelColor = Vlk.TextDim,
            cursorColor = Vlk.Accent,
            focusedTextColor = Vlk.Text,
            unfocusedTextColor = Vlk.Text,
            focusedContainerColor = Vlk.SurfaceHi,
            unfocusedContainerColor = Vlk.SurfaceHi
        )
    )
}
