/**********************************************************
 * Created by: Asiyah Shoeb
 * Date: 4/16/26
 * Description: Screen displaying the list of quiz components (questions) within a specific lesson.
 * Last Modified by: Asiyah Shoeb
 **********************************************************/

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skillsync.data.FirestoreRepository
import com.example.skillsync.models.Lesson
import kotlinx.coroutines.launch

@Composable
fun ComponentListScreen(
    subjectId: String,
    courseId: String,
    lesson: Lesson,
    onEditComponent: (Int?) -> Unit,
    onBack: () -> Unit
) {
    val repo = remember { FirestoreRepository() }
    val scope = rememberCoroutineScope()
    var components by remember { mutableStateOf(lesson.components.toMutableList()) }

    fun saveComponents() {
        scope.launch {
            repo.saveLesson(subjectId, courseId, lesson.copy(components = components))
        }
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
                lesson.title, 
                fontSize = 18.sp, 
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }

        Spacer(Modifier.height(12.dp))

        components.forEachIndexed { index, component ->
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
                        Text(
                            component["type"].toString()
                                .replace("_", " ")
                                .replaceFirstChar { it.uppercase() },
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = Color.Black
                        )
                        Text(
                            component["question"].toString(),
                            fontSize = 12.sp,
                            color = Color.Black.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { onEditComponent(index) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Black)
                    }
                    IconButton(onClick = {
                        components = components.toMutableList().also { it.removeAt(index) }
                        saveComponents()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Black)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { onEditComponent(null) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ Add question")
        }
    }
}
