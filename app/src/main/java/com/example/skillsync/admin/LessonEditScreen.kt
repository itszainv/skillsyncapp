/**********************************************************
 * Created by: Asiyah Shoeb
 * Date: 4/16/26
 * Description: Screen for creating or editing lesson details including title, order, and video content.
 * Last Modified by: Asiyah Shoeb
 **********************************************************/

package com.example.skillsync.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.skillsync.data.FirestoreRepository
import com.example.skillsync.models.Lesson
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@Composable
fun LessonEditScreen(
    subjectId: String,
    courseId: String,
    lesson: Lesson? = null,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val repo = remember { FirestoreRepository() }
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(lesson?.title ?: "") }
    var order by remember { mutableStateOf(lesson?.order?.toString() ?: "1") }
    var videoUrl by remember { mutableStateOf(lesson?.videoUrl ?: "") }
    // State for the thumbnail URL, either uploaded to Firebase or manually entered
    var thumbnailUrl by remember { mutableStateOf(lesson?.thumbnailUrl ?: "") }
    var saving by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    var uploadStatus by remember { mutableStateOf("") }

    // Use white text on black background for the inputs
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        cursorColor = Color.White,
        focusedLabelColor = Color.White,
        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
        focusedBorderColor = Color.White,
        unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
    )

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            uploading = true
            uploadStatus = "Uploading video..."
            scope.launch {
                try {
                    val storageRef = FirebaseStorage.getInstance().reference
                        .child("lessons/videos/${UUID.randomUUID()}.mp4")
                    
                    val uploadTask = storageRef.putFile(it).await()
                    val downloadUrl = storageRef.downloadUrl.await()
                    videoUrl = downloadUrl.toString()
                    uploadStatus = "Video uploaded successfully!"
                } catch (e: Exception) {
                    uploadStatus = "Upload failed: ${e.localizedMessage}"
                } finally {
                    uploading = false
                }
            }
        }
    }

    val thumbnailPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            uploading = true
            uploadStatus = "Uploading thumbnail..."
            scope.launch {
                try {
                    // Upload the thumbnail image
                    val storageRef = FirebaseStorage.getInstance().reference
                        .child("lessons/thumbnails/${UUID.randomUUID()}.png")

                    storageRef.putFile(it).await()
                    val downloadUrl = storageRef.downloadUrl.await()
                    // Set the URL as the thumbnail
                    thumbnailUrl = downloadUrl.toString()
                    uploadStatus = "Thumbnail uploaded successfully!"
                } catch (e: Exception) {
                    uploadStatus = "Upload failed: ${e.localizedMessage}"
                } finally {
                    uploading = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Force background to Black
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, 
                    contentDescription = "Back", 
                    tint = Color.White
                )
            }
            Text(
                if (lesson == null) "New lesson" else "Edit lesson",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Lesson title") },
            colors = textFieldColors,
            textStyle = TextStyle(color = Color.White),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = order,
            onValueChange = { order = it },
            label = { Text("Display order") },
            colors = textFieldColors,
            textStyle = TextStyle(color = Color.White),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
        Spacer(Modifier.height(20.dp))

        // Video Content Section
        Text("Video Content", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium, color = Color.White)
        Text("Choose one method below", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
        
        Spacer(Modifier.height(16.dp))
        
        // Option 1: File Picker
        Text("Option 1: Upload from device", style = MaterialTheme.typography.labelMedium, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { videoPickerLauncher.launch("video/*") },
            enabled = !uploading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.padding(horizontal = 4.dp))
            Text(if (videoUrl.isEmpty()) "Select Video File" else "Replace Video File")
        }
        
        if (uploadStatus.isNotEmpty()) {
            Text(
                text = uploadStatus,
                color = if (uploadStatus.contains("failed")) Color.Red else Color.White,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(Modifier.height(16.dp))
        Text("— OR —", modifier = Modifier.align(Alignment.CenterHorizontally), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
        Spacer(Modifier.height(16.dp))

        // Option 2: Manual URL
        Text("Option 2: Enter URL manually", style = MaterialTheme.typography.labelMedium, color = Color.White)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = videoUrl,
            onValueChange = { videoUrl = it },
            label = { Text("Direct Video URL or res://raw/name") },
            placeholder = { Text("https://example.com/video.mp4") },
            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) },
            colors = textFieldColors,
            textStyle = TextStyle(color = Color.White),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
        Spacer(Modifier.height(20.dp))

        // Thumbnail Section
        Text("Lesson Thumbnail", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium, color = Color.White)
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { thumbnailPickerLauncher.launch("image/*") },
            enabled = !uploading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.padding(horizontal = 4.dp))
            Text(if (thumbnailUrl.isEmpty()) "Upload Thumbnail Image" else "Replace Thumbnail Image")
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = thumbnailUrl,
            onValueChange = { thumbnailUrl = it },
            label = { Text("Thumbnail URL") },
            placeholder = { Text("https://example.com/image.png") },
            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) },
            colors = textFieldColors,
            textStyle = TextStyle(color = Color.White),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                saving = true
                scope.launch {
                    repo.saveLesson(
                        subjectId, courseId,
                        Lesson(
                            id = lesson?.id ?: "",
                            title = title,
                            order = order.toIntOrNull() ?: 1,
                            videoUrl = videoUrl,
                            thumbnailUrl = thumbnailUrl,
                            components = lesson?.components ?: emptyList()
                        )
                    )
                    onSaved()
                }
            },
            enabled = title.isNotBlank() && !saving && !uploading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (saving) "Saving..." else "Save lesson")
        }
    }
}
