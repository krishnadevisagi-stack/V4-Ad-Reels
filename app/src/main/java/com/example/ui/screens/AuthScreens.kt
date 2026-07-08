package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.AdViewModel
import com.example.ui.components.SponzaLogo
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

@Composable
fun LoginScreen(
    viewModel: AdViewModel,
    onNavigateToInterests: (username: String, email: String, hasInterests: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf("") }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Real Google Sign-In launcher integration
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val idToken = account.idToken
                val email = account.email ?: ""
                val displayName = account.displayName ?: ""

                if (email.isNotEmpty()) {
                    if (idToken != null) {
                        val credential = GoogleAuthProvider.getCredential(idToken, null)
                        FirebaseAuth.getInstance().signInWithCredential(credential)
                            .addOnCompleteListener { authTask ->
                                // Always log in using their real selected Gmail account from the device!
                                viewModel.signInWithGoogleUser(
                                    email = email,
                                    displayName = displayName,
                                    onSuccess = { profile ->
                                        Toast.makeText(context, "Welcome, ${profile.fullName}!", Toast.LENGTH_SHORT).show()
                                        onNavigateToInterests(profile.username, profile.email, profile.selectedCategories.isNotBlank())
                                    },
                                    onError = { err ->
                                        errorMessage = err
                                    }
                                )
                            }
                    } else {
                        viewModel.signInWithGoogleUser(
                            email = email,
                            displayName = displayName,
                            onSuccess = { profile ->
                                Toast.makeText(context, "Welcome, ${profile.fullName}!", Toast.LENGTH_SHORT).show()
                                onNavigateToInterests(profile.username, profile.email, profile.selectedCategories.isNotBlank())
                            },
                            onError = { err ->
                                errorMessage = err
                            }
                        )
                    }
                } else {
                    errorMessage = "Failed to retrieve Google account email."
                }
            } catch (e: Exception) {
                errorMessage = "Google Play Services Error: ${e.message}"
            }
        } else {
            // User cancelled or Play Services error, show a message so they can select it again
            errorMessage = "Google Sign-In was cancelled or failed. Please select your account again."
        }
    }

    val isDarkTheme by viewModel.isDarkMode.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(36.dp))

            // Elegant Gold Badge Hero Logo
            SponzaLogo(size = 120.dp, transparent = true)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Sponza",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            Text(
                text = "Watch Relevant Ads. Earn Rewards. Transparently.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 48.dp)
            )

            // High Fidelity Real/Simulated Google Sign-In Button
            Button(
                onClick = {
                    try {
                        val clientIdResId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                        val clientId = if (clientIdResId != 0) context.getString(clientIdResId) else null
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .apply {
                                if (clientId != null) {
                                    requestIdToken(clientId)
                                }
                            }
                            .requestEmail()
                            .build()
                        val signInClient = GoogleSignIn.getClient(context, gso)
                        
                        // Clear previous sign-in states to guarantee choice of Gmail accounts
                        signInClient.signOut().addOnCompleteListener {
                            val intent = signInClient.signInIntent
                            googleSignInLauncher.launch(intent)
                        }
                    } catch (e: Exception) {
                        errorMessage = "Error initializing Google Sign-In: ${e.localizedMessage ?: "Please try again."}"
                    }
                },
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("google_login_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "G",
                            color = Color(0xFF4285F4),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Sign in with Google",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Error Message UI Feedback
            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Terms of Use & Privacy Policy Footer Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Privacy Policy",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .clickable { showPrivacyDialog = true }
                        .padding(6.dp)
                )
                Text(
                    text = "•",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Text(
                    text = "Terms of Use",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .clickable { showTermsDialog = true }
                        .padding(6.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = {
                Text("Privacy Policy", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            },
            text = {
                Box(modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Sponza respects your local privacy. In this offline prototype version, all data, including your passwords, name, mobile number, chosen categories, watched advertisements statistics, and wallet coins histories, are fully securely encrypted/hashed and stored locally on your physical device. We do not transmit, analyze, or share any of your private statistics, preferences, or activities with third parties.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Terms of Use Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = {
                Text("Terms of Use", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            },
            text = {
                Box(modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        text = "By accessing Sponza, you acknowledge that this is a simulated reward prototype app. All earned coins, cashback balances, vouchers, and UPI withdrawals listed on this platform are for demonstration and product evaluation purposes. These balances hold no real-world financial value and cannot be redeemed for legal currency outside this prototype environment.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) {
                    Text("I Understand", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun InterestSelectionScreen(
    username: String,
    email: String,
    viewModel: AdViewModel,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoriesList = listOf(
        "Gaming", "Education", "Technology", "Finance", "Business", "Fashion",
        "Fitness", "Automobile", "Travel", "Food", "Movies", "Music",
        "Sports", "Health", "Books", "News"
    )

    val selectedCategories = remember { mutableStateListOf<String>() }
    var errorMsg by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome, $username!",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Select your interests to curate your rewarded advertisement feed. Select at least 3 categories to get a 200 Coin Signup Bonus!",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(categoriesList) { category ->
                    val isSelected = selectedCategories.contains(category)
                    InterestPillItem(
                        category = category,
                        isSelected = isSelected,
                        onToggle = {
                            if (isSelected) {
                                selectedCategories.remove(category)
                            } else {
                                selectedCategories.add(category)
                            }
                            if (selectedCategories.size >= 3) {
                                errorMsg = ""
                            }
                        }
                    )
                }
            }

            if (errorMsg.isNotEmpty()) {
                Text(
                    text = errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (selectedCategories.size < 3) {
                        errorMsg = "Please choose at least 3 interests to continue."
                    } else if (selectedCategories.size > 10) {
                        errorMsg = "You can select up to 10 interests maximum."
                    } else {
                        viewModel.registerUser(username, email, selectedCategories.toList())
                        onComplete()
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("interests_continue_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "Let's Earn Rewards",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun InterestPillItem(
    category: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onToggle() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = category,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected Icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
