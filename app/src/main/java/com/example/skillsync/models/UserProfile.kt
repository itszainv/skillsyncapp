package com.example.skillsync.models

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val xp: Int = 0,
    val level: Int = 1,
    val currentStreak: Int = 0,
    val highestStreak: Int = 0,
    val watchLaterCount: Int = 0
)

data class WatchLaterLesson(
    val lessonId: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val courseId: String = "",
    val courseTitle: String = "",
    val courseDescription: String = "",
    val lessonTitle: String = "",
    val lessonOrder: Int = 0,
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctAnswerIndex: Int? = null,
    val explanation: String = "",
    val quizType: String = StudentQuizType.INFO.name,
    val videoUrl: String = "", // Updated to String
    val thumbnailResId: Int = 0,
    val savedAt: Long = 0L
) {
    fun toStudentFeedLesson(): StudentFeedLesson {
        val parsedQuizType = runCatching { StudentQuizType.valueOf(quizType) }
            .getOrDefault(StudentQuizType.INFO)

        return StudentFeedLesson(
            lessonId = lessonId,
            lessonTitle = lessonTitle,
            lessonOrder = lessonOrder,
            quiz = StudentQuiz(
                question = question,
                options = options,
                correctAnswerIndex = correctAnswerIndex,
                explanation = explanation,
                type = parsedQuizType
            ),
            videoUrl = videoUrl,
            thumbnailResId = thumbnailResId,
            isSaved = true
        )
    }
}
