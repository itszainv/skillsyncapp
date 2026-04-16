package com.example.skillsync.models

import androidx.annotation.DrawableRes

data class StudentFeedCourse( //Represents a course in the student feed.
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

data class StudentFeedSubject( //Represents a subject in the student feed.
    val subjectId: String,
    val subjectName: String,
    val courses: List<StudentFeedCourse>,
    val completionPercent: Int = 0,
    val completedCourses: Int = 0,
    val totalCourses: Int = 0
)



data class StudentFeedLesson( //Represents a lesson in the student feed.
    val lessonId: String,
    val lessonTitle: String,
    val lessonOrder: Int,
    val quiz: StudentQuiz,
    val videoUrl: String, // Changed from Int to String for URL-based playback
    @DrawableRes val thumbnailResId: Int,
    val isSaved: Boolean = false,
    val isCompleted: Boolean = false
)


data class StudentQuiz( // Represents a quiz associated with a lesson.
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
