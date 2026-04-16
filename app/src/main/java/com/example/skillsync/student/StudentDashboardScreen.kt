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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.material3.HorizontalDivider
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
import com.example.skillsync.models.UserProfile
import com.example.skillsync.models.ShopItem
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.EmojiEvents
import com.example.skillsync.models.LeaderboardData
import androidx.compose.foundation.layout.width
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter

private sealed interface StudentDestination {
    data object Feed : StudentDestination
    data class Courses(val subjectIndex: Int, val courseIndex: Int) : StudentDestination
    data object Shop : StudentDestination
    data object Profile : StudentDestination
    data object Leaderboards : StudentDestination
}

@Composable
fun StudentDashboardScreen(onLogout: () -> Unit, onThemeChanged: (String) -> Unit) {
    val repo = remember { FirestoreRepository() }
    val userEmail = FirebaseAuth.getInstance().currentUser?.email.orEmpty()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var subjects by remember { mutableStateOf<List<StudentFeedSubject>>(emptyList()) }
    var selectedSubjectIndex by remember { mutableIntStateOf(0) }
    var destination by remember { mutableStateOf<StudentDestination>(StudentDestination.Feed) }
    var feedStartPage by remember { mutableIntStateOf(0) }
    var selectedCourseIndex by remember { mutableIntStateOf(0) }
    var userProfile by remember { mutableStateOf(UserProfile()) }
    var leaderboardData by remember { mutableStateOf(LeaderboardData()) }
    val savedLessonsCount = remember(subjects) {
        subjects
            .flatMap { it.courses }
            .flatMap { it.lessons }
            .count { it.isSaved }
    }
    val totalCoursesCount = remember(subjects) { subjects.sumOf { it.courses.size } }
    val totalLessonsCount = remember(subjects) {
        subjects.flatMap { it.courses }.sumOf { it.lessons.size }
    }

    suspend fun refreshSubjects(showLoading: Boolean = false) {
        if (showLoading) loading = true
        subjects = withContext(Dispatchers.IO) { repo.getStudentFeedSubjects() }
        userProfile = withContext(Dispatchers.IO) { repo.getUserProfileModel() }
        leaderboardData = withContext(Dispatchers.IO) { repo.getLeaderboardData() }

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

                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            tonalElevation = 0.dp
                        ) {
                            NavigationBarItem(
                                selected = destination is StudentDestination.Feed,
                                onClick = { destination = StudentDestination.Feed },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Feed", modifier = Modifier.size(20.dp)) },
                                label = { Text("Feed", style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    unselectedTextColor = MaterialTheme.colorScheme.onPrimary,
                                    indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f)
                                )
                            )
                            NavigationBarItem(
                                selected = destination is StudentDestination.Courses,
                                onClick = {
                                    destination = StudentDestination.Courses(safeSubjectIndex, safeCourseIndex)
                                },
                                icon = { Icon(Icons.Default.Person, contentDescription = "Courses", modifier = Modifier.size(20.dp)) },
                                label = { Text("Courses", style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    unselectedTextColor = MaterialTheme.colorScheme.onPrimary,
                                    indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f)
                                )
                            )
                            NavigationBarItem(
                                selected = destination is StudentDestination.Shop,
                                onClick = { destination = StudentDestination.Shop },
                                icon = { Icon(Icons.Default.BookmarkBorder, contentDescription = "Shop", modifier = Modifier.size(20.dp)) },
                                label = { Text("Shop", style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    unselectedTextColor = MaterialTheme.colorScheme.onPrimary,
                                    indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f)
                                )
                            )
                            NavigationBarItem(
                                selected = destination is StudentDestination.Leaderboards,
                                onClick = { destination = StudentDestination.Leaderboards },
                                icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Leaderboards", modifier = Modifier.size(20.dp)) },
                                label = { Text("Leaders", style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    unselectedTextColor = MaterialTheme.colorScheme.onPrimary,
                                    indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f)
                                )
                            )
                            NavigationBarItem(
                                selected = destination is StudentDestination.Profile,
                                onClick = { destination = StudentDestination.Profile },
                                icon = { Icon(Icons.Default.Bookmark, contentDescription = "Profile", modifier = Modifier.size(20.dp)) },
                                label = { Text("Profile", style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    unselectedTextColor = MaterialTheme.colorScheme.onPrimary,
                                    indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f)
                                )
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
                                destination = StudentDestination.Courses(subjectIndex, courseIndex)
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
                            },
                            onQuizAnswered = {
                                refreshSubjects()
                            }
                        )

                        is StudentDestination.Courses -> StudentProfileScreen(
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
                                destination = StudentDestination.Courses(newSubjectIndex, 0)
                            },
                            onSwitchCourse = { newCourseIndex ->
                                selectedCourseIndex = newCourseIndex
                                destination = StudentDestination.Courses(selectedSubjectIndex, newCourseIndex)
                            },
                            onLogout = onLogout
                        )

                        StudentDestination.Shop -> ShopScreen(
                            modifier = Modifier.padding(innerPadding),
                            profile = userProfile,
                            onPurchaseTheme = { themeId, cost ->
                                val purchased = withContext(Dispatchers.IO) { repo.purchaseTheme(themeId, cost) }
                                if (purchased) {
                                    refreshSubjects()
                                    snackbarHostState.showSnackbar("Theme unlocked")
                                } else {
                                    snackbarHostState.showSnackbar("Not enough SkillBux")
                                }
                            },
                            onApplyTheme = { themeId ->
                                withContext(Dispatchers.IO) { repo.applyTheme(themeId) }
                                onThemeChanged(themeId)
                                refreshSubjects()
                                snackbarHostState.showSnackbar("Theme applied")
                            },
                            onPurchaseAvatar = { avatar, cost ->
                                val purchased = withContext(Dispatchers.IO) { repo.purchaseAvatar(avatar, cost) }
                                if (purchased) {
                                    refreshSubjects()
                                    snackbarHostState.showSnackbar("Avatar unlocked")
                                } else {
                                    snackbarHostState.showSnackbar("Not enough SkillBux")
                                }
                            },
                            onApplyAvatar = { avatar ->
                                withContext(Dispatchers.IO) { repo.applyAvatar(avatar) }
                                refreshSubjects()
                                snackbarHostState.showSnackbar("Avatar applied")
                            },
                            onPurchaseNameIcon = { icon, cost ->
                                val purchased = withContext(Dispatchers.IO) { repo.purchaseNameIcon(icon, cost) }
                                if (purchased) {
                                    refreshSubjects()
                                    snackbarHostState.showSnackbar("Name icon unlocked")
                                } else {
                                    snackbarHostState.showSnackbar("Not enough SkillBux")
                                }
                            },
                            onApplyNameIcon = { icon ->
                                withContext(Dispatchers.IO) { repo.applyNameIcon(icon) }
                                refreshSubjects()
                                snackbarHostState.showSnackbar("Name icon applied")
                            }
                        )

                        StudentDestination.Profile -> StudentAccountProfileScreen(
                            modifier = Modifier.padding(innerPadding),
                            userEmail = userEmail,
                            displayName = currentUser?.displayName.orEmpty(),
                            savedLessonsCount = userProfile.watchLaterCount,
                            totalCoursesCount = totalCoursesCount,
                            totalLessonsCount = totalLessonsCount,
                            level = userProfile.level,
                            xp = userProfile.xp,
                            streak = userProfile.currentStreak,
                            skillBux = userProfile.skillBux,
                            avatar = userProfile.selectedAvatar,
                            nameIcon = userProfile.selectedNameIcon,
                            onLogout = onLogout
                        )

                        StudentDestination.Leaderboards -> LeaderboardsScreen(
                            modifier = Modifier.padding(innerPadding),
                            leaderboardData = leaderboardData
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
    onRefreshAfterWatchLater: suspend () -> Unit,
    onQuizAnswered: suspend () -> Unit
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
            Text("No lessons found")
        }
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
                    },
                    onQuizAnswered = {
                        scope.launch {
                            onQuizAnswered()
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
            color = MaterialTheme.colorScheme.onPrimary,
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
            color = MaterialTheme.colorScheme.onPrimary,
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
    onToggleWatchLater: (StudentFeedLesson, StudentFeedCourse) -> Unit,
    onQuizAnswered: suspend () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        VideoBackground(videoUrl = item.lesson.videoUrl)

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
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = item.lesson.lessonTitle,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            Text(
                text = subjectName,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (item.course.courseDescription.isNotBlank()) {
                Text(
                    text = item.course.courseDescription,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
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
                lessonId = item.lesson.lessonId,
                quiz = item.lesson.quiz,
                state = quizState,
                modifier = Modifier.align(Alignment.Center),
                onQuizAnswered = onQuizAnswered
            )
        }
    }
}

@Composable
private fun QuizOverlay(
    lessonId: String,
    quiz: StudentQuiz,
    state: QuizUiState,
    modifier: Modifier = Modifier,
    onQuizAnswered: suspend () -> Unit
) {
    val scope = rememberCoroutineScope()
    val repo = remember { FirestoreRepository() }
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
                    onClick = {
                        if (state.submitted) {
                            state.isVisible = false
                            state.submitted = false
                            state.selectedAnswerIndex = null
                            return@Button
                        }

                        state.submitted = true
                        val isCorrect = state.selectedAnswerIndex == quiz.correctAnswerIndex

                        scope.launch {
                            repo.recordQuizAnswer(
                                lessonId = lessonId,
                                isCorrect = isCorrect
                            )
                            onQuizAnswered()
                        }
                    },
                    enabled = state.submitted || quiz.correctAnswerIndex == null || state.selectedAnswerIndex != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (state.submitted) "Done" else "Submit")
                }
            }
        }
    }
}

@Composable
private fun VideoBackground(videoUrl: String) {
    // keeps track of whether the video is paused or playing
    // resets if the video URL changes
    var isPaused by remember(videoUrl) { mutableStateOf(false) }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            // tap anywhere on the screen to pause/play the video
            .clickable {
                isPaused = !isPaused
            },
        factory = { context ->
            VideoView(context).apply {
                // convert the string URL into a URI so VideoView can use it
                val uri = Uri.parse(videoUrl)

                // store the current video URL so we can compare later
                tag = videoUrl

                // set the video source
                setVideoURI(uri)

                // runs when the video is ready to play
                setOnPreparedListener { mediaPlayer ->
                    // loop the video so it keeps playing in the background
                    mediaPlayer.isLooping = true

                    // only start if it's not paused
                    if (!isPaused) {
                        start()
                    }
                }
            }
        },
        update = { videoView ->
            // if the video URL changed, reload the video
            if (videoView.tag != videoUrl) {
                val uri = Uri.parse(videoUrl)

                // update tag to new URL
                videoView.tag = videoUrl

                // set new video source
                videoView.setVideoURI(uri)

                // same setup as before for looping and auto-play
                videoView.setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.isLooping = true
                    if (!isPaused) {
                        videoView.start()
                    }
                }
            }

            // handle pause/play based on current state
            if (isPaused) {
                // pause only if it's currently playing
                if (videoView.isPlaying) {
                    videoView.pause()
                }
            } else {
                // start only if it's not already playing
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
                text = "Courses",
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
                    color = MaterialTheme.colorScheme.onPrimary,
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
            Text(
                text = "Subject progress: ${subject.completedCourses}/${subject.totalCourses} courses completed (${subject.completionPercent}%)",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (!course?.courseDescription.isNullOrBlank()) {
                Text(text = course?.courseDescription.orEmpty(), modifier = Modifier.padding(top = 8.dp))
            }
            Text(
                text = "Course progress: ${course?.completedLessons ?: 0}/${course?.totalLessons ?: 0} lessons completed (${course?.completionPercent ?: 0}%)",
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = Color.Transparent,
                        labelColor = MaterialTheme.colorScheme.onBackground
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = index == safeCourseIndex,
                        borderColor = MaterialTheme.colorScheme.secondary,
                        selectedBorderColor = MaterialTheme.colorScheme.secondary,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 1.dp
                    )
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
                        AsyncImage(
                            // Load the lesson thumbnail from the URL stored in Firestore
                            model = lesson.thumbnailUrl,
                            contentDescription = lesson.lessonTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            // Show a default cover image while loading or if the URL is invalid
                            placeholder = painterResource(id = com.example.skillsync.R.drawable.example_cover),
                            error = painterResource(id = com.example.skillsync.R.drawable.example_cover)
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
                            color = MaterialTheme.colorScheme.onPrimary,
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
                    label = { Text(subject.subjectName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = Color.Transparent,
                        labelColor = MaterialTheme.colorScheme.onBackground
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = index == selectedSubjectIndex,
                        borderColor = MaterialTheme.colorScheme.secondary,
                        selectedBorderColor = MaterialTheme.colorScheme.secondary,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 1.dp
                    )
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




@Composable
private fun StudentAccountProfileScreen(
    modifier: Modifier = Modifier,
    userEmail: String,
    displayName: String,
    savedLessonsCount: Int,
    totalCoursesCount: Int,
    totalLessonsCount: Int,
    level: Int,
    xp: Int,
    streak: Int,
    skillBux: Int,
    avatar: String,
    nameIcon: String,
    onLogout: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = avatar,
                        color = Color.Black,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = listOf(nameIcon, if (displayName.isBlank()) "Student" else displayName)
                        .filter { it.isNotBlank() }
                        .joinToString(" "),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(top = 12.dp)
                )

                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            userScrollEnabled = false
        ) {
            item {
                val xpProgress = remember(xp) {
                    var currentLevel = 1
                    var xpNeededForThisLevel = 10
                    var remainingXp = xp

                    while (currentLevel < 20 && remainingXp >= xpNeededForThisLevel) {
                        remainingXp -= xpNeededForThisLevel
                        currentLevel++
                        xpNeededForThisLevel += 10
                    }

                    remainingXp to xpNeededForThisLevel
                }

                ProfileStatCard(
                    title = "XP",
                    value = "${xpProgress.first}/${xpProgress.second}"
                )
            }
            item {
                ProfileStatCard(title = "Level", value = level.toString())
            }
            item {
                ProfileStatCard(title = "Streak", value = streak.toString())
            }
            item {
                ProfileStatCard(title = "Saved", value = savedLessonsCount.toString())
            }
            item {
                ProfileStatCard(title = "SkillBux", value = skillBux.toString())
            }

        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                onLogout()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }
    }
}

@Composable
private fun ShopScreen(
    modifier: Modifier = Modifier,
    profile: UserProfile,
    onPurchaseTheme: suspend (String, Int) -> Unit,
    onApplyTheme: suspend (String) -> Unit,
    onPurchaseAvatar: suspend (String, Int) -> Unit,
    onApplyAvatar: suspend (String) -> Unit,
    onPurchaseNameIcon: suspend (String, Int) -> Unit,
    onApplyNameIcon: suspend (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val themes = listOf(
        ShopItem("dark_red", "Midnight Red", "Dark black + red", 0, "dark_red" in profile.purchasedThemes, profile.selectedTheme == "dark_red"),
        ShopItem("dark_blue", "Midnight Blue", "Dark black + blue", 40, "dark_blue" in profile.purchasedThemes, profile.selectedTheme == "dark_blue"),
        ShopItem("dark_green", "Midnight Green", "Dark black + green", 40, "dark_green" in profile.purchasedThemes, profile.selectedTheme == "dark_green"),
        ShopItem("light_red", "Ivory Red", "Light white + red", 60, "light_red" in profile.purchasedThemes, profile.selectedTheme == "light_red"),
        ShopItem("light_blue", "Ivory Blue", "Light white + blue", 60, "light_blue" in profile.purchasedThemes, profile.selectedTheme == "light_blue"),
        ShopItem("light_green", "Ivory Green", "Light white + green", 60, "light_green" in profile.purchasedThemes, profile.selectedTheme == "light_green")
    )
    val avatars = listOf("🙂" to 0, "😎" to 20, "🧠" to 25, "👑" to 35, "🚀" to 35, "🐯" to 45)
    val nameIcons = listOf("🔥" to 20, "⚡" to 20, "👑" to 30, "🎯" to 25, "🏆" to 35)

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Shop", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text("SkillBux balance", color = Color.Black.copy(alpha = 0.8f), modifier = Modifier.padding(top = 4.dp))
                    Text(profile.skillBux.toString(), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        item { ShopSectionTitle("Themes") }
        items(themes) { item ->
            ShopActionCard(
                title = item.title,
                subtitle = item.subtitle,
                trailing = if (item.owned) if (item.applied) "Applied" else "Apply" else "${item.cost} SkillBux",
                onClick = {
                    scope.launch {
                        if (item.owned) onApplyTheme(item.id) else onPurchaseTheme(item.id, item.cost)
                    }
                }
            )
        }
        item { ShopSectionTitle("Profile Pictures") }
        items(avatars) { (avatar, cost) ->
            val owned = avatar in profile.purchasedAvatars
            val applied = profile.selectedAvatar == avatar
            ShopActionCard(
                title = "$avatar Avatar",
                subtitle = if (owned) "Unlocked" else "Unlock for $cost SkillBux",
                trailing = if (owned) if (applied) "Applied" else "Apply" else "Buy",
                onClick = {
                    scope.launch {
                        if (owned) onApplyAvatar(avatar) else onPurchaseAvatar(avatar, cost)
                    }
                }
            )
        }
        item { ShopSectionTitle("Name Icons") }
        items(nameIcons) { (icon, cost) ->
            val owned = icon in profile.purchasedNameIcons
            val applied = profile.selectedNameIcon == icon
            ShopActionCard(
                title = "$icon Name Icon",
                subtitle = if (owned) "Unlocked" else "Unlock for $cost SkillBux",
                trailing = if (owned) if (applied) "Applied" else "Apply" else "Buy",
                onClick = {
                    scope.launch {
                        if (owned) onApplyNameIcon(icon) else onPurchaseNameIcon(icon, cost)
                    }
                }
            )
        }
    }
}

@Composable
private fun ShopSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun ShopActionCard(
    title: String,
    subtitle: String,
    trailing: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                Text(subtitle, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f), modifier = Modifier.padding(top = 4.dp))
            }
            Button(onClick = onClick) {
                Text(trailing)
            }
        }
    }
}

@Composable
private fun LeaderboardsScreen(
    modifier: Modifier = Modifier,
    leaderboardData: LeaderboardData
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Leaderboards",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        item {
            LeaderboardCard(
                title = "Highest Streak",
                subtitle = "Top right-answer streaks",
                entries = leaderboardData.topStreakUsers,
                valueLabel = "streak"
            )
        }

        item {
            LeaderboardCard(
                title = "Highest Level",
                subtitle = "Top current levels",
                entries = leaderboardData.topLevelUsers,
                valueLabel = "level"
            )
        }

        item {
            LeaderboardCard(
                title = "Most XP Today",
                subtitle = "Top XP earned in a single day",
                entries = leaderboardData.topDailyXpUsers,
                valueLabel = "XP"
            )
        }
    }
}

@Composable
private fun LeaderboardCard(
    title: String,
    subtitle: String,
    entries: List<com.example.skillsync.models.LeaderboardEntry>,
    valueLabel: String
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
            )

            if (entries.isEmpty()) {
                Text(
                    text = "No data yet",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                entries.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}.",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(28.dp)
                        )
                        Text(
                            text = entry.name,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${entry.value} $valueLabel",
                            color = Color(0xFFFFD54F),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatCard(
    title: String,
    value: String
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.secondary),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}