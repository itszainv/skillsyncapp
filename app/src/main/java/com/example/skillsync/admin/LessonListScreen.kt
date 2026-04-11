package com.example.skillsync.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skillsync.data.FirestoreRepository
import com.example.skillsync.models.Lesson
import kotlinx.coroutines.launch

@Composable
fun LessonListScreen(
    subjectId: String,
    courseId: String,
    onEditLesson: (Lesson?) -> Unit,
    onOpenComponents: (Lesson) -> Unit,
    onBack: () -> Unit
) {
    val repo = remember { FirestoreRepository() }
    val scope = rememberCoroutineScope()
    var lessons by remember { mutableStateOf<List<Lesson>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        lessons = repo.getLessons(subjectId, courseId)
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, 
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                "Lessons", 
                fontSize = 18.sp, 
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }

        Spacer(Modifier.height(12.dp))

        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = Color.White)
        } else {
            lessons.forEach { lesson ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(lesson.title, fontWeight = FontWeight.Medium, color = Color.Black)
                            Text(
                                "${lesson.components.size} components · order ${lesson.order}",
                                fontSize = 12.sp,
                                color = Color.Black.copy(alpha = 0.7f)
                            )
                        }
                        IconButton(onClick = { onOpenComponents(lesson) }) {
                            Icon(Icons.Default.List, contentDescription = "Components", tint = Color.Black)
                        }
                        IconButton(onClick = { onEditLesson(lesson) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Black)
                        }
                        IconButton(onClick = {
                            scope.launch {
                                repo.deleteLesson(subjectId, courseId, lesson.id)
                                lessons = repo.getLessons(subjectId, courseId)
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Black)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { onEditLesson(null) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Add lesson")
            }
        }
    }
}
