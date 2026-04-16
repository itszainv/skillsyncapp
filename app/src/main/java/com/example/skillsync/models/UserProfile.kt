package com.example.skillsync.models
import android.util.Log // Needed to see why multiple_choice questions were being read as NULL

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val xp: Int = 0,
    val level: Int = 1,
    val currentStreak: Int = 0,
    val highestStreak: Int = 0,
    val watchLaterCount: Int = 0,
    val dailyXp: Int = 0,
    val skillBux: Int = 0,
    val selectedTheme: String = "dark_red",
    val purchasedThemes: List<String> = listOf("dark_red"),
    val selectedAvatar: String = "🙂",
    val purchasedAvatars: List<String> = listOf("🙂"),
    val selectedNameIcon: String = "",
    val purchasedNameIcons: List<String> = emptyList()
)

data class LeaderboardEntry(
    val name: String = "",
    val value: Int = 0
)

data class LeaderboardData(
    val topStreakUsers: List<LeaderboardEntry> = emptyList(),
    val topLevelUsers: List<LeaderboardEntry> = emptyList(),
    val topDailyXpUsers: List<LeaderboardEntry> = emptyList()
)

data class ShopItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val cost: Int,
    val owned: Boolean,
    val applied: Boolean
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
    val videoUrl: String = "", // Caches the remote URL for network streaming
    val thumbnailResId: Int = 0,
    val savedAt: Long = 0L
) {
    fun toStudentFeedLesson(): StudentFeedLesson {
        Log.d("QUIZ_DEBUG", "quizType from Firebase = $quizType")
        val parsedQuizType = runCatching { StudentQuizType.valueOf(quizType) }
            .getOrDefault(StudentQuizType.INFO)

        return StudentFeedLesson(
            // basic lesson info
            lessonId = lessonId,
            lessonTitle = lessonTitle,
            lessonOrder = lessonOrder,

            // creates the quiz object for this lesson
            quiz = StudentQuiz(
                question = question,
                options = options,
                correctAnswerIndex = correctAnswerIndex,
                explanation = explanation,
                type = parsedQuizType
            ),

            // video link pulled from Firebase
            videoUrl = videoUrl,

            // thumbnail for UI (still using local resource for now)
            thumbnailResId = thumbnailResId,

            // defaulting to saved since this is coming from saved lessons
            isSaved = true
        )
    }
}