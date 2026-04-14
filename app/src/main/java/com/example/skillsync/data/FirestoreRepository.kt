package com.example.skillsync.data

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
import com.example.skillsync.models.LeaderboardData
import com.example.skillsync.models.LeaderboardEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirestoreRepository {

    private data class LevelProgress(
        val level: Int,
        val xpIntoCurrentLevel: Int,
        val xpRequiredForNextLevel: Int
    )

    private fun todayKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

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

    private suspend fun getCompletedLessonIds(): Set<String> {
        val userId = currentUserId() ?: return emptySet()
        return try {
            db.collection("users")
                .document(userId)
                .collection("completedLessons")
                .get()
                .await()
                .documents
                .map { it.id }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    private suspend fun claimProgressMilestoneIfNeeded(
        userId: String,
        type: String,
        entityId: String,
        completionPercent: Int
    ): Int {
        val milestones = listOf(25, 50, 75, 100)
        var bonusXp = 0

        for (milestone in milestones) {
            if (completionPercent >= milestone) {
                val claimId = "${type}_${entityId}_$milestone"
                val claimRef = db.collection("users")
                    .document(userId)
                    .collection("claimedMilestones")
                    .document(claimId)

                val existing = claimRef.get().await()
                if (!existing.exists()) {
                    claimRef.set(
                        mapOf(
                            "type" to type,
                            "entityId" to entityId,
                            "milestone" to milestone,
                            "claimedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    bonusXp += 15
                }
            }
        }

        return bonusXp
    }

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

        val defaultFields = mapOf(
            "email" to user.email.orEmpty(),
            "xp" to (snapshot.getLong("xp") ?: 0L),
            "level" to (snapshot.getLong("level") ?: 1L),
            "currentStreak" to (snapshot.getLong("currentStreak") ?: 0L),
            "highestStreak" to (snapshot.getLong("highestStreak") ?: 0L),
            "watchLaterCount" to (snapshot.getLong("watchLaterCount") ?: 0L),
            "dailyXp" to (snapshot.getLong("dailyXp") ?: 0L),
            "dailyXpDate" to (snapshot.getString("dailyXpDate") ?: todayKey()),
            "skillBux" to (snapshot.getLong("skillBux") ?: 0L),
            "theme" to (snapshot.getString("theme") ?: "red_black"),
            "avatar" to (snapshot.getString("avatar") ?: "🙂"),
            "nameIcon" to (snapshot.getString("nameIcon") ?: ""),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        ref.set(defaultFields, com.google.firebase.firestore.SetOptions.merge()).await()
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
        val videoIds = listOf(
            com.example.skillsync.R.raw.video1,
            com.example.skillsync.R.raw.video2
        )
        val thumbnailIds = listOf(
            com.example.skillsync.R.drawable.example_cover,
            com.example.skillsync.R.drawable.ic_launcher_foreground
        )

        return try {
            val savedLessons = getWatchLaterLessons()
            val savedLessonIds = savedLessons.map { it.lessonId }.toSet()
            val completedLessonIds = getCompletedLessonIds()

            val regularSubjects = getSubjects().mapIndexed { subjectIndex, subject ->
                val courses = getCourses(subject.id).mapIndexed { courseIndex, course ->
                    val rawLessons = getLessons(subject.id, course.id).sortedBy { it.order }

                    val lessons = rawLessons.mapIndexed { lessonIndex, lesson ->
                        val mediaIndex = if (videoIds.isEmpty()) 0
                        else (subjectIndex + courseIndex + lessonIndex) % videoIds.size

                        StudentFeedLesson(
                            lessonId = lesson.id,
                            lessonTitle = lesson.title.ifBlank { "Lesson ${lessonIndex + 1}" },
                            lessonOrder = lesson.order,
                            quiz = buildStudentQuiz(lesson),
                            videoResId = videoIds[mediaIndex],
                            thumbnailResId = thumbnailIds[mediaIndex % thumbnailIds.size],
                            isSaved = lesson.id in savedLessonIds,
                            isCompleted = lesson.id in completedLessonIds
                        )
                    }

                    val totalLessons = lessons.size
                    val completedLessons = lessons.count { it.isCompleted }
                    val courseCompletionPercent =
                        if (totalLessons == 0) 0 else (completedLessons * 100) / totalLessons

                    StudentFeedCourse(
                        subjectId = subject.id,
                        subjectName = subject.name,
                        courseId = course.id,
                        courseTitle = course.title,
                        courseDescription = course.description,
                        lessons = lessons,
                        completionPercent = courseCompletionPercent,
                        completedLessons = completedLessons,
                        totalLessons = totalLessons
                    )
                }

                val totalCourses = courses.size
                val completedCourses =
                    courses.count { it.totalLessons > 0 && it.completedLessons == it.totalLessons }
                val subjectCompletionPercent =
                    if (totalCourses == 0) 0 else (completedCourses * 100) / totalCourses

                StudentFeedSubject(
                    subjectId = subject.id,
                    subjectName = subject.name,
                    courses = courses,
                    completionPercent = subjectCompletionPercent,
                    completedCourses = completedCourses,
                    totalCourses = totalCourses
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
                    videoUrl = lesson.videoResId.toString(),
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

            snapshot.documents.map { doc ->
                WatchLaterLesson(
                    lessonId = doc.getString("lessonId").orEmpty(),
                    subjectId = doc.getString("subjectId").orEmpty(),
                    subjectName = doc.getString("subjectName").orEmpty(),
                    courseId = doc.getString("courseId").orEmpty(),
                    courseTitle = doc.getString("courseTitle").orEmpty(),
                    courseDescription = doc.getString("courseDescription").orEmpty(),
                    lessonTitle = doc.getString("lessonTitle").orEmpty(),
                    lessonOrder = (doc.getLong("lessonOrder") ?: 0L).toInt(),
                    question = doc.getString("question").orEmpty(),
                    options = (doc.get("options") as? List<*>)
                        ?.mapNotNull { it?.toString() }
                        .orEmpty(),
                    correctAnswerIndex = (doc.getLong("correctAnswerIndex") ?: 0L).toInt(),
                    explanation = doc.getString("explanation").orEmpty(),
                    quizType = doc.getString("quizType").orEmpty(),
                    videoUrl = doc.getString("videoUrl")
                        ?: (doc.getLong("videoResId")?.toString().orEmpty()),
                    thumbnailResId = (doc.getLong("thumbnailResId") ?: 0L).toInt(),
                    savedAt = doc.getLong("savedAt") ?: 0L
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
                    lessons = savedLessons.map { saved ->
                        StudentFeedLesson(
                            lessonId = saved.lessonId,
                            lessonTitle = saved.lessonTitle,
                            lessonOrder = saved.lessonOrder,
                            quiz = StudentQuiz(
                                question = saved.question,
                                options = saved.options,
                                correctAnswerIndex = saved.correctAnswerIndex,
                                explanation = saved.explanation,
                                type = when (saved.quizType) {
                                    StudentQuizType.TRUE_FALSE.name -> StudentQuizType.TRUE_FALSE
                                    StudentQuizType.INFO.name -> StudentQuizType.INFO
                                    else -> StudentQuizType.MULTIPLE_CHOICE
                                }
                            ),
                            videoResId = saved.videoUrl.toIntOrNull() ?: com.example.skillsync.R.raw.video1,
                            thumbnailResId = saved.thumbnailResId,
                            isSaved = true,
                            isCompleted = false
                        )
                    },
                    completionPercent = 0,
                    completedLessons = 0,
                    totalLessons = savedLessons.size
                )
            ),
            completionPercent = 0,
            completedCourses = 0,
            totalCourses = 1
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
            val dailyXp = (doc.getLong("dailyXp") ?: 0L).toInt()
            val skillBux = (doc.getLong("skillBux") ?: 0L).toInt()
            val selectedTheme = doc.getString("theme").orEmpty().ifBlank { "dark_red" }
            val purchasedThemes = (doc.get("purchasedThemes") as? List<*>)?.mapNotNull { it?.toString() }?.ifEmpty { listOf("dark_red") } ?: listOf("dark_red")
            val selectedAvatar = doc.getString("avatar").orEmpty().ifBlank { "🙂" }
            val purchasedAvatars = (doc.get("purchasedAvatars") as? List<*>)?.mapNotNull { it?.toString() }?.ifEmpty { listOf("🙂") } ?: listOf("🙂")
            val selectedNameIcon = doc.getString("nameIcon").orEmpty()
            val purchasedNameIcons = (doc.get("purchasedNameIcons") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

            UserProfile(
                uid = user.uid,
                email = user.email.orEmpty(),
                xp = xp,
                level = level,
                currentStreak = currentStreak,
                highestStreak = highestStreak,
                watchLaterCount = watchLaterCount,
                dailyXp = dailyXp,
                skillBux = skillBux,
                selectedTheme = selectedTheme,
                purchasedThemes = purchasedThemes,
                selectedAvatar = selectedAvatar,
                purchasedAvatars = purchasedAvatars,
                selectedNameIcon = selectedNameIcon,
                purchasedNameIcons = purchasedNameIcons
            )
        } catch (e: Exception) {
            UserProfile(
                uid = user.uid,
                email = user.email.orEmpty(),
                dailyXp = 0,
                skillBux = 0,
                selectedTheme = "dark_red",
                purchasedThemes = listOf("dark_red"),
                selectedAvatar = "🙂",
                purchasedAvatars = listOf("🙂"),
                selectedNameIcon = "",
                purchasedNameIcons = emptyList()
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
        val completedRef = userRef.collection("completedLessons").document(lessonId)

        return try {
            val doc = userRef.get().await()

            val currentXp = (doc.getLong("xp") ?: 0L).toInt()
            val currentStreak = (doc.getLong("currentStreak") ?: 0L).toInt()
            val highestStreak = (doc.getLong("highestStreak") ?: 0L).toInt()
            val watchLaterCount = (doc.getLong("watchLaterCount") ?: 0L).toInt()
            val storedDailyXp = (doc.getLong("dailyXp") ?: 0L).toInt()
            val storedDailyXpDate = doc.getString("dailyXpDate").orEmpty()
            val currentSkillBux = (doc.getLong("skillBux") ?: 0L).toInt()
            val email = doc.getString("email").orEmpty()
            val today = todayKey()

            val alreadyCompleted = completedRef.get().await().exists()
            if (alreadyCompleted) {
                return getUserProfileModel()
            }

            val currentDailyXp = if (storedDailyXpDate == today) storedDailyXp else 0

            if (!isCorrect) {
                userRef.update(
                    mapOf(
                        "currentStreak" to 0,
                        "dailyXp" to currentDailyXp,
                        "dailyXpDate" to today,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                return getUserProfileModel().copy(
                    uid = userId,
                    email = email,
                    xp = currentXp,
                    level = calculateLevelFromXp(currentXp),
                    currentStreak = 0,
                    highestStreak = highestStreak,
                    watchLaterCount = watchLaterCount,
                    dailyXp = currentDailyXp,
                    skillBux = currentSkillBux
                )
            }

            val newCurrentStreak = currentStreak + 1
            val multiplier = when {
                newCurrentStreak >= 5 -> 2.0
                newCurrentStreak >= 3 -> 1.5
                newCurrentStreak >= 2 -> 1.2
                else -> 1.0
            }

            val baseXp = 5
            val quizXp = (baseXp * multiplier).toInt()

            completedRef.set(
                mapOf(
                    "completedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val refreshedSubjects = getStudentFeedSubjects()
            val subject = refreshedSubjects.firstOrNull { s ->
                s.courses.any { c -> c.lessons.any { it.lessonId == lessonId } }
            }
            val course = subject?.courses?.firstOrNull { c ->
                c.lessons.any { it.lessonId == lessonId }
            }

            val courseBonus = if (course != null) {
                claimProgressMilestoneIfNeeded(userId, "course", course.courseId, course.completionPercent)
            } else 0

            val subjectBonus = if (subject != null) {
                claimProgressMilestoneIfNeeded(userId, "subject", subject.subjectId, subject.completionPercent)
            } else 0

            val totalXpToAdd = quizXp + courseBonus + subjectBonus
            val newXp = currentXp + totalXpToAdd
            val newDailyXp = currentDailyXp + totalXpToAdd
            val newHighestStreak = maxOf(highestStreak, newCurrentStreak)
            val oldLevel = calculateLevelFromXp(currentXp)
            val levelProgress = getLevelProgress(newXp)
            val levelsGained = (levelProgress.level - oldLevel).coerceAtLeast(0)
            val skillBuxEarned = levelsGained * 25
            val newSkillBux = currentSkillBux + skillBuxEarned

            userRef.update(
                mapOf(
                    "xp" to newXp,
                    "level" to levelProgress.level,
                    "currentStreak" to newCurrentStreak,
                    "highestStreak" to newHighestStreak,
                    "dailyXp" to newDailyXp,
                    "dailyXpDate" to today,
                    "skillBux" to newSkillBux,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            getUserProfileModel().copy(
                uid = userId,
                email = email,
                xp = newXp,
                level = levelProgress.level,
                currentStreak = newCurrentStreak,
                highestStreak = newHighestStreak,
                watchLaterCount = watchLaterCount,
                dailyXp = newDailyXp,
                skillBux = newSkillBux
            )
        } catch (e: Exception) {
            getUserProfileModel()
        }
    }

    suspend fun applyTheme(themeId: String) {
        val userId = currentUserId() ?: return
        ensureUserProfileDocument()
        db.collection("users").document(userId)
            .update(
                mapOf(
                    "theme" to themeId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
    }

    suspend fun applyAvatar(avatar: String) {
        val userId = currentUserId() ?: return
        ensureUserProfileDocument()
        db.collection("users").document(userId)
            .update(
                mapOf(
                    "avatar" to avatar,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
    }

    suspend fun applyNameIcon(icon: String) {
        val userId = currentUserId() ?: return
        ensureUserProfileDocument()
        db.collection("users").document(userId)
            .update(
                mapOf(
                    "nameIcon" to icon,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
    }

    suspend fun purchaseTheme(themeId: String, cost: Int): Boolean {
        val userId = currentUserId() ?: return false
        ensureUserProfileDocument()
        val userRef = db.collection("users").document(userId)
        val doc = userRef.get().await()
        val skillBux = (doc.getLong("skillBux") ?: 0L).toInt()
        val purchased = (doc.get("purchasedThemes") as? List<*>)?.mapNotNull { it?.toString() } ?: listOf("dark_red")
        if (themeId in purchased) return true
        if (skillBux < cost) return false
        userRef.update(
            mapOf(
                "skillBux" to skillBux - cost,
                "purchasedThemes" to purchased + themeId,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        return true
    }

    suspend fun purchaseAvatar(avatar: String, cost: Int): Boolean {
        val userId = currentUserId() ?: return false
        ensureUserProfileDocument()
        val userRef = db.collection("users").document(userId)
        val doc = userRef.get().await()
        val skillBux = (doc.getLong("skillBux") ?: 0L).toInt()
        val purchased = (doc.get("purchasedAvatars") as? List<*>)?.mapNotNull { it?.toString() } ?: listOf("🙂")
        if (avatar in purchased) return true
        if (skillBux < cost) return false
        userRef.update(
            mapOf(
                "skillBux" to skillBux - cost,
                "purchasedAvatars" to purchased + avatar,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        return true
    }

    suspend fun purchaseNameIcon(icon: String, cost: Int): Boolean {
        val userId = currentUserId() ?: return false
        ensureUserProfileDocument()
        val userRef = db.collection("users").document(userId)
        val doc = userRef.get().await()
        val skillBux = (doc.getLong("skillBux") ?: 0L).toInt()
        val purchased = (doc.get("purchasedNameIcons") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        if (icon in purchased) return true
        if (skillBux < cost) return false
        userRef.update(
            mapOf(
                "skillBux" to skillBux - cost,
                "purchasedNameIcons" to purchased + icon,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        return true
    }

    suspend fun getLeaderboardData(): LeaderboardData {
        return try {
            val usersSnapshot = db.collection("users").get().await()
            val today = todayKey()

            val entries = usersSnapshot.documents.map { doc ->
                val email = doc.getString("email").orEmpty()
                val displayName = doc.getString("displayName").orEmpty()
                val name = displayName.ifBlank {
                    if (email.isNotBlank()) email.substringBefore("@") else "User"
                }

                val highestStreak = (doc.getLong("highestStreak") ?: 0L).toInt()
                val level = (doc.getLong("level") ?: 1L).toInt()
                val dailyXpDate = doc.getString("dailyXpDate").orEmpty()
                val dailyXp = if (dailyXpDate == today) {
                    (doc.getLong("dailyXp") ?: 0L).toInt()
                } else {
                    0
                }

                Triple(
                    LeaderboardEntry(name = name, value = highestStreak),
                    LeaderboardEntry(name = name, value = level),
                    LeaderboardEntry(name = name, value = dailyXp)
                )
            }

            LeaderboardData(
                topStreakUsers = entries.map { it.first }
                    .sortedByDescending { it.value }
                    .take(10),
                topLevelUsers = entries.map { it.second }
                    .sortedByDescending { it.value }
                    .take(10),
                topDailyXpUsers = entries.map { it.third }
                    .sortedByDescending { it.value }
                    .take(10)
            )
        } catch (e: Exception) {
            LeaderboardData()
        }
    }
}
