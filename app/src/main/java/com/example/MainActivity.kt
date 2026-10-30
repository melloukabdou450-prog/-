package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.repository.VehicleRepository
import com.example.ui.localization.LanguageManager
import com.example.ui.localization.ProvideAppLanguage
import com.example.ui.screens.AdminFraudScreen
import com.example.ui.screens.CustomerConfirmationScreen
import com.example.ui.screens.EstimateScreen
import com.example.ui.screens.GaragePortalScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InspectScreen
import com.example.ui.screens.InspectionResultScreen
import com.example.ui.screens.MarketDataScreen
import com.example.ui.screens.OwnerLogbookScreen
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.PublicReportScreen
import com.example.ui.screens.ReportScreen
import com.example.ui.screens.SellerPortalScreen
import com.example.ui.screens.TimelineScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

sealed interface Screen {
    object Home : Screen
    data class Timeline(val vin: String) : Screen
    data class Report(val vin: String) : Screen
    data class Inspect(val vin: String) : Screen
    data class InspectionResult(val vin: String, val inspectionId: String) : Screen
    data class Estimate(val vin: String) : Screen
    object MarketData : Screen
    object GaragePortal : Screen
    data class CustomerConfirm(val vin: String = "", val eventId: String = "", val code: String = "") : Screen
    object AdminFraud : Screen
    data class PublicReport(val publicId: String) : Screen
    object SellerPortal : Screen
    data class OwnerLogbook(val vin: String) : Screen
    object Privacy : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CarPassportApp()
            }
        }
    }
}

@Composable
fun CarPassportApp() {
    val context = LocalContext.current
    val languageManager = remember { LanguageManager() }
    val repository = remember { VehicleRepository(context) }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    // Maintain auth state reactivity
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    ProvideAppLanguage(languageManager = languageManager) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (val screen = currentScreen) {
                is Screen.Home -> {
                    HomeScreen(
                        repository = repository,
                        onNavigateToVehicle = { vin ->
                            currentScreen = Screen.Timeline(vin)
                        },
                        onNavigateToMarketData = {
                            currentScreen = Screen.MarketData
                        },
                        onNavigateToGaragePortal = {
                            currentScreen = Screen.GaragePortal
                        },
                        onNavigateToCustomerConfirm = {
                            currentScreen = Screen.CustomerConfirm()
                        },
                        onNavigateToAdminFraud = {
                            currentScreen = Screen.AdminFraud
                        },
                        onNavigateToPrivacy = {
                            currentScreen = Screen.Privacy
                        }
                    )
                }

                is Screen.Timeline -> {
                    BackHandler {
                        currentScreen = Screen.Home
                    }
                    TimelineScreen(
                        vin = screen.vin,
                        repository = repository,
                        onNavigateBack = { currentScreen = Screen.Home },
                        onNavigateToReport = { vin ->
                            currentScreen = Screen.Report(vin)
                        },
                        onNavigateToInspect = { vin ->
                            currentScreen = Screen.Inspect(vin)
                        },
                        onNavigateToInspectionResult = { vin, inspectionId ->
                            currentScreen = Screen.InspectionResult(vin, inspectionId)
                        },
                        onNavigateToEstimate = { vin ->
                            currentScreen = Screen.Estimate(vin)
                        }
                    )
                }

                is Screen.Report -> {
                    BackHandler {
                        currentScreen = Screen.Timeline(screen.vin)
                    }
                    ReportScreen(
                        vin = screen.vin,
                        repository = repository,
                        onNavigateBack = { currentScreen = Screen.Timeline(screen.vin) },
                        onNavigateToInspection = { vin ->
                            currentScreen = Screen.Inspect(vin)
                        },
                        onNavigateToInspectionResult = { vin, inspectionId ->
                            currentScreen = Screen.InspectionResult(vin, inspectionId)
                        },
                        onNavigateToEstimate = { vin ->
                            currentScreen = Screen.Estimate(vin)
                        }
                    )
                }

                is Screen.Inspect -> {
                    BackHandler {
                        currentScreen = Screen.Timeline(screen.vin)
                    }
                    InspectScreen(
                        vin = screen.vin,
                        vehicle = null,
                        repository = repository,
                        onBack = { currentScreen = Screen.Timeline(screen.vin) },
                        onInspectionCompleted = { inspectionId ->
                            currentScreen = Screen.InspectionResult(screen.vin, inspectionId)
                        }
                    )
                }

                is Screen.InspectionResult -> {
                    BackHandler {
                        currentScreen = Screen.Timeline(screen.vin)
                    }
                    InspectionResultScreen(
                        vin = screen.vin,
                        inspectionId = screen.inspectionId,
                        vehicle = null,
                        repository = repository,
                        onBack = { currentScreen = Screen.Timeline(screen.vin) },
                        onViewReport = { vin ->
                            currentScreen = Screen.Report(vin)
                        },
                        onViewTimeline = { vin ->
                            currentScreen = Screen.Timeline(vin)
                        }
                    )
                }

                is Screen.Estimate -> {
                    BackHandler {
                        currentScreen = Screen.Timeline(screen.vin)
                    }
                    EstimateScreen(
                        vin = screen.vin,
                        repository = repository,
                        onNavigateBack = { currentScreen = Screen.Timeline(screen.vin) },
                        onNavigateToReport = { vin ->
                            currentScreen = Screen.Report(vin)
                        }
                    )
                }

                is Screen.MarketData -> {
                    BackHandler {
                        currentScreen = Screen.Home
                    }
                    MarketDataScreen(
                        repository = repository,
                        onNavigateBack = { currentScreen = Screen.Home }
                    )
                }

                is Screen.GaragePortal -> {
                    BackHandler {
                        currentScreen = Screen.Home
                    }
                    GaragePortalScreen(
                        repository = repository,
                        onNavigateBack = { currentScreen = Screen.Home },
                        onNavigateToVehicle = { vin ->
                            currentScreen = Screen.Timeline(vin)
                        },
                        onNavigateToAdmin = {
                            currentScreen = Screen.AdminFraud
                        }
                    )
                }

                is Screen.CustomerConfirm -> {
                    BackHandler {
                        currentScreen = Screen.Home
                    }
                    CustomerConfirmationScreen(
                        initialVin = screen.vin,
                        initialEventId = screen.eventId,
                        initialCode = screen.code,
                        repository = repository,
                        onNavigateBack = { currentScreen = Screen.Home },
                        onNavigateToReport = { vin ->
                            currentScreen = Screen.Report(vin)
                        }
                    )
                }

                is Screen.AdminFraud -> {
                    BackHandler {
                        currentScreen = Screen.Home
                    }
                    AdminFraudScreen(
                        repository = repository,
                        onNavigateBack = { currentScreen = Screen.Home },
                        onNavigateToVehicle = { vin ->
                            currentScreen = Screen.Timeline(vin)
                        }
                    )
                }

                is Screen.PublicReport -> {
                    BackHandler {
                        currentScreen = Screen.Home
                    }
                    PublicReportScreen(
                        publicId = screen.publicId,
                        repository = repository,
                        onNavigateBack = { currentScreen = Screen.Home },
                        onNavigateToDispute = { vin, eventId ->
                            currentScreen = Screen.CustomerConfirm(vin, eventId)
                        }
                    )
                }

                is Screen.SellerPortal -> {
                    BackHandler {
                        currentScreen = Screen.Home
                    }
                    SellerPortalScreen(
                        repository = repository,
                        onNavigateBack = { currentScreen = Screen.Home },
                        onNavigateToVehicle = { vin ->
                            currentScreen = Screen.Timeline(vin)
                        },
                        onNavigateToPublicReport = { publicId ->
                            currentScreen = Screen.PublicReport(publicId)
                        }
                    )
                }

                is Screen.OwnerLogbook -> {
                    BackHandler {
                        currentScreen = Screen.Timeline(screen.vin)
                    }
                    OwnerLogbookScreen(
                        vin = screen.vin,
                        repository = repository,
                        onNavigateBack = { currentScreen = Screen.Timeline(screen.vin) }
                    )
                }

                is Screen.Privacy -> {
                    BackHandler {
                        currentScreen = Screen.Home
                    }
                    PrivacyPolicyScreen(
                        repository = repository,
                        onNavigateBack = { currentScreen = Screen.Home }
                    )
                }
            }
        }
    }
}
