package com.example.skillsync.student

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.skillsync.data.FirestoreRepository
import com.example.skillsync.models.StudentFeedCourse
import com.example.skillsync.models.StudentFeedLesson
import com.example.skillsync.models.StudentFeedSubject
import com.example.skillsync.models.StudentQuiz
import com.example.skillsync.models.StudentQuizType
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface StudentDestination {
    data object Feed : StudentDestination
    data class Profile(val subjectIndex: Int, val courseIndex: Int) : StudentDestination
}

@Composable
fun StudentDashboardScreen(onLogout: () -> Unit) {
    val repo = remember { FirestoreRepository() }
    val userEmail = FirebaseAuth.getInstance().currentUser?.email.orEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var subjects by remember { mutableStateOf<List<StudentFeedSubject>>(emptyList()) }
    var selectedSubjectIndex by remember { mutableIntStateOf(0) }
    var destination by remember { mutableStateOf<StudentDestination>(StudentDestination.Feed) }
    var feedStartPage by remember { mutableIntStateOf(0) }
    var selectedCourseIndex by remember { mutableIntStateOf(0) }

    suspend fun refreshSubjects(showLoading: Boolean = false) {
        if (showLoading) loading = true
        subjects = withContext(Dispatchers.IO) { repo.getStudentFeedSubjects() }
        if (subjects.isNotEmpty()) {
            selectedSubjectIndex = selectedSubjectIndex.coerceIn(0, subjects.lastIndex)
            val activeSubject = subjects[selectedSubjectIndex]
            selectedCourseIndex = if (activeSubject.courses.isEmpty()) 0
            else selectedCourseIndex.coerceIn(0, activeSubject.courses.lastIndex)
        } else {
            selectedSubjectIndex = 0
            selectedCourseIndex = 0
        }
        if (showLoading) loading = false
    }

    LaunchedEffect(Unit) {
        refreshSubjects(showLoading = true)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        when {
            loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            subjects.isEmpty() -> EmptyStudentState(userEmail = userEmail, onLogout = onLogout)

            else -> {
                val safeSubjectIndex = selectedSubjectIndex.coerceIn(0, subjects.lastIndex)
                val activeSubject = subjects[safeSubjectIndex]
                val safeCourseIndex = if (activeSubject.courses.isEmpty()) 0
                else selectedCourseIndex.coerceIn(0, activeSubject.courses.lastIndex)
                val activeProfileCourseIndex = when (val current = destination) {
                    is StudentDestination.Profile -> {
                        if (activeSubject.courses.isEmpty()) 0
                        else current.courseIndex.coerceIn(0, activeSubject.courses.lastIndex)
                    }
                    else -> safeCourseIndex
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = destination is StudentDestination.Feed,
                                onClick = { destination = StudentDestination.Feed },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Feed") },
                                label = { Text("Feed") }
                            )
                            NavigationBarItem(
                                selected = destination is StudentDestination.Profile,
                                onClick = {
                                    destination = StudentDestination.Profile(safeSubjectIndex, safeCourseIndex)
                                },
                                icon = { Icon(Icons.Default.Person, contentDescription = "Course") },
                                label = { Text("Course") }
                            )
                        }
                    }
                ) { innerPadding ->
                    when (val current = destination) {
                        is StudentDestination.Feed -> StudentFeedScreen(
                            modifier = Modifier.padding(innerPadding),
                            subjects = subjects,
                            selectedSubjectIndex = safeSubjectIndex,
                            selectedCourseIndex = safeCourseIndex,
                            startPage = feedStartPage,
                            onSelectSubject = { newIndex ->
                                selectedSubjectIndex = newIndex
                                selectedCourseIndex = 0
                                feedStartPage = 0
                            },
                            onSelectCourse = { newCourseIndex ->
                                selectedCourseIndex = newCourseIndex
                                feedStartPage = 0
                            },
                            onOpenProfile = { subjectIndex, courseIndex ->
                                selectedSubjectIndex = subjectIndex
                                selectedCourseIndex = courseIndex
                                destination = StudentDestination.Profile(subjectIndex, courseIndex)
                            },
                            onToggleWatchLater = { lesson, course ->
                                withContext(Dispatchers.IO) {
                                    repo.toggleWatchLater(lesson, course)
                                }
                            },
                            onWatchLaterMessage = { isSavedNow ->
                                snackbarHostState.showSnackbar(
                                    message = if (isSavedNow) {
                                        "Lesson saved to \"Watch Later\""
                                    } else {
                                        "Lesson removed from \"Watch Later\""
                                    },
                                    duration = SnackbarDuration.Short
                                )
                            },
                            onRefreshAfterWatchLater = {
                                refreshSubjects()
                            }
                        )

                        is StudentDestination.Profile -> StudentProfileScreen(
                            modifier = Modifier.padding(innerPadding),
                            subjects = subjects,
                            subjectIndex = current.subjectIndex.coerceIn(0, subjects.lastIndex),
                            courseIndex = current.courseIndex,
                            onBack = { destination = StudentDestination.Feed },
                            onOpenLesson = { subjectIndex, lessonIndex ->
                                selectedSubjectIndex = subjectIndex
                                selectedCourseIndex = current.courseIndex
                                feedStartPage = lessonIndex.coerceAtLeast(0)
                                destination = StudentDestination.Feed
                            },
                            onSwitchSubject = { newSubjectIndex ->
                                selectedSubjectIndex = newSubjectIndex
                                selectedCourseIndex = 0
                                destination = StudentDestination.Profile(newSubjectIndex, 0)
                            },
                            onSwitchCourse = { newCourseIndex ->
                                selectedCourseIndex = newCourseIndex
                                destination = StudentDestination.Profile(selectedSubjectIndex, newCourseIndex)
                            },
                            onLogout = onLogout
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStudentState(userEmail: String, onLogout: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text("No courses available yet", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (userEmail.isBlank()) "Ask an admin to add a subject, course, and lesson."
                else "Logged in as $userEmail. Ask an admin to add a subject, course, and lesson.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = {
                FirebaseAuth.getInstance().signOut()
                onLogout()
            }) {
                Text("Logout")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StudentFeedScreen(
    modifier: Modifier = Modifier,
    subjects: List<StudentFeedSubject>,
    selectedSubjectIndex: Int,
    selectedCourseIndex: Int,
    startPage: Int,
    onSelectSubject: (Int) -> Unit,
    onSelectCourse: (Int) -> Unit,
    onOpenProfile: (Int, Int) -> Unit,
    onToggleWatchLater: suspend (StudentFeedLesson, StudentFeedCourse) -> Boolean,
    onWatchLaterMessage: suspend (Boolean) -> Unit,
    onRefreshAfterWatchLater: suspend () -> Unit
) {
    val subject = subjects[selectedSubjectIndex.coerceIn(0, subjects.lastIndex)]
    val safeCourseIndex = if (subject.courses.isEmpty()) 0 else selectedCourseIndex.coerceIn(0, subject.courses.lastIndex)
    val activeCourse = subject.courses.getOrNull(safeCourseIndex)
    val lessonPages = remember(subject.subjectId, safeCourseIndex, activeCourse) {
        activeCourse
            ?.lessons
            ?.sortedBy { it.lessonOrder }
            ?.mapIndexed { lessonIndex, lesson ->
                SubjectFeedPage(safeCourseIndex, lessonIndex, activeCourse, lesson)
            }
            .orEmpty()
    }

    if (subject.courses.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No courses found in this subject")
        }
        return
    }

    if (lessonPages.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No lessons found in this course")
        }

        SubjectTabs(
            subjects = subjects,
            selectedSubjectIndex = selectedSubjectIndex,
            onSelectSubject = onSelectSubject,
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 8.dp, start = 12.dp, end = 12.dp)
        )
        CourseTabs(
            courses = subject.courses,
            selectedCourseIndex = safeCourseIndex,
            onSelectCourse = onSelectCourse,
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 62.dp, start = 12.dp, end = 12.dp)
        )
        return
    }

    val safeStartPage = startPage.coerceIn(0, lessonPages.lastIndex)
    val quizStateMap = remember { mutableStateMapOf<String, QuizUiState>() }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        SubjectTabs(
            subjects = subjects,
            selectedSubjectIndex = selectedSubjectIndex,
            onSelectSubject = onSelectSubject,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 8.dp, start = 12.dp, end = 12.dp)
        )
        CourseTabs(
            courses = subject.courses,
            selectedCourseIndex = safeCourseIndex,
            onSelectCourse = onSelectCourse,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 62.dp, start = 12.dp, end = 12.dp)
        )
        key(subject.subjectId, safeCourseIndex, safeStartPage) {
            val pagerState = rememberPagerState(initialPage = safeStartPage, pageCount = { lessonPages.size })
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val item = lessonPages[page]
                val quizState = quizStateMap.getOrPut(item.lesson.lessonId) { QuizUiState() }
                LessonFeedPage(
                    item = item,
                    subjectName = subject.subjectName,
                    quizState = quizState,
                    onOpenProfile = { onOpenProfile(selectedSubjectIndex, item.courseIndex) },
                    onToggleWatchLater = { lesson, course ->
                        scope.launch {
                            val isSavedNow = onToggleWatchLater(lesson, course)
                            onRefreshAfterWatchLater()
                            onWatchLaterMessage(isSavedNow)
                        }
                    }
                )
            }
        }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CourseTabs(
    courses: List<StudentFeedCourse>,
    selectedCourseIndex: Int,
    onSelectCourse: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (courses.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Courses",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            courses.forEachIndexed { index, course ->
                FilterChip(
                    selected = index == selectedCourseIndex,
                    onClick = { onSelectCourse(index) },
                    label = {
                        Text(
                            text = course.courseTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectTabs(
    subjects: List<StudentFeedSubject>,
    selectedSubjectIndex: Int,
    onSelectSubject: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Subjects",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            subjects.forEachIndexed { index, subject ->
                FilterChip(
                    selected = index == selectedSubjectIndex,
                    onClick = { onSelectSubject(index) },
                    label = { Text(subject.subjectName) }
                )
            }
        }
    }
}

@Composable
private fun LessonFeedPage(
    item: SubjectFeedPage,
    subjectName: String,
    quizState: QuizUiState,
    onOpenProfile: () -> Unit,
    onToggleWatchLater: (StudentFeedLesson, StudentFeedCourse) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        VideoBackground(videoResId = item.lesson.videoResId)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0x70000000), Color.Transparent, Color(0xD0000000))
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                )

                Spacer(modifier = Modifier.size(10.dp))

                TextButton(onClick = onOpenProfile) {
                    Text(
                        text = item.course.courseTitle,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = item.lesson.lessonTitle,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = subjectName,
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (item.course.courseDescription.isNotBlank()) {
                Text(
                    text = item.course.courseDescription,
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { quizState.isVisible = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Test knowledge")
                }

                OutlinedButton(
                    onClick = { onToggleWatchLater(item.lesson, item.course) },
                    modifier = Modifier
                        .height(40.dp)
                        .aspectRatio(1f),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (item.lesson.isSaved) {
                            Color.White.copy(alpha = 0.22f)
                        } else {
                            Color.Black.copy(alpha = 0.16f)
                        },
                        contentColor = Color.White
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (item.lesson.isSaved) {
                                Icons.Default.Bookmark
                            } else {
                                Icons.Default.BookmarkBorder
                            },
                            contentDescription = if (item.lesson.isSaved) {
                                "Remove from Watch Later"
                            } else {
                                "Save to Watch Later"
                            },
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (quizState.isVisible) {
            QuizOverlay(
                quiz = item.lesson.quiz,
                state = quizState,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun QuizOverlay(
    quiz: StudentQuiz,
    state: QuizUiState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.56f))
    )

    Card(
        modifier = modifier.padding(20.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Knowledge Check",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    state.isVisible = false
                    state.submitted = false
                    state.selectedAnswerIndex = null
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = quiz.question, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))

            when (quiz.type) {
                StudentQuizType.MULTIPLE_CHOICE, StudentQuizType.TRUE_FALSE -> {
                    quiz.options.forEachIndexed { index, option ->
                        val isCorrect = state.submitted && index == quiz.correctAnswerIndex
                        val isWrongSelection = state.submitted && state.selectedAnswerIndex == index && index != quiz.correctAnswerIndex
                        val containerColor = when {
                            isCorrect -> Color(0x1F4CAF50)
                            isWrongSelection -> Color(0x1FFF5252)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .clickable(enabled = !state.submitted) { state.selectedAnswerIndex = index },
                            colors = CardDefaults.cardColors(containerColor = containerColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = state.selectedAnswerIndex == index,
                                    onClick = { state.selectedAnswerIndex = index },
                                    enabled = !state.submitted
                                )
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (state.submitted && isCorrect) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Correct")
                                }
                            }
                        }
                    }
                }

                StudentQuizType.INFO -> {
                    Text(
                        text = "This lesson has an info card instead of a scored question.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (state.submitted) {
                val isCorrectAnswer = state.selectedAnswerIndex == quiz.correctAnswerIndex
                Text(
                    text = if (quiz.correctAnswerIndex == null) "Review complete"
                    else if (isCorrectAnswer) "Correct"
                    else "Incorrect",
                    fontWeight = FontWeight.Bold,
                    color = if (quiz.correctAnswerIndex == null || isCorrectAnswer) Color(0xFF2E7D32) else Color(0xFFC62828)
                )

                if (quiz.correctAnswerIndex != null) {
                    val answerText = quiz.options.getOrNull(quiz.correctAnswerIndex).orEmpty()
                    Text(
                        text = "Correct answer: $answerText",
                        modifier = Modifier.padding(top = 6.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (quiz.explanation.isNotBlank()) {
                    Text(
                        text = quiz.explanation,
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        state.isVisible = false
                        state.submitted = false
                        state.selectedAnswerIndex = null
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (state.submitted) "Done" else "Close")
                }
                Button(
                    onClick = { state.submitted = true },
                    enabled = quiz.correctAnswerIndex == null || state.selectedAnswerIndex != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (state.submitted) "Results" else "Submit")
                }
            }
        }
    }
}

@Composable
private fun VideoBackground(videoResId: Int) {
    var isPaused by remember(videoResId) { mutableStateOf(false) }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                isPaused = !isPaused
            },
        factory = { context ->
            VideoView(context).apply {
                val uri = Uri.parse("android.resource://${context.packageName}/$videoResId")
                tag = videoResId
                setVideoURI(uri)
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.isLooping = true
                    if (!isPaused) {
                        start()
                    }
                }
            }
        },
        update = { videoView ->
            val uri = Uri.parse("android.resource://${videoView.context.packageName}/$videoResId")
            if (videoView.tag != videoResId) {
                videoView.tag = videoResId
                videoView.setVideoURI(uri)
                videoView.setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.isLooping = true
                    if (!isPaused) {
                        videoView.start()
                    }
                }
            }

            if (isPaused) {
                if (videoView.isPlaying) {
                    videoView.pause()
                }
            } else {
                if (!videoView.isPlaying) {
                    videoView.start()
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudentProfileScreen(
    modifier: Modifier = Modifier,
    subjects: List<StudentFeedSubject>,
    subjectIndex: Int,
    courseIndex: Int,
    onBack: () -> Unit,
    onOpenLesson: (Int, Int) -> Unit,
    onSwitchSubject: (Int) -> Unit,
    onSwitchCourse: (Int) -> Unit,
    onLogout: () -> Unit
) {
    val safeSubjectIndex = subjectIndex.coerceIn(0, subjects.lastIndex)
    val subject = subjects[safeSubjectIndex]
    val safeCourseIndex = if (subject.courses.isEmpty()) 0 else courseIndex.coerceIn(0, subject.courses.lastIndex)
    val course = subject.courses.getOrNull(safeCourseIndex)
    val orderedLessons = course?.lessons?.sortedBy { it.lessonOrder }.orEmpty()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }

            Text(
                text = course?.courseTitle.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = {
                FirebaseAuth.getInstance().signOut()
                onLogout()
            }) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = course?.courseTitle?.take(1)?.uppercase().orEmpty(),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = course?.courseTitle.orEmpty(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = subject.subjectName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (!course?.courseDescription.isNullOrBlank()) {
                Text(text = course?.courseDescription.orEmpty(), modifier = Modifier.padding(top = 8.dp))
            }
        }

        SubjectSection(
            subjects = subjects,
            selectedSubjectIndex = safeSubjectIndex,
            onSwitchSubject = onSwitchSubject
        )

        Text(
            text = "Choose course",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            subject.courses.forEachIndexed { index, item ->
                FilterChip(
                    selected = index == safeCourseIndex,
                    onClick = { onSwitchCourse(index) },
                    label = {
                        Text(
                            item.courseTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }

        if (orderedLessons.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (subject.subjectId == "__watch_later__") {
                        "No saved lessons yet"
                    } else {
                        "No lessons available yet"
                    }
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(orderedLessons) { lessonIndex, lesson ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenLesson(safeSubjectIndex, lessonIndex) }
                    ) {
                        Image(
                            painter = painterResource(id = lesson.thumbnailResId),
                            contentDescription = lesson.lessonTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Transparent, Color(0xAA000000))
                                    )
                                )
                        )

                        Text(
                            text = lesson.lessonTitle,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectSection(
    subjects: List<StudentFeedSubject>,
    selectedSubjectIndex: Int,
    onSwitchSubject: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = "Choose subject",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            subjects.forEachIndexed { index, subject ->
                FilterChip(
                    selected = index == selectedSubjectIndex,
                    onClick = { onSwitchSubject(index) },
                    label = { Text(subject.subjectName) }
                )
            }
        }
    }
}

private data class SubjectFeedPage(
    val courseIndex: Int,
    val lessonIndex: Int,
    val course: StudentFeedCourse,
    val lesson: StudentFeedLesson
)

private class QuizUiState(
    isVisible: Boolean = false,
    selectedAnswerIndex: Int? = null,
    submitted: Boolean = false
) {
    var isVisible by mutableStateOf(isVisible)
    var selectedAnswerIndex by mutableStateOf(selectedAnswerIndex)
    var submitted by mutableStateOf(submitted)
}


