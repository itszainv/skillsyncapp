package com.example.skillsync.models

data class Subject(
    val id: String = "",
    val name: String = "",
    val icon: String = ""
)

data class Course(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val order: Int = 0
)

/*
videoUrl stores a network address which is streamed directly by the VideoView in the dashboard.
*/
data class Lesson(
    val id: String = "",
    val title: String = "",
    val order: Int = 0,
    val videoUrl: String = "",
    // URL for the lesson's preview thumbnail image, can be a web link or Firebase Storage URL
    val thumbnailUrl: String = "",
    val components: List<Map<String, Any>> = emptyList()
)

enum class ComponentType(val label: String) {
    MULTIPLE_CHOICE("Multiple choice"),
    TRUE_FALSE("True / False"),
    FILL_IN_BLANK("Fill in the blank")
}
