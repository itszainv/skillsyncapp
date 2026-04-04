package com.example.skillsync.data

import com.example.skillsync.models.Course
import com.example.skillsync.models.Lesson
import com.example.skillsync.models.Subject
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val db: FirebaseFirestore = Firebase.firestore

    // ── Subjects ──────────────────────────────────────────────────────────────

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

    // ── Courses ───────────────────────────────────────────────────────────────

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

    // ── Lessons ───────────────────────────────────────────────────────────────

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

    // ── Users ─────────────────────────────────────────────────────────────────

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
        } catch (e: Exception) {
            // Handle error
        }
    }

    // ── Progress ──────────────────────────────────────────────────────────────

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
        } catch (e: Exception) {
            // Handle error
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
