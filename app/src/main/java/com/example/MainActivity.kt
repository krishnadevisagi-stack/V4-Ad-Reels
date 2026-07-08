package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AdViewModel
import com.example.viewmodel.WalletViewModel
import com.google.android.gms.ads.MobileAds

import com.example.data.adengine.SecurityAndAntiFraudManager
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info

class MainActivity : ComponentActivity() {
    
    private val viewModel: AdViewModel by viewModels()
    private val walletViewModel: WalletViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SecurityAndAntiFraudManager.init(applicationContext)
        MobileAds.initialize(this) {}
        com.example.data.firebase.FirebaseManager.init(this)
        setContent {
            val isDarkTheme by viewModel.isDarkMode.collectAsState()
            MyApplicationTheme(darkTheme = isDarkTheme) {
                val securityStatus by viewModel.securityStatus.collectAsState()

                // Complete full-screen block is ONLY for modified packages / signature integrity compromise.
                if (securityStatus != null && securityStatus!!.isAppModified) {
                    SecurityBlockScreen(
                        threats = securityStatus!!,
                        onRescan = { viewModel.runSecurityCheck() }
                    )
                } else {
                    // Update non-blocking VPN/Proxy/AdBlock status
                    LaunchedEffect(securityStatus) {
                        securityStatus?.let {
                            SecurityAndAntiFraudManager.updateSecurityStatus(it)
                        }
                    }

                    // Floating Warn / Dialog Overlays for non-blocking security alerts
                    val rewardsSuspensionMsg by SecurityAndAntiFraudManager.rewardsSuspensionDetails.collectAsState()
                    val suspiciousWarningMsg by SecurityAndAntiFraudManager.suspiciousWarningMsg.collectAsState()
                    val fraudBlockMsg by SecurityAndAntiFraudManager.fraudBlockMsg.collectAsState()

                    var showSuspensionDialog by remember { mutableStateOf(false) }
                    var showSuspiciousDialog by remember { mutableStateOf(false) }
                    var showFraudBlockDialog by remember { mutableStateOf(false) }

                    LaunchedEffect(rewardsSuspensionMsg) {
                        showSuspensionDialog = rewardsSuspensionMsg != null
                    }
                    LaunchedEffect(suspiciousWarningMsg) {
                        showSuspiciousDialog = suspiciousWarningMsg != null
                    }
                    LaunchedEffect(fraudBlockMsg) {
                        showFraudBlockDialog = fraudBlockMsg != null
                    }

                    if (showSuspensionDialog && rewardsSuspensionMsg != null) {
                        AlertDialog(
                            onDismissRequest = { showSuspensionDialog = false },
                            icon = { Icon(Icons.Default.Warning, contentDescription = "Security Alert", tint = MaterialTheme.colorScheme.error) },
                            title = { Text("Rewards Suspended", fontWeight = FontWeight.Bold) },
                            text = { Text(rewardsSuspensionMsg!!) },
                            confirmButton = {
                                Button(
                                    onClick = { showSuspensionDialog = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("I Understand")
                                }
                            }
                        )
                    }

                    if (showSuspiciousDialog && suspiciousWarningMsg != null) {
                        AlertDialog(
                            onDismissRequest = { SecurityAndAntiFraudManager.clearWarning() },
                            icon = { Icon(Icons.Default.Warning, contentDescription = "Spam Warning", tint = MaterialTheme.colorScheme.primary) },
                            title = { Text("Suspicious Click Warning", fontWeight = FontWeight.Bold) },
                            text = { Text(suspiciousWarningMsg!!) },
                            confirmButton = {
                                Button(
                                    onClick = { SecurityAndAntiFraudManager.clearWarning() }
                                ) {
                                    Text("OK")
                                }
                            }
                        )
                    }

                    if (showFraudBlockDialog && fraudBlockMsg != null) {
                        AlertDialog(
                            onDismissRequest = { /* force acknowledge, don't auto-dismiss */ },
                            icon = { Icon(Icons.Default.Info, contentDescription = "Access Restricted", tint = MaterialTheme.colorScheme.error) },
                            title = { Text("Ad-View Access Restricted", fontWeight = FontWeight.Bold) },
                            text = { Text(fraudBlockMsg!!) },
                            confirmButton = {
                                Button(
                                    onClick = { SecurityAndAntiFraudManager.clearBlockMessage() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Acknowledge")
                                }
                            }
                        )
                    }

                    NavigationHost(viewModel = viewModel, walletViewModel = walletViewModel)
                }
            }
        }
    }
}

@Composable
fun NavigationHost(
    viewModel: AdViewModel,
    walletViewModel: WalletViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val userProfile by viewModel.userProfile.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = modifier
    ) {
        composable("splash") {
            SplashScreen(
                viewModel = viewModel,
                onNavigateNext = { targetRoute ->
                    navController.navigate(targetRoute) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            LoginScreen(
                viewModel = viewModel,
                onNavigateToInterests = { username, email, hasInterests ->
                    if (hasInterests) {
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        navController.navigate("interests/$username/$email")
                    }
                }
            )
        }

        composable(
            route = "interests/{username}/{email}",
            arguments = listOf(
                navArgument("username") { type = NavType.StringType },
                navArgument("email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            val email = backStackEntry.arguments?.getString("email") ?: ""
            InterestSelectionScreen(
                username = username,
                email = email,
                viewModel = viewModel,
                onComplete = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("disclaimer") {
            DisclaimerScreen(
                viewModel = viewModel,
                onProceed = {
                    navController.navigate("main") {
                        popUpTo("disclaimer") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            // If user somehow gets logged out, redirect them automatically
            val loggedIn = userProfile?.isLoggedIn ?: false
            LaunchedEffect(userProfile) {
                if (userProfile == null || !loggedIn) {
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            }
            if (userProfile != null && loggedIn) {
                MainScreenContainer(viewModel = viewModel, walletViewModel = walletViewModel)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreenContainer(
    viewModel: AdViewModel,
    walletViewModel: WalletViewModel
) {
    var selectedTab by remember { mutableStateOf("home") }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("main_bottom_nav_bar")
            ) {
                // Tab 1: Home
                NavigationBarItem(
                    selected = selectedTab == "home",
                    onClick = { selectedTab = "home" },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == "home") Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_home")
                )

                // Tab 2: Reels
                NavigationBarItem(
                    selected = selectedTab == "reels",
                    onClick = { selectedTab = "reels" },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == "reels") Icons.Filled.Movie else Icons.Outlined.Movie,
                            contentDescription = "Reels"
                        )
                    },
                    label = { Text("Reels", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_reels")
                )

                // Tab 3: Profile
                NavigationBarItem(
                    selected = selectedTab == "profile",
                    onClick = { selectedTab = "profile" },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == "profile") Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "Profile"
                        )
                    },
                    label = { Text("Profile", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_profile")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when (selectedTab) {
                "home" -> HomeScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                )
                "reels" -> ReelsScreen(
                    adViewModel = viewModel,
                    modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                )
                "profile" -> ProfileScreen(
                    viewModel = viewModel,
                    walletViewModel = walletViewModel,
                    modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                )
            }
        }
    }
}

