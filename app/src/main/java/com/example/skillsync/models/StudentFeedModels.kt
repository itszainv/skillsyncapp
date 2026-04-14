package com.example.skillsync.models

import androidx.annotation.DrawableRes

data class StudentFeedSubject(
    val subjectId: String,
    val subjectName: String,
    val courses: List<StudentFeedCourse>,
    val completionPercent: Int = 0,
    val completedCourses: Int = 0,
    val totalCourses: Int = 0
)

data class StudentFeedCourse(
    val subjectId: String,
    val subjectName: String,
    val courseId: String,
    val courseTitle: String,
    val courseDescription: String,
    val lessons: List<StudentFeedLesson>,
    val completionPercent: Int = 0,
    val completedLessons: Int = 0,
    val totalLessons: Int = 0
)

data class StudentFeedLesson(
    val lessonId: String,
    val lessonTitle: String,
    val lessonOrder: Int,
    val quiz: StudentQuiz,
    @DrawableRes val videoResId: Int,
    @DrawableRes val thumbnailResId: Int,
    val isSaved: Boolean = false,
    val isCompleted: Boolean = false
)

data class StudentQuiz(
    val question: String,
    val options: List<String> = emptyList(),
    val correctAnswerIndex: Int? = null,
    val explanation: String = "",
    val type: StudentQuizType = StudentQuizType.INFO
)

enum class StudentQuizType {
    INFO,
    MULTIPLE_CHOICE,
    TRUE_FALSE
}