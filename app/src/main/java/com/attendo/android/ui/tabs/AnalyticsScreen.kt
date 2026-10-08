package com.attendo.android.ui.tabs

import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.attendo.android.ui.analytics.AnalyticsViewModel
import com.attendo.android.ui.analytics.StudentStats

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AnalyticsScreen(
    activeWorkspace: String?,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val exportExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri: Uri? -> uri?.let { viewModel.exportToExcel(it) } }
    )
    
    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri: Uri? -> uri?.let { viewModel.exportToPdf(context, it) } }
    )

    LaunchedEffect(activeWorkspace) {
        if (activeWorkspace != null) {
            viewModel.loadAnalytics(activeWorkspace)
        }
    }

    LaunchedEffect(uiState.exportResult) {
        if (uiState.exportResult != null) {
            viewModel.clearExportResult()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .background(Color(0xFF1E293B), RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Workspace Analytics", color = Color(0xFF14B8A6), fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Progress bar for weeks
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (i in 0 until uiState.totalWeeks) {
                                val color = if (i <= uiState.currentWeekIndex) Color(0xFF14B8A6) else Color(0xFF334155)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .background(color, RoundedCornerShape(3.dp))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        val pct = if (uiState.totalWeeks > 0) ((uiState.currentWeekIndex + 1).toFloat() / uiState.totalWeeks.toFloat() * 100).toInt() else 0
                        Text("${pct.coerceAtMost(100)}%", color = Color(0xFF14B8A6), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val safeWs = (activeWorkspace ?: "Workspace").replace("[", "").replace("]", "")
                    Button(
                        onClick = { exportPdfLauncher.launch("Attendo_MasterReport_$safeWs.pdf") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Export PDF", fontWeight = FontWeight.Bold) }

                    Button(
                        onClick = { exportExcelLauncher.launch("Attendo_MasterReport_$safeWs.csv") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Export Excel", fontWeight = FontWeight.Bold) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Charts Pager
            val pagerState = rememberPagerState(pageCount = { 2 })
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) { page ->
                when (page) {
                    0 -> LineChartCard(uiState.turnoutTrend)
                    1 -> DonutChartCard(uiState.totalSafe, uiState.totalAtRisk, uiState.avgTurnout, uiState.totalWeeks)
                }
            }
            
            // Pager Indicator
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
                for (i in 0 until 2) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (pagerState.currentPage == i) 8.dp else 6.dp)
                            .background(if (pagerState.currentPage == i) Color(0xFF14B8A6) else Color.Gray, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Control Bar
            var typeExpanded by remember { mutableStateOf(false) }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    placeholder = { Text("Search Student by Name or ID...", color = Color.Gray, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Box(modifier = Modifier.height(50.dp).background(Color(0xFF0F172A), RoundedCornerShape(12.dp))) {
                    Row(
                        modifier = Modifier.fillMaxHeight().clickable { typeExpanded = true }.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TYPE: ", color = Color.Gray, fontSize = 10.sp)
                        Text(uiState.sessionTypeFilter, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        listOf("All Sessions", "Lecture", "Section").forEach { type ->
                            DropdownMenuItem(text = { Text(type) }, onClick = { viewModel.updateSessionTypeFilter(type); typeExpanded = false })
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.height(50.dp).background(Color(0xFF0F172A), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("THRESHOLD: ", color = Color.Gray, fontSize = 10.sp)
                    IconButton(onClick = { viewModel.updateThreshold(uiState.threshold - 1) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Text("${uiState.threshold}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                    IconButton(onClick = { viewModel.updateThreshold(uiState.threshold + 1) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))

            // List Header
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("STUDENT", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2.5f))
                Text("RECENT TREND", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), textAlign = TextAlign.Center)
                Text("ATTENDED", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("ABSENT", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("STATUS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.stats, key = { it.nationalId }) { stat ->
                    StudentAnalyticsCard(stat)
                }
            }
        }
    }
}

@Composable
fun LineChartCard(trend: List<com.attendo.android.ui.analytics.SessionTurnout>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("SESSION TURNOUT TREND", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (trend.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No data available", color = Color.Gray)
                }
                return@Column
            }
            
            Canvas(modifier = Modifier.fillMaxSize()) {
                val maxTurnout = trend.maxOfOrNull { it.turnout }?.coerceAtLeast(10) ?: 10
                
                val width = size.width
                val height = size.height
                
                val stepX = if (trend.size > 1) width / (trend.size - 1) else width
                
                val path = Path()
                val points = mutableListOf<Offset>()
                
                trend.forEachIndexed { index, session ->
                    val x = index * stepX
                    val y = height - (session.turnout.toFloat() / maxTurnout * height)
                    points.add(Offset(x, y))
                    
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                
                // Draw gradient fill
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF14B8A6).copy(alpha = 0.3f), Color.Transparent)
                    )
                )
                
                // Draw line
                drawPath(
                    path = path,
                    color = Color(0xFF14B8A6),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
                
                // Draw labels (basic)
                val textPaint = Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 24f
                }
                
                if (trend.isNotEmpty()) {
                    drawContext.canvas.nativeCanvas.drawText(trend.first().label, 0f, height + 30f, textPaint)
                    if (trend.size > 1) {
                        drawContext.canvas.nativeCanvas.drawText(trend.last().label, width - 40f, height + 30f, textPaint)
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChartCard(safeCount: Int, atRiskCount: Int, avgTurnout: Int, totalWeeks: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("CLASS STATUS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Donut
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    val total = safeCount + atRiskCount
                    val safePct = if (total > 0) (safeCount.toFloat() / total * 100) else 0f
                    
                    Canvas(modifier = Modifier.size(100.dp)) {
                        val strokeWidth = 12.dp.toPx()
                        
                        // Background circle
                        drawCircle(
                            color = Color(0xFF1E293B),
                            radius = (size.minDimension / 2) - (strokeWidth / 2),
                            style = Stroke(width = strokeWidth)
                        )
                        
                        if (total > 0) {
                            // Safe Arc
                            val safeSweep = (safeCount.toFloat() / total) * 360f
                            drawArc(
                                color = Color(0xFF14B8A6),
                                startAngle = -90f,
                                sweepAngle = safeSweep,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            
                            // At Risk Arc
                            drawArc(
                                color = Color(0xFFF43F5E),
                                startAngle = -90f + safeSweep,
                                sweepAngle = 360f - safeSweep,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${String.format("%.1f", safePct)}%", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text("SAFE", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                // Legend
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF14B8A6), CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("SAFE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("$safeCount", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFFF43F5E), CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("AT RISK", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("$atRiskCount", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
            
            // Bottom Stats
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("AVG TURNOUT", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("$avgTurnout", color = Color(0xFF14B8A6), fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOTAL WEEKS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("$totalWeeks", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun StudentAnalyticsCard(stat: StudentStats) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(12.dp),
        border = if (stat.isAtRisk) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF991B1B).copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Student
            Column(modifier = Modifier.weight(2.5f)) {
                Text(stat.studentName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            
            // Recent Trend
            Row(modifier = Modifier.weight(1.5f), horizontalArrangement = Arrangement.Center) {
                stat.recentTrend.forEach { attended ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(8.dp)
                            .background(if (attended) Color(0xFF10B981) else Color(0xFFEF4444), CircleShape)
                    )
                }
                if (stat.recentTrend.isEmpty()) {
                    Text("-", color = Color.Gray)
                }
            }
            
            // Attended
            Text(
                text = "${stat.attendedCount}", 
                color = Color(0xFF14B8A6), 
                fontSize = 14.sp, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f), 
                textAlign = TextAlign.Center
            )
            
            // Absent
            Text(
                text = "${stat.absentCount}", 
                color = Color(0xFFEF4444), 
                fontSize = 14.sp, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f), 
                textAlign = TextAlign.Center
            )
            
            // Status
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                val (bgColor, textColor, label) = if (stat.isAtRisk) {
                    Triple(Color(0xFF991B1B).copy(alpha = 0.2f), Color(0xFFEF4444), "AT RISK")
                } else {
                    Triple(Color.Transparent, Color.Gray, "SAFE")
                }
                
                Box(
                    modifier = Modifier
                        .background(bgColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = label, color = textColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
        }
    }
}
