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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.skillsync.models.Course
import com.example.skillsync.models.Subject
import kotlinx.coroutines.launch

@Composable
fun AdminCourseListScreen(
    onAddCourse: (String) -> Unit,
    onEditCourse: (String, Course) -> Unit,
    onOpenLessons: (String, String) -> Unit,
    onLogout: () -> Unit
) {
    val repo = remember { FirestoreRepository() }
    val scope = rememberCoroutineScope()
    
    var subjects by remember { mutableStateOf<List<Subject>>(emptyList()) }
    var coursesMap by remember { mutableStateOf<Map<String, List<Course>>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var newSubjectName by remember { mutableStateOf("") }

    val refreshData = {
        scope.launch {
            loading = true
            val fetchedSubjects = repo.getSubjects()
            subjects = fetchedSubjects
            val map = mutableMapOf<String, List<Course>>()
            fetchedSubjects.forEach { subject ->
                map[subject.id] = repo.getCourses(subject.id)
            }
            coursesMap = map
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    if (showAddSubjectDialog) {
        AlertDialog(
            onDismissRequest = { showAddSubjectDialog = false },
            title = { Text("New Subject") },
            text = {
                OutlinedTextField(
                    value = newSubjectName,
                    onValueChange = { newSubjectName = it },
                    label = { Text("Subject Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repo.saveSubject(Subject(name = newSubjectName))
                        showAddSubjectDialog = false
                        newSubjectName = ""
                        refreshData()
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubjectDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Admin Dashboard", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
            IconButton(onClick = onLogout) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = Color.White)
            }
        }

        Button(
            onClick = { showAddSubjectDialog = true },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.padding(4.dp))
            Text("Add New Subject")
        }

        Spacer(Modifier.height(16.dp))

        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = Color.White)
        } else if (subjects.isEmpty()) {
            Text("No subjects found. Add one to get started!", color = Color.White, modifier = Modifier.padding(top = 20.dp))
        } else {
            subjects.forEach { subject ->
                Text(subject.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))

                val subjectCourses = coursesMap[subject.id] ?: emptyList()
                subjectCourses.forEach { course ->
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
                                Text(course.title, fontWeight = FontWeight.Medium, color = Color.Black)
                                Text(course.description, fontSize = 12.sp, color = Color.Black.copy(alpha = 0.7f), maxLines = 1)
                            }
                            IconButton(onClick = { onOpenLessons(subject.id, course.id) }) {
                                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Lessons", tint = Color.Black)
                            }
                            IconButton(onClick = { onEditCourse(subject.id, course) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Black)
                            }
                        }
                    }
                }

                Button(
                    onClick = { onAddCourse(subject.id) },
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text("Add Course to ${subject.name}")
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.2f))
            }
        }
    }
}
