/**********************************************************
 * Created by: Asiyah Shoeb
 * Date: 4/16/26
 * Description: Main container and navigation controller for the admin section of the app.
 * Last Modified by: Asiyah Shoeb
 **********************************************************/

package com.example.skillsync.admin

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.skillsync.models.Course
import com.example.skillsync.models.Lesson

@Composable
fun AdminDashboard(onLogout: () -> Unit) {

    var screen by remember { mutableStateOf("course_list") }

    // Context we carry forward as the admin drills down
    var selectedSubjectId by remember { mutableStateOf("") }
    var selectedCourseId by remember { mutableStateOf("") }
    var selectedLesson by remember { mutableStateOf<Lesson?>(null) }
    var selectedComponentIndex by remember { mutableStateOf<Int?>(null) }
    var editingCourse by remember { mutableStateOf<Course?>(null) }
    var editingLesson by remember { mutableStateOf<Lesson?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black // Force black background for all admin screens
    ) {
        Box(modifier = Modifier.padding(top = 20.dp)) {
            when (screen) {

                // ── Top level: pick a subject + see its courses ──────────
                "course_list" -> AdminCourseListScreen(
                    onAddCourse = { subjectId ->
                        selectedSubjectId = subjectId
                        editingCourse = null
                        screen = "course_edit"
                    },
                    onEditCourse = { subjectId, course ->
                        selectedSubjectId = subjectId
                        editingCourse = course
                        screen = "course_edit"
                    },
                    onOpenLessons = { subjectId, courseId ->
                        selectedSubjectId = subjectId
                        selectedCourseId = courseId
                        screen = "lesson_list"
                    },
                    onLogout = onLogout
                )

                // ── Create / edit a course ────────────────────────────────
                "course_edit" -> AdminCourseEditScreen(
                    subjectId = selectedSubjectId,
                    course = editingCourse,
                    onSaved = { screen = "course_list" },
                    onBack = { screen = "course_list" }
                )

                // ── List lessons inside a course ──────────────────────────
                "lesson_list" -> LessonListScreen(
                    subjectId = selectedSubjectId,
                    courseId = selectedCourseId,
                    onEditLesson = { lesson ->
                        editingLesson = lesson
                        screen = "lesson_edit"
                    },
                    onOpenComponents = { lesson ->
                        selectedLesson = lesson
                        screen = "component_list"
                    },
                    onBack = { screen = "course_list" }
                )

                // ── Create / edit a lesson ────────────────────────────────
                "lesson_edit" -> LessonEditScreen(
                    subjectId = selectedSubjectId,
                    courseId = selectedCourseId,
                    lesson = editingLesson,
                    onSaved = { screen = "lesson_list" },
                    onBack = { screen = "lesson_list" }
                )

                // ── List components (questions) inside a lesson ───────────
                "component_list" -> selectedLesson?.let { lesson ->
                    ComponentListScreen(
                        subjectId = selectedSubjectId,
                        courseId = selectedCourseId,
                        lesson = lesson,
                        onEditComponent = { index ->
                            selectedComponentIndex = index
                            screen = "component_edit"
                        },
                        onBack = { screen = "lesson_list" }
                    )
                }

                // ── Create / edit a single question ──────────────────────
                "component_edit" -> selectedLesson?.let { lesson ->
                    ComponentEditScreen(
                        subjectId = selectedSubjectId,
                        courseId = selectedCourseId,
                        lesson = lesson,
                        componentIndex = selectedComponentIndex,
                        onSaved = { screen = "component_list" },
                        onBack = { screen = "component_list" }
                    )
                }
            }
        }
    }
}
