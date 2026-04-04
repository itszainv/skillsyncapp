package com.example.skillsync.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skillsync.data.FirestoreRepository
import com.example.skillsync.models.Lesson
import kotlinx.coroutines.launch

@Composable
fun LessonEditScreen(
    subjectId: String,
    courseId: String,
    lesson: Lesson? = null,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val repo = remember { FirestoreRepository() }
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(lesson?.title ?: "") }
    var order by remember { mutableStateOf(lesson?.order?.toString() ?: "1") }
    var saving by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                if (lesson == null) "New lesson" else "Edit lesson",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Lesson title") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = order,
            onValueChange = { order = it },
            label = { Text("Display order") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                saving = true
                scope.launch {
                    repo.saveLesson(
                        subjectId, courseId,
                        Lesson(
                            id = lesson?.id ?: "",
                            title = title,
                            order = order.toIntOrNull() ?: 1,
                            components = lesson?.components ?: emptyList()
                        )
                    )
                    onSaved()
                }
            },
            enabled = title.isNotBlank() && !saving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (saving) "Saving..." else "Save lesson")
        }
    }
}