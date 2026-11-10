package com.vlektra.batterydoctor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vlektra.batterydoctor.AppState
import com.vlektra.batterydoctor.Screen
import com.vlektra.batterydoctor.diagnosis.Finding
import com.vlektra.batterydoctor.ui.theme.Vlk

@Composable
fun DiagnosisScreen(state: AppState) {
    val d = state.diagnosis
    if (d == null) {
        EmptyState("Nothing diagnosed yet", "Connect to a pack and read it first.")
        return
    }

    val (fg, bg) = severityColours(d.worst)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        // The verdict — the one thing a technician must read.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg, RoundedCornerShape(16.dp))
                .border(1.5.dp, fg, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                d.worst.badge,
                color = fg,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp
            )
            Text(
                d.worst.verdict,
                color = Vlk.Text,
                style = MaterialTheme.typography.displaySmall
            )
            Text(
                d.worst.verdictUrdu,
                color = fg,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                d.worst.subtitle,
                color = Vlk.Text.copy(alpha = 0.78f),
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Context switch that changes the verdict
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Vlk.Surface, RoundedCornerShape(12.dp))
                .border(1.dp, Vlk.Line, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = state.rangeComplaint,
                onCheckedChange = {
                    state.rangeComplaint = it
                    state.recompute()
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = Vlk.Accent,
                    uncheckedColor = Vlk.TextDim,
                    checkmarkColor = Color.White
                )
            )
            Text(
                "Customer complaint is low range",
                color = Vlk.Text,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        SectionLabel("What the data shows")
        d.findings.forEach { FindingCard(it) }

        SectionLabel("What to do")
        Card {
            val acts = d.actionable.ifEmpty { d.findings }
            acts.forEachIndexed { i, f ->
                ActionRow(i + 1, f)
                if (i != acts.lastIndex) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Vlk.Line)
                    )
                }
            }
        }

        Button(
            onClick = { state.go(Screen.REPORT) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(13.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Vlk.Accent,
                contentColor = Color.White
            )
        ) {
            Text("Make the report", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun FindingCard(f: Finding) {
    val (fg, _) = severityColours(f.severity)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Vlk.Surface, RoundedCornerShape(13.dp))
            .border(1.dp, Vlk.Line, RoundedCornerShape(13.dp))
            .height(IntrinsicSize.Min)
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(fg, RoundedCornerShape(topStart = 13.dp, bottomStart = 13.dp))
        )
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    f.title,
                    color = Vlk.Text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Pill(f.severity.badge, fg, Vlk.SurfaceHi)
            }
            Text(
                f.evidence,
                color = Vlk.TextDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
            Text(
                f.reasoning,
                color = Vlk.Text.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ActionRow(number: Int, f: Finding) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Box(
            Modifier
                .size(23.dp)
                .background(Vlk.SurfaceHi, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$number",
                color = Vlk.TextDim,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                f.actionUrdu,
                color = Vlk.Text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                f.actionEnglish,
                color = Vlk.TextDim,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
