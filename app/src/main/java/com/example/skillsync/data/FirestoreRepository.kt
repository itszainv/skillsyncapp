package com.example.skillsync.data

import com.example.skillsync.models.Course
import com.example.skillsync.models.Lesson
import com.example.skillsync.models.StudentFeedCourse
import com.example.skillsync.models.StudentFeedLesson
import com.example.skillsync.models.StudentFeedSubject
import com.example.skillsync.models.StudentQuiz
import com.example.skillsync.models.StudentQuizType
import com.example.skillsync.models.Subject
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db: FirebaseFirestore = Firebase.firestore

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
        val videoIds = listOf(com.example.skillsync.R.raw.video1, com.example.skillsync.R.raw.video2)
        val thumbnailIds = listOf(
            com.example.skillsync.R.drawable.example_cover,
            com.example.skillsync.R.drawable.ic_launcher_foreground
        )

        return try {
            getSubjects().mapIndexedNotNull { subjectIndex, subject ->
                val courses = getCourses(subject.id).mapIndexedNotNull { courseIndex, course ->
                    val lessons = getLessons(subject.id, course.id)
                        .sortedBy { it.order }
                        .mapIndexed { lessonIndex, lesson ->
                            val mediaIndex = (subjectIndex + courseIndex + lessonIndex) % videoIds.size
                            StudentFeedLesson(
                                lessonId = lesson.id,
                                lessonTitle = lesson.title.ifBlank { "Lesson ${lessonIndex + 1}" },
                                lessonOrder = lesson.order,
                                quiz = buildStudentQuiz(lesson),
                                videoResId = videoIds[mediaIndex],
                                thumbnailResId = thumbnailIds[mediaIndex % thumbnailIds.size]
                            )
                        }

                    if (lessons.isEmpty()) {
                        null
                    } else {
                        StudentFeedCourse(
                            subjectId = subject.id,
                            subjectName = subject.name,
                            courseId = course.id,
                            courseTitle = course.title,
                            courseDescription = course.description,
                            lessons = lessons
                        )
                    }
                }

                if (courses.isEmpty()) {
                    null
                } else {
                    StudentFeedSubject(
                        subjectId = subject.id,
                        subjectName = subject.name,
                        courses = courses
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
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

    suspend fun getUserProfile(): Map<String, Any> {
        val userId = Firebase.auth.currentUser?.uid ?: return emptyMap()
        return try {
            val doc = db.collection("users").document(userId).get().await()
            doc.data ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun getUserXp(): Int {
        val userId = Firebase.auth.currentUser?.uid ?: return 0
        return try {
            val doc = db.collection("users").document(userId).get().await()
            (doc.getLong("xp") ?: 0L).toInt()
        } catch (e: Exception) {
            0
        }
    }

    suspend fun addXp(amount: Int) {
        val userId = Firebase.auth.currentUser?.uid ?: return
        try {
            db.collection("users").document(userId)
                .update("xp", FieldValue.increment(amount.toLong()))
                .await()
        } catch (_: Exception) {
        }
    }

    suspend fun markLessonComplete(lessonId: String, score: Int, total: Int) {
        val userId = Firebase.auth.currentUser?.uid ?: return
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
        val userId = Firebase.auth.currentUser?.uid ?: return null
        return try {
            val doc = db.collection("users/$userId/progress").document(lessonId).get().await()
            if (doc.exists()) doc.data else null
        } catch (e: Exception) {
            null
        }
    }
}
