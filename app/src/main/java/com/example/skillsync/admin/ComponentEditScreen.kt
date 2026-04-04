package com.example.skillsync.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skillsync.data.FirestoreRepository
import com.example.skillsync.models.ComponentType
import com.example.skillsync.models.Lesson
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentEditScreen(
    subjectId: String,
    courseId: String,
    lesson: Lesson,
    componentIndex: Int? = null,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val repo = remember { FirestoreRepository() }
    val scope = rememberCoroutineScope()
    val existing = componentIndex?.let { lesson.components[it] }

    var selectedType by remember {
        mutableStateOf(
            when (existing?.get("type")) {
                "true_false" -> ComponentType.TRUE_FALSE
                "fill_in_blank" -> ComponentType.FILL_IN_BLANK
                else -> ComponentType.MULTIPLE_CHOICE
            }
        )
    }
    var question by remember { mutableStateOf(existing?.get("question")?.toString() ?: "") }
    var explanation by remember { mutableStateOf(existing?.get("explanation")?.toString() ?: "") }

    // Multiple choice
    val existingOptions = existing?.get("options") as? List<*>
    var optionA by remember { mutableStateOf(existingOptions?.getOrNull(0)?.toString() ?: "") }
    var optionB by remember { mutableStateOf(existingOptions?.getOrNull(1)?.toString() ?: "") }
    var optionC by remember { mutableStateOf(existingOptions?.getOrNull(2)?.toString() ?: "") }
    var optionD by remember { mutableStateOf(existingOptions?.getOrNull(3)?.toString() ?: "") }
    var correctIndex by remember { mutableStateOf((existing?.get("correct") as? Long)?.toInt() ?: 0) }

    // True / False
    var correctBool by remember { mutableStateOf(existing?.get("answer") as? Boolean ?: true) }

    // Fill in blank
    var answer by remember { mutableStateOf(existing?.get("answer")?.toString() ?: "") }
    var hint by remember { mutableStateOf(existing?.get("hint")?.toString() ?: "") }

    var saving by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                if (componentIndex == null) "New question" else "Edit question",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(20.dp))

        // Type picker
        ExposedDropdownMenuBox(
            expanded = typeExpanded,
            onExpandedChange = { typeExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedType.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Question type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = typeExpanded,
                onDismissRequest = { typeExpanded = false }
            ) {
                ComponentType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.label) },
                        onClick = { selectedType = type; typeExpanded = false }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Question field (all types)
        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            label = { Text("Question") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Spacer(Modifier.height(16.dp))

        // Type-specific fields
        when (selectedType) {
            ComponentType.MULTIPLE_CHOICE -> {
                Text("Options", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))

                listOf(
                    "A" to optionA,
                    "B" to optionB,
                    "C" to optionC,
                    "D" to optionD
                ).forEachIndexed { index, (label, value) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = correctIndex == index,
                            onClick = { correctIndex = index }
                        )
                        OutlinedTextField(
                            value = value,
                            onValueChange = {
                                when (index) {
                                    0 -> optionA = it
                                    1 -> optionB = it
                                    2 -> optionC = it
                                    3 -> optionD = it
                                }
                            },
                            label = { Text("Option $label") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Text(
                    "Select the radio button next to the correct answer",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ComponentType.TRUE_FALSE -> {
                Text("Correct answer", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = correctBool,
                        onClick = { correctBool = true }
                    )
                    Text("True", modifier = Modifier.padding(end = 24.dp))
                    RadioButton(
                        selected = !correctBool,
                        onClick = { correctBool = false }
                    )
                    Text("False")
                }
            }

            ComponentType.FILL_IN_BLANK -> {
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Accepted answer") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text("Hint (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Explanation (all types)
        OutlinedTextField(
            value = explanation,
            onValueChange = { explanation = it },
            label = { Text("Explanation (shown after answer)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                saving = true
                scope.launch {
                    val component: Map<String, Any> = when (selectedType) {
                        ComponentType.MULTIPLE_CHOICE -> mapOf(
                            "type" to "multiple_choice",
                            "question" to question,
                            "options" to listOf(optionA, optionB, optionC, optionD),
                            "correct" to correctIndex,
                            "explanation" to explanation
                        )
                        ComponentType.TRUE_FALSE -> mapOf(
                            "type" to "true_false",
                            "question" to question,
                            "answer" to correctBool,
                            "explanation" to explanation
                        )
                        ComponentType.FILL_IN_BLANK -> mapOf(
                            "type" to "fill_in_blank",
                            "question" to question,
                            "answer" to answer,
                            "hint" to hint,
                            "explanation" to explanation
                        )
                    }

                    val updatedComponents = lesson.components.toMutableList()
                    if (componentIndex != null) {
                        updatedComponents[componentIndex] = component
                    } else {
                        updatedComponents.add(component)
                    }

                    repo.saveLesson(
                        subjectId, courseId,
                        lesson.copy(components = updatedComponents)
                    )
                    onSaved()
                }
            },
            enabled = question.isNotBlank() && !saving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (saving) "Saving..." else "Save question")
        }
    }
}
