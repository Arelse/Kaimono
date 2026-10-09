package eu.kanade.tachiyomi.ui.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.MultipartBody
import okhttp3.Request
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import eu.kanade.tachiyomi.data.auth.AuthManager
import eu.kanade.domain.ui.UiPreferences
import tachiyomi.presentation.core.util.collectAsState

private enum class AuthMode { SIGN_IN, SIGN_UP }

class ProfileScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val authManager: AuthManager = Injekt.get()
        val user by authManager.currentUser.collectAsState()

        val webClientId = "997612260567-i7gkfnks53c0tlh9kvfslmml0bnn0lle.apps.googleusercontent.com"

        if (user != null) {
            val highResPhotoUrl = user?.photoUrl?.toString()?.replace("s96-c", "s800-c")
            val profileDisplayName = user?.displayName?.takeIf { it.isNotBlank() }
                ?: if (user?.isAnonymous == true) "Guest" else "Unknown User"

            var stats by remember { mutableStateOf(ProfileStats.EMPTY) }
            var statsLoading by remember { mutableStateOf(true) }
            LaunchedEffect(user?.uid) {
                statsLoading = true
                stats = computeProfileStats()
                statsLoading = false
                syncLeaderboardEntry(stats.totalChaptersRead, stats.currentStreak)
            }

            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                AsyncImage(
                    model = highResPhotoUrl,
                    contentDescription = "Profile Background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.15f to Color.Black.copy(alpha = 0.6f),
                                0.35f to Color.Black,
                                1.0f to Color.Black
                            )
                        )
                ) {}

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { navigator.pop() },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { navigator.push(LeaderboardScreen()) },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = "Leaderboard", tint = Color.White)
                        }
                        Box {
                            var showOptionsMenu by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = { showOptionsMenu = true },
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.MoreHoriz, contentDescription = "Options", tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = showOptionsMenu,
                                onDismissRequest = { showOptionsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Sign Out") },
                                    onClick = {
                                        showOptionsMenu = false
                                        scope.launch {
                                            eu.kanade.tachiyomi.data.sync.CloudSyncManager.syncUp(context)
                                            authManager.signOut()
                                            navigator.pop()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 100.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val photoModel = user?.photoUrl?.toString()?.replace("s96-c", "s192-c")
                        Box(
                            modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (photoModel != null) {
                                AsyncImage(
                                    model = photoModel,
                                    contentDescription = "Profile Picture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = profileDisplayName.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.headlineSmall,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = profileDisplayName,
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            if (user?.isAnonymous == true) {
                                Text(
                                    text = "Playing as guest",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f),
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        var showEditDialog by remember { mutableStateOf(false) }
                        Button(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                            shape = RoundedCornerShape(25.dp)
                        ) {
                            Text("Edit Profile", fontWeight = FontWeight.Bold)
                        }

                        var showSettingsDialog by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.size(50.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                        }

                        if (showSettingsDialog) {
                            val uiPreferences: UiPreferences = Injekt.get()
                            var showRecentlyRead by remember { mutableStateOf(uiPreferences.profileShowRecentlyRead.get()) }
                            var showGenres by remember { mutableStateOf(uiPreferences.profileShowGenres.get()) }

                            AlertDialog(
                                onDismissRequest = { showSettingsDialog = false },
                                title = { Text("Settings") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("Show recently read", modifier = Modifier.weight(1f))
                                            Switch(
                                                checked = showRecentlyRead,
                                                onCheckedChange = {
                                                    showRecentlyRead = it
                                                    uiPreferences.profileShowRecentlyRead.set(it)
                                                }
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("Show genres", modifier = Modifier.weight(1f))
                                            Switch(
                                                checked = showGenres,
                                                onCheckedChange = {
                                                    showGenres = it
                                                    uiPreferences.profileShowGenres.set(it)
                                                }
                                            )
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showSettingsDialog = false }) {
                                        Text("Done")
                                    }
                                }
                            )
                        }

                        if (showEditDialog) {
                            var newName by remember { mutableStateOf(user?.displayName ?: "") }
                            var newPhotoUrl by remember { mutableStateOf(user?.photoUrl?.toString() ?: "") }
                            var isUpdating by remember { mutableStateOf(false) }

                            val photoPickerLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.PickVisualMedia()
                            ) { uri ->
                                if (uri != null) {
                                    try {
                                        val inputStream = context.contentResolver.openInputStream(uri)
                                        val file = File(context.filesDir, "profile_pic_${System.currentTimeMillis()}.jpg")
                                        val outputStream = FileOutputStream(file)
                                        inputStream?.copyTo(outputStream)
                                        inputStream?.close()
                                        outputStream.close()
                                        newPhotoUrl = "file://${file.absolutePath}"
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            AlertDialog(
                                onDismissRequest = { if (!isUpdating) showEditDialog = false },
                                title = { Text("Edit Profile") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        OutlinedTextField(
                                            value = newName,
                                            onValueChange = { newName = it },
                                            label = { Text("Name") },
                                            singleLine = true
                                        )

                                        Column {
                                            Text("Profile Picture", style = MaterialTheme.typography.labelMedium)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            if (newPhotoUrl.isNotEmpty()) {
                                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                    AsyncImage(
                                                        model = newPhotoUrl,
                                                        contentDescription = "Preview",
                                                        modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(16.dp))
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    photoPickerLauncher.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Choose from Gallery")
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            isUpdating = true
                                            if (newPhotoUrl.startsWith("file://")) {
                                                val fileUri = android.net.Uri.parse(newPhotoUrl)
                                                val file = java.io.File(fileUri.path!!)
                                                val client = Injekt.get<NetworkHelper>().client

                                                GlobalScope.launch(Dispatchers.IO) {
                                                    try {
                                                        val requestBody = MultipartBody.Builder()
                                                            .setType(MultipartBody.FORM)
                                                            .addFormDataPart("reqtype", "fileupload")
                                                            .addFormDataPart("fileToUpload", file.name, file.asRequestBody("image/jpeg".toMediaTypeOrNull()))
                                                            .build()

                                                        val request = Request.Builder()
                                                            .url("https://catbox.moe/user/api.php")
                                                            .post(requestBody)
                                                            .build()

                                                        val response = client.newCall(request).execute()
                                                        val responseUrl = response.body?.string() ?: ""

                                                        withContext(Dispatchers.Main) {
                                                            if (response.isSuccessful && responseUrl.startsWith("http")) {
                                                                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                                                                    displayName = newName
                                                                    photoUri = android.net.Uri.parse(responseUrl)
                                                                }
                                                                user?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
                                                                    isUpdating = false
                                                                    showEditDialog = false
                                                                    if (!task.isSuccessful) {
                                                                        Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            } else {
                                                                isUpdating = false
                                                                Toast.makeText(context, "Failed to upload image to cloud", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        withContext(Dispatchers.Main) {
                                                            isUpdating = false
                                                            Toast.makeText(context, "Upload error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            } else {
                                                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                                                    displayName = newName
                                                    if (newPhotoUrl.isNotEmpty()) {
                                                        photoUri = android.net.Uri.parse(newPhotoUrl)
                                                    }
                                                }
                                                user?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
                                                    isUpdating = false
                                                    showEditDialog = false
                                                    if (!task.isSuccessful) {
                                                        Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isUpdating
                                    ) {
                                        if (isUpdating) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Saving...")
                                        } else {
                                            Text("Save")
                                        }
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = { showEditDialog = false },
                                        enabled = !isUpdating
                                    ) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(40.dp))

                    if (statsLoading) {
                        CircularProgressIndicator(color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(32.dp))
                    } else if (stats.itemsConsumed == 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .height(120.dp)
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Start reading to see your stats here",
                                color = Color.White.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        val uiPreferences: UiPreferences = Injekt.get()
                        val showRecentlyRead by uiPreferences.profileShowRecentlyRead.collectAsState()
                        val showGenres by uiPreferences.profileShowGenres.collectAsState()
                        ProfileStatsSection(stats, showRecentlyRead = showRecentlyRead, showGenres = showGenres)
                    }
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        } else {
            var authMode by remember { mutableStateOf(AuthMode.SIGN_IN) }
            var showEmailForm by remember { mutableStateOf(false) }
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var signUpName by remember { mutableStateOf("") }
            var isSubmitting by remember { mutableStateOf(false) }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Profile") },
                        navigationIcon = {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(Icons.Default.Close, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("You are not logged in.", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val credentialManager = CredentialManager.create(context)
                                    val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                                        .setFilterByAuthorizedAccounts(false)
                                        .setServerClientId(webClientId)
                                        .build()
                                    val request: GetCredentialRequest = GetCredentialRequest.Builder()
                                        .addCredentialOption(googleIdOption)
                                        .build()
                                    val result = credentialManager.getCredential(context, request)
                                    val credential = result.credential
                                    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                                        FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).addOnCompleteListener { task ->
                                            if (!task.isSuccessful) {
                                                android.util.Log.e("ProfileScreen", "Auth Failed", task.exception)
                                                Toast.makeText(context, "Firebase Auth Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                            } else {
                                                scope.launch { eu.kanade.tachiyomi.data.sync.CloudSyncManager.syncDown(context) }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("ProfileScreen", "Sign in failed", e)
                                    Toast.makeText(context, "Sign in failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                    ) {
                        Text("Sign in with Google")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    AuthDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    if (!showEmailForm) {
                        OutlinedButton(
                            onClick = { showEmailForm = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                        ) {
                            Text("Continue with Email")
                        }
                    } else {
                        if (authMode == AuthMode.SIGN_UP) {
                            OutlinedTextField(
                                value = signUpName,
                                onValueChange = { signUpName = it },
                                label = { Text("Display name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                if (email.isBlank() || password.isBlank()) {
                                    Toast.makeText(context, "Enter an email and password", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isSubmitting = true
                                val auth = FirebaseAuth.getInstance()
                                if (authMode == AuthMode.SIGN_UP) {
                                    auth.createUserWithEmailAndPassword(email.trim(), password)
                                        .addOnCompleteListener { task ->
                                            isSubmitting = false
                                            if (task.isSuccessful) {
                                                val newUser = task.result?.user
                                                if (signUpName.isNotBlank()) {
                                                    val req = com.google.firebase.auth.userProfileChangeRequest {
                                                        displayName = signUpName
                                                    }
                                                    newUser?.updateProfile(req)
                                                }
                                                scope.launch { eu.kanade.tachiyomi.data.sync.CloudSyncManager.syncDown(context) }
                                            } else {
                                                Toast.makeText(context, task.exception?.message ?: "Sign up failed", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                } else {
                                    auth.signInWithEmailAndPassword(email.trim(), password)
                                        .addOnCompleteListener { task ->
                                            isSubmitting = false
                                            if (!task.isSuccessful) {
                                                Toast.makeText(context, task.exception?.message ?: "Sign in failed", Toast.LENGTH_LONG).show()
                                            } else {
                                                scope.launch { eu.kanade.tachiyomi.data.sync.CloudSyncManager.syncDown(context) }
                                            }
                                        }
                                }
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text(if (authMode == AuthMode.SIGN_UP) "Create Account" else "Sign In")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = {
                            authMode = if (authMode == AuthMode.SIGN_UP) AuthMode.SIGN_IN else AuthMode.SIGN_UP
                        }) {
                            Text(
                                if (authMode == AuthMode.SIGN_UP) "Already have an account? Sign in" else "New here? Create an account"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    AuthDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            isSubmitting = true
                            FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener { task ->
                                isSubmitting = false
                                if (!task.isSuccessful) {
                                    Toast.makeText(context, task.exception?.message ?: "Guest sign-in failed", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isSubmitting,
                    ) {
                        Text("Continue as Guest")
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun AuthDivider() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.15f))
        Text(
            text = "or",
            color = Color.White.copy(alpha = 0.5f),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.15f))
    }
}

@Composable
private fun ProfileStatsSection(stats: ProfileStats, showRecentlyRead: Boolean = true, showGenres: Boolean = true) {
    val accent = MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        // Top row: three capsule-shaped pills (a deliberate shape variation from the plain
        // rounded-rect tiles below, rather than six identical boxes).
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatPill("Time Spent", formatReadDuration(stats.totalReadDuration), Modifier.weight(1f))
            StatPill("Titles Read", stats.itemsConsumed.toString(), Modifier.weight(1f))
            StatPill("Days Active", stats.daysActive.toString(), Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard("Chapters / day", "%.1f".format(stats.avgChaptersPerDay), Modifier.weight(1f))
            StatCard("Streak (cur / best)", "${stats.currentStreak}d / ${stats.longestStreak}d", Modifier.weight(1f))
        }

        stats.favoriteTitle?.let { fav ->
            Spacer(modifier = Modifier.height(28.dp))
            SectionLabel("Favorite title", accent)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                coil3.compose.AsyncImage(
                    model = fav.coverData,
                    contentDescription = fav.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(width = 48.dp, height = 64.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.1f)),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(fav.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 2)
                    Text(
                        text = "${formatReadDuration(fav.totalDuration)} spent \u00b7 ${fav.interactionCount}x",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        if (showRecentlyRead && stats.recentItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(28.dp))
            SectionLabel("Lately Alighted Upon", accent)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(stats.recentItems.take(10)) { item ->
                    Column(modifier = Modifier.width(100.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2f / 3f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.08f)),
                        ) {
                            coil3.compose.AsyncImage(
                                model = item.coverData,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(item.title, color = Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        Text(
                            text = "${if (item.isNovel) "Novel" else "Manga"} \u00b7 ${item.interactionCount}x",
                            color = Color.White.copy(alpha = 0.55f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }

        val showGenresGroup = showGenres && stats.topGenres.isNotEmpty()
        if (showGenresGroup || stats.topSources.isNotEmpty() || stats.typeBreakdown.isNotEmpty()) {
            Spacer(modifier = Modifier.height(28.dp))
            SectionLabel("Insights", accent)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (showGenresGroup) {
                    InsightGroup("Top Genres", stats.topGenres, accent)
                }
                if (stats.topSources.isNotEmpty()) {
                    InsightGroup("Top Sources", stats.topSources, accent)
                }
                if (stats.typeBreakdown.isNotEmpty()) {
                    InsightGroup("Type Breakdown", stats.typeBreakdown, accent)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, accent: Color) {
    Text(
        text = text.uppercase(),
        color = accent,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

@Composable
private fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(label, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(14.dp),
    ) {
        Text(label, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun InsightGroup(title: String, rows: List<Pair<String, Int>>, accent: Color) {
    val max = (rows.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    Column {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
        rows.forEach { (name, count) ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, color = Color.White, style = MaterialTheme.typography.bodySmall)
                Text("$count", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.1f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(count.toFloat() / max.toFloat())
                        .clip(RoundedCornerShape(3.dp))
                        .background(accent),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

 
                                     
