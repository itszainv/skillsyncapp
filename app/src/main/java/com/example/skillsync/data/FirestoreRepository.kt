package com.example.skillsync.data

import android.net.Uri
import com.example.skillsync.models.Course
import com.example.skillsync.models.Lesson
import com.example.skillsync.models.StudentFeedCourse
import com.example.skillsync.models.StudentFeedLesson
import com.example.skillsync.models.StudentFeedSubject
import com.example.skillsync.models.StudentQuiz
import com.example.skillsync.models.StudentQuizType
import com.example.skillsync.models.Subject
import com.example.skillsync.models.UserProfile
import com.example.skillsync.models.WatchLaterLesson
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db: FirebaseFirestore = Firebase.firestore

    companion object {
        private const val WATCH_LATER_SUBJECT_ID = "__watch_later__"
        private const val WATCH_LATER_COURSE_ID = "__watch_later_course__"
    }

    private fun currentUserId(): String? = Firebase.auth.currentUser?.uid

    private suspend fun ensureUserProfileDocument() {
        val user = Firebase.auth.currentUser ?: return
        val ref = db.collection("users").document(user.uid)
        val snapshot = ref.get().await()
        if (!snapshot.exists()) {
            ref.set(
                mapOf(
                    "email" to user.email.orEmpty(),
                    "xp" to 0,
                    "currentStreak" to 0,
                    "highestStreak" to 0,
                    "watchLaterCount" to 0,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun getSubjects(): List<Subject> {
        return try {
            val snapshot = db.collection("subjects").get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Subject::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveSubject(subject: Subject): String {
        val ref = if (subject.id.isEmpty())
            db.collection("subjects").document()
        else
            db.collection("subjects").document(subject.id)

        ref.set(subject.copy(id = ref.id)).await()
        return ref.id
    }

    suspend fun getCourses(subjectId: String): List<Course> {
        return try {
            val snapshot = db.collection("subjects/$subjectId/courses")
                .orderBy("order")
                .get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Course::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveCourse(subjectId: String, course: Course): String {
        val ref = if (course.id.isEmpty())
            db.collection("subjects/$subjectId/courses").document()
        else
            db.collection("subjects/$subjectId/courses").document(course.id)

        ref.set(course.copy(id = ref.id)).await()
        return ref.id
    }

    suspend fun deleteCourse(subjectId: String, courseId: String) {
        db.document("subjects/$subjectId/courses/$courseId").delete().await()
    }

    suspend fun getLessons(subjectId: String, courseId: String): List<Lesson> {
        return try {
            val snapshot = db.collection("subjects/$subjectId/courses/$courseId/lessons")
                .orderBy("order")
                .get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Lesson::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveLesson(subjectId: String, courseId: String, lesson: Lesson): String {
        val ref = if (lesson.id.isEmpty())
            db.collection("subjects/$subjectId/courses/$courseId/lessons").document()
        else
            db.collection("subjects/$subjectId/courses/$courseId/lessons").document(lesson.id)

        ref.set(lesson.copy(id = ref.id)).await()
        return ref.id
    }

    suspend fun deleteLesson(subjectId: String, courseId: String, lessonId: String) {
        db.document("subjects/$subjectId/courses/$courseId/lessons/$lessonId").delete().await()
    }

    suspend fun getStudentFeedSubjects(): List<StudentFeedSubject> {
        // Fallback hardcoded videos (now as resource strings)
        val fallbackVideoUrls = listOf(
            "res://raw/video1",
            "res://raw/video2"
        )
        val thumbnailIds = listOf(
            com.example.skillsync.R.drawable.example_cover,
            com.example.skillsync.R.drawable.ic_launcher_foreground
        )

        return try {
            val savedLessons = getWatchLaterLessons()
            val savedLessonIds = savedLessons.map { it.lessonId }.toSet()

            val regularSubjects = getSubjects().mapIndexed { subjectIndex, subject ->
                val courses = getCourses(subject.id).mapIndexed { courseIndex, course ->
                    val lessons = getLessons(subject.id, course.id)
                        .sortedBy { it.order }
                        .mapIndexed { lessonIndex, lesson ->
                            // Check if the lesson has a videoUrl stored in Firestore (future-proofing)
                            // Note: We might need to update the Lesson model to include videoUrl
                            val videoUrlFromDb = lesson.components.firstOrNull()?.get("videoUrl")?.toString()
                            
                            val mediaIndex = if (fallbackVideoUrls.isEmpty()) 0
                            else (subjectIndex + courseIndex + lessonIndex) % fallbackVideoUrls.size

                            StudentFeedLesson(
                                lessonId = lesson.id,
                                lessonTitle = lesson.title.ifBlank { "Lesson ${lessonIndex + 1}" },
                                lessonOrder = lesson.order,
                                quiz = buildStudentQuiz(lesson),
                                videoUrl = videoUrlFromDb ?: fallbackVideoUrls[mediaIndex],
                                thumbnailResId = thumbnailIds[mediaIndex % thumbnailIds.size],
                                isSaved = lesson.id in savedLessonIds
                            )
                        }

                    StudentFeedCourse(
                        subjectId = subject.id,
                        subjectName = subject.name,
                        courseId = course.id,
                        courseTitle = course.title,
                        courseDescription = course.description,
                        lessons = lessons
                    )
                }

                StudentFeedSubject(
                    subjectId = subject.id,
                    subjectName = subject.name,
                    courses = courses
                )
            }

            regularSubjects + buildWatchLaterSubject(savedLessons)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun toggleWatchLater(lesson: StudentFeedLesson, course: StudentFeedCourse): Boolean {
        val userId = currentUserId() ?: return false
        ensureUserProfileDocument()
        val userRef = db.collection("users").document(userId)
        val ref = userRef
            .collection("watchLater")
            .document(lesson.lessonId)

        return try {
            val existing = ref.get().await()
            if (existing.exists()) {
                ref.delete().await()
                userRef.update(
                    mapOf(
                        "watchLaterCount" to FieldValue.increment(-1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                false
            } else {
                val record = WatchLaterLesson(
                    lessonId = lesson.lessonId,
                    subjectId = course.subjectId,
                    subjectName = course.subjectName,
                    courseId = course.courseId,
                    courseTitle = course.courseTitle,
                    courseDescription = course.courseDescription,
                    lessonTitle = lesson.lessonTitle,
                    lessonOrder = lesson.lessonOrder,
                    question = lesson.quiz.question,
                    options = lesson.quiz.options,
                    correctAnswerIndex = lesson.quiz.correctAnswerIndex,
                    explanation = lesson.quiz.explanation,
                    quizType = lesson.quiz.type.name,
                    videoUrl = lesson.videoUrl,
                    thumbnailResId = lesson.thumbnailResId,
                    savedAt = System.currentTimeMillis()
                )
                ref.set(record).await()
                userRef.update(
                    mapOf(
                        "watchLaterCount" to FieldValue.increment(1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getWatchLaterLessons(): List<WatchLaterLesson> {
        val userId = currentUserId() ?: return emptyList()
        return try {
            val snapshot = db.collection("users").document(userId)
                .collection("watchLater")
                .orderBy("savedAt")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(WatchLaterLesson::class.java)?.copy(
                    lessonId = doc.getString("lessonId").orEmpty()
                )
            }.sortedByDescending { it.savedAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun buildWatchLaterSubject(savedLessons: List<WatchLaterLesson>): StudentFeedSubject {
        return StudentFeedSubject(
            subjectId = WATCH_LATER_SUBJECT_ID,
            subjectName = "Watch Later",
            courses = listOf(
                StudentFeedCourse(
                    subjectId = WATCH_LATER_SUBJECT_ID,
                    subjectName = "Watch Later",
                    courseId = WATCH_LATER_COURSE_ID,
                    courseTitle = "Saved Lessons",
                    courseDescription = "Lessons you saved to watch later.",
                    lessons = savedLessons.map { it.toStudentFeedLesson() }
                )
            )
        )
    }

    private fun buildStudentQuiz(lesson: Lesson): StudentQuiz {
        val component = lesson.components.firstOrNull() ?: return StudentQuiz(
            question = "What is the main takeaway from ${lesson.title.ifBlank { "this lesson" }}?",
            options = listOf(
                "Understand the key concept",
                "Skip the lesson content",
                "Ignore the topic completely",
                "Memorize random details only"
            ),
            correctAnswerIndex = 0,
            explanation = "The goal is to understand the main concept from the lesson before moving on.",
            type = StudentQuizType.MULTIPLE_CHOICE
        )

        val type = component["type"]?.toString().orEmpty()
        val question = component["question"]?.toString()?.ifBlank { null }
            ?: "What is the main takeaway from ${lesson.title.ifBlank { "this lesson" }}?"
        val explanation = component["explanation"]?.toString().orEmpty()

        return when (type) {
            "multiple_choice" -> {
                val options = (component["options"] as? List<*>)
                    ?.mapNotNull { it?.toString() }
                    ?.filter { it.isNotBlank() }
                    .orEmpty()
                val correct = (component["correct"] as? Number)?.toInt()
                if (options.isNotEmpty() && correct != null) {
                    StudentQuiz(
                        question = question,
                        options = options,
                        correctAnswerIndex = correct.coerceIn(0, options.lastIndex),
                        explanation = explanation,
                        type = StudentQuizType.MULTIPLE_CHOICE
                    )
                } else {
                    StudentQuiz(question = question, explanation = explanation)
                }
            }

            "true_false" -> {
                val answer = component["answer"] as? Boolean ?: true
                StudentQuiz(
                    question = question,
                    options = listOf("True", "False"),
                    correctAnswerIndex = if (answer) 0 else 1,
                    explanation = explanation,
                    type = StudentQuizType.TRUE_FALSE
                )
            }

            else -> StudentQuiz(
                question = question,
                explanation = explanation,
                type = StudentQuizType.INFO
            )
        }
    }

    private data class LevelProgress(
        val level: Int,
        val xpIntoCurrentLevel: Int,
        val xpRequiredForNextLevel: Int
    )

    private fun getLevelProgress(totalXp: Int): LevelProgress {
        var level = 1
        var xpRequired = 10
        var remainingXp = totalXp.coerceAtLeast(0)

        while (level < 20 && remainingXp >= xpRequired) {
            remainingXp -= xpRequired
            level++
            xpRequired += 10
        }

        return if (level >= 20) {
            LevelProgress(
                level = 20,
                xpIntoCurrentLevel = 0,
                xpRequiredForNextLevel = 0
            )
        } else {
            LevelProgress(
                level = level,
                xpIntoCurrentLevel = remainingXp,
                xpRequiredForNextLevel = xpRequired
            )
        }
    }

    private fun calculateLevelFromXp(totalXp: Int): Int {
        return getLevelProgress(totalXp).level
    }

    suspend fun getUserProfileModel(): UserProfile {
        val user = Firebase.auth.currentUser ?: return UserProfile()
        ensureUserProfileDocument()

        return try {
            val doc = db.collection("users").document(user.uid).get().await()
            val xp = (doc.getLong("xp") ?: 0L).toInt()
            val currentStreak = (doc.getLong("currentStreak") ?: 0L).toInt()
            val highestStreak = (doc.getLong("highestStreak") ?: 0L).toInt()
            val watchLaterCount = (doc.getLong("watchLaterCount") ?: 0L).toInt()
            val level = calculateLevelFromXp(xp)

            UserProfile(
                uid = user.uid,
                email = user.email.orEmpty(),
                xp = xp,
                level = level,
                currentStreak = currentStreak,
                highestStreak = highestStreak,
                watchLaterCount = watchLaterCount
            )
        } catch (e: Exception) {
            UserProfile(
                uid = user.uid,
                email = user.email.orEmpty()
            )
        }
    }

    suspend fun getUserProfile(): Map<String, Any> {
        val userId = currentUserId() ?: return emptyMap()
        return try {
            val doc = db.collection("users").document(userId).get().await()
            doc.data ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun getUserXp(): Int {
        val userId = currentUserId() ?: return 0
        ensureUserProfileDocument()
        return try {
            val doc = db.collection("users").document(userId).get().await()
            (doc.getLong("xp") ?: 0L).toInt()
        } catch (e: Exception) {
            0
        }
    }

    suspend fun addXp(amount: Int) {
        val userId = currentUserId() ?: return
        ensureUserProfileDocument()
        try {
            val userRef = db.collection("users").document(userId)
            userRef.update(
                mapOf(
                    "xp" to FieldValue.increment(amount.toLong()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val refreshed = userRef.get().await()
            val newXp = (refreshed.getLong("xp") ?: 0L).toInt()
            userRef.update("level", calculateLevelFromXp(newXp)).await()
        } catch (_: Exception) {
        }
    }

    suspend fun getCurrentStreak(): Int {
        val userId = currentUserId() ?: return 0
        ensureUserProfileDocument()
        return try {
            val doc = db.collection("users").document(userId).get().await()
            (doc.getLong("currentStreak") ?: 0L).toInt()
        } catch (e: Exception) {
            0
        }
    }

    suspend fun recordQuizAnswer(
        lessonId: String,
        isCorrect: Boolean
    ): UserProfile {
        val userId = currentUserId() ?: return UserProfile()
        ensureUserProfileDocument()

        val userRef = db.collection("users").document(userId)
        val answeredRef = userRef.collection("answeredLessons").document(lessonId)

        return try {
            val alreadyAnswered = answeredRef.get().await().exists()
            if (alreadyAnswered) {
                return getUserProfileModel()
            }

            val doc = userRef.get().await()
            val currentXp = (doc.getLong("xp") ?: 0L).toInt()
            val currentStreak = (doc.getLong("currentStreak") ?: 0L).toInt()
            val highestStreak = (doc.getLong("highestStreak") ?: 0L).toInt()
            val watchLaterCount = (doc.getLong("watchLaterCount") ?: 0L).toInt()
            val email = doc.getString("email").orEmpty()

            val newCurrentStreak = if (isCorrect) currentStreak + 1 else 0
            val multiplier = when {
                newCurrentStreak >= 5 -> 2.0
                newCurrentStreak >= 3 -> 1.5
                newCurrentStreak >= 2 -> 1.2
                else -> 1.0
            }

            val baseXp = 5
            val xpEarned = if (isCorrect) (baseXp * multiplier).toInt() else 0
            val newXp = currentXp + xpEarned
            val newHighestStreak = maxOf(highestStreak, newCurrentStreak)
            val levelProgress = getLevelProgress(newXp)

            userRef.update(
                mapOf(
                    "xp" to newXp,
                    "level" to levelProgress.level,
                    "currentStreak" to newCurrentStreak,
                    "highestStreak" to newHighestStreak,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            answeredRef.set(
                mapOf(
                    "isCorrect" to isCorrect,
                    "xpEarned" to xpEarned,
                    "answeredAt" to FieldValue.serverTimestamp()
                )
            ).await()

            UserProfile(
                uid = userId,
                email = email,
                xp = newXp,
                level = levelProgress.level,
                currentStreak = newCurrentStreak,
                highestStreak = newHighestStreak,
                watchLaterCount = watchLaterCount
            )
        } catch (e: Exception) {
            getUserProfileModel()
        }
    }

    suspend fun markLessonComplete(lessonId: String, score: Int, total: Int) {
        val userId = currentUserId() ?: return
        try {
            db.collection("users/$userId/progress").document(lessonId).set(
                mapOf(
                    "completed" to true,
                    "score" to score,
                    "total" to total,
                    "completedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) {
        }
    }

    suspend fun getLessonProgress(lessonId: String): Map<String, Any>? {
        val userId = currentUserId() ?: return null
        return try {
            val doc = db.collection("users/$userId/progress").document(lessonId).get().await()
            if (doc.exists()) doc.data else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getLevelInfo(totalXp: Int): Pair<Int, Int> {
        var level = 1
        var xpNeeded = 10
        var remainingXp = totalXp

        while (level < 20 && remainingXp >= xpNeeded) {
            remainingXp -= xpNeeded
            level++
            xpNeeded += 10
        }

        return level to xpNeeded // (current level, xp needed for NEXT level)
    }
}
