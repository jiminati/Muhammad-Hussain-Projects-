package com.vlektra.batterydoctor.ui

import android.content.Intent
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.vlektra.batterydoctor.location.CityFinder
import com.vlektra.batterydoctor.model.BikeModels
import com.vlektra.batterydoctor.model.RepairStatus
import com.vlektra.batterydoctor.report.ReportBuilder
import com.vlektra.batterydoctor.ui.theme.Eyebrow
import com.vlektra.batterydoctor.ui.theme.Vlk
import kotlinx.coroutines.launch

@Composable
fun ReportScreen(state: AppState) {
    val snapshot = state.snapshot
    val diagnosis = state.diagnosis
    if (snapshot == null || diagnosis == null) {
        EmptyState("Nothing to report", "Diagnose a battery first.")
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showFull by remember { mutableStateOf(false) }
    var cityNote by remember { mutableStateOf<String?>(null) }
    var locating by remember { mutableStateOf(false) }

    val shortMessage = ReportBuilder.whatsAppMessage(state.job, snapshot, diagnosis)
    val fullReport = ReportBuilder.fullReport(
        state.job, snapshot, state.history, state.parameters, diagnosis
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Spacer(Modifier.height(6.dp))

        Column {
            Text("Report", style = MaterialTheme.typography.displaySmall, color = Vlk.Text)
            Spacer(Modifier.height(4.dp))
            Text(
                "Fill in the job, pick where the battery goes, then send.",
                color = Vlk.TextDim,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // ---------------- status, first, because it is the decision ----------------
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("WHERE DOES THIS BATTERY GO", style = Eyebrow, color = Vlk.TextFaint)
            RepairStatus.ordered.forEach { s ->
                StatusChoice(
                    status = s,
                    selected = state.job.status == s,
                    onPick = { state.job = state.job.copy(status = s) }
                )
            }
        }

        // ---------------- the job card ----------------
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("JOB CARD", style = Eyebrow, color = Vlk.TextFaint)

            // City with a detect button
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f)) {
                    Field("City", state.job.city, KeyboardType.Text) {
                        state.job = state.job.copy(city = it)
                    }
                }
                Button(
                    onClick = {
                        locating = true
                        cityNote = null
                        scope.launch {
                            when (val r = CityFinder.detect(context)) {
                                is CityFinder.Result.Found -> {
                                    state.job = state.job.copy(city = r.city)
                                    cityNote = null
                                }
                                CityFinder.Result.NoPermission ->
                                    cityNote = "Location permission is off. Type the city."
                                CityFinder.Result.NoFix ->
                                    cityNote = "No location fix yet. Type the city."
                                CityFinder.Result.Unavailable ->
                                    cityNote = "Could not look up the city. Type it."
                            }
                            locating = false
                        }
                    },
                    enabled = !locating,
                    modifier = Modifier.height(54.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Vlk.SurfaceHi,
                        contentColor = Vlk.Accent,
                        disabledContainerColor = Vlk.SurfaceHi,
                        disabledContentColor = Vlk.TextFaint
                    )
                ) {
                    Text(
                        if (locating) "…" else "Detect",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
            cityNote?.let {
                Text(it, color = Vlk.Watch, style = MaterialTheme.typography.bodySmall)
            }

            // Bike model as chips, so nobody mistypes a model name
            Text("BIKE MODEL", style = Eyebrow, color = Vlk.TextFaint)
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                BikeModels.all.forEach { m ->
                    ModelChip(
                        label = m,
                        selected = state.job.model == m,
                        onPick = {
                            state.job = state.job.copy(
                                model = if (state.job.model == m) "" else m
                            )
                        }
                    )
                }
            }

            Field("Customer name", state.job.customerName) {
                state.job = state.job.copy(customerName = it)
            }
            Field("Customer phone", state.job.customerPhone, KeyboardType.Phone) {
                state.job = state.job.copy(customerPhone = it)
            }
            Field("Chassis number", state.job.chassisNumber) {
                state.job = state.job.copy(chassisNumber = it)
            }
            Field("Battery ID", state.job.batteryId) {
                state.job = state.job.copy(batteryId = it)
            }
            Field("Technician name", state.job.technician) {
                state.job = state.job.copy(technician = it)
            }
            Field("Complaint", state.job.complaint) {
                state.job = state.job.copy(complaint = it)
            }
        }

        // ---------------- preview ----------------
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (showFull) "FULL REPORT" else "MESSAGE",
                style = Eyebrow,
                color = Vlk.TextFaint
            )
            Text(
                if (showFull) "Show message" else "Show full report",
                color = Vlk.Accent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                modifier = Modifier
                    .clickable { showFull = !showFull }
                    .padding(4.dp)
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .background(Vlk.LineSoft, RoundedCornerShape(12.dp))
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
                    Modifier.horizontalScroll(rememberScrollState()) else Modifier
            )
        }

        Button(
            onClick = {
                val body = if (showFull) fullReport else shortMessage
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        "VLK Battery Doctor — ${diagnosis.worst.verdict}"
                    )
                    putExtra(Intent.EXTRA_TEXT, body)
                }
                context.startActivity(Intent.createChooser(send, "Send report"))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Vlk.Accent,
                contentColor = Color.White
            )
        ) {
            Text("Send report", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

// ----------------------------------------------------------------------

@Composable
private fun StatusChoice(
    status: RepairStatus,
    selected: Boolean,
    onPick: () -> Unit
) {
    val tint = when (status) {
        RepairStatus.WORKING -> Vlk.Ok
        RepairStatus.SENT_TO_PLANT -> Vlk.Engineer
        RepairStatus.REPAIRING_AT_CENTRE -> Vlk.Shop
    }
    val grow by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(220),
        label = "statusPick"
    )

    Row(
        Modifier
            .fillMaxWidth()
            .background(
                if (selected) tint.copy(alpha = 0.10f) else Vlk.Surface,
                RoundedCornerShape(13.dp)
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) tint else Vlk.Line,
                shape = RoundedCornerShape(13.dp)
            )
            .clickable(onClick = onPick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(18.dp)
                .border(
                    1.6.dp,
                    if (selected) tint else Vlk.Idle,
                    RoundedCornerShape(999.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size((9 * grow).dp)
                    .background(tint, RoundedCornerShape(999.dp))
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                status.label,
                color = if (selected) Vlk.Text else Vlk.TextDim,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.5.sp
            )
            Text(
                status.urdu,
                color = if (selected) tint else Vlk.TextFaint,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ModelChip(label: String, selected: Boolean, onPick: () -> Unit) {
    Box(
        Modifier
            .background(
                if (selected) Vlk.Accent else Vlk.SurfaceHi,
                RoundedCornerShape(999.dp)
            )
            .border(
                1.dp,
                if (selected) Vlk.Accent else Vlk.Line,
                RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onPick)
            .padding(horizontal = 15.dp, vertical = 9.dp)
    ) {
        Text(
            label,
            color = if (selected) Color.White else Vlk.TextDim,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp
        )
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
            unfocusedLabelColor = Vlk.TextFaint,
            cursorColor = Vlk.Accent,
            focusedTextColor = Vlk.Text,
            unfocusedTextColor = Vlk.Text,
            focusedContainerColor = Vlk.Surface,
            unfocusedContainerColor = Vlk.Surface
        )
    )
}
