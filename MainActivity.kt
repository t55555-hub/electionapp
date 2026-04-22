package com.splitsmart.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import kotlin.concurrent.thread
import kotlin.math.cos
import kotlin.math.sin

private val PrimaryBlue = Color(0xFF1A73E8)
private val BorderGray = Color(0xFFD9D9D9)
private val SurfaceGray = Color(0xFFF1F1F1)
private val DarkText = Color(0xFF111111)
private val TextGray = Color(0xFF8A8A8A)
private val BusyRed = Color(0xFFE53935)
private val WarningOrange = Color(0xFFFB8C00)
private val WarningYellow = Color(0xFFFDD835)
private val LightGreen = Color(0xFF8BC34A)
private val SuccessGreen = Color(0xFF43A047)

// Replace this with your real Google Civic API key
private const val CIVIC_API_KEY = "PASTE_YOUR_GOOGLE_CIVIC_API_KEY_HERE"

enum class RootScreen {
    LOGIN,
    APP
}

enum class AppTab {
    HOME,
    PLANNER,
    TRACKER,
    HUB,
    DOCS,
    DOC_DETAIL
}

data class ChecklistItem(
    val title: String,
    val subtitle: String,
    val done: Boolean
)

data class DocItem(
    val title: String,
    val body: String
)

data class PollingLocation(
    val name: String,
    val address: String,
    val notes: String
)

data class LookupResult(
    val pollingLocations: List<PollingLocation>,
    val earlyVoteSites: List<PollingLocation>,
    val dropOffLocations: List<PollingLocation>,
    val electionName: String?,
    val errorMessage: String?
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White
            ) {
                VoteApp()
            }
        }
    }
}

@Composable
fun VoteApp() {
    var rootScreen by remember { mutableStateOf(RootScreen.LOGIN) }
    var currentTab by remember { mutableStateOf(AppTab.HOME) }
    var loggedInUser by remember { mutableStateOf("User") }

    val checklist = remember {
        mutableStateListOf(
            ChecklistItem("Polling place", "123 N. Street Atlanta, Georgia, 1234", true),
            ChecklistItem("Photo ID", "Required, make sure you have it!", true),
            ChecklistItem("Review your selections", "Check out this elections ballot.", true)
        )
    }

    val plannerSlots = listOf(
        "7:00 AM - 8:59 AM",
        "9:00 AM - 11:59 AM",
        "12:00 PM - 2:59 PM",
        "3:00 PM - 5:59 PM"
    )

    var selectedPlannerSlot by remember { mutableIntStateOf(1) }

    val trackerSteps = listOf(
        "Request Ballot",
        "Ballot delivered",
        "Ballot submitted",
        "Received in system"
    )

    var trackerIndex by remember { mutableIntStateOf(3) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var lightMode by remember { mutableStateOf(true) }

    val docs = remember {
        listOf(
            DocItem("Incorrect details in ballot", "If your ballot information is incorrect, review your registration details and contact your election office."),
            DocItem("Report unethical ballot sites", "Use official county or state election resources whenever possible. Report suspicious sites to local election authorities."),
            DocItem("Errors in voting", "If you spot a voting error, document the issue, ask for assistance on site, and request official guidance."),
            DocItem("What to bring", "Bring your ID if required, registration details if applicable, and any instructions mailed to you."),
            DocItem("Denied ballot", "If your ballot was denied, review the reason listed and check whether a cure process is available."),
            DocItem("Voting process", "Check in, verify identity if needed, receive ballot, complete ballot, review, and submit."),
            DocItem("No near ballot places", "Use your county election website to verify assigned locations or mail ballot alternatives."),
            DocItem("No ballot received in mail", "Check ballot tracking first, then contact your local election office."),
            DocItem("Invalid ID", "Review accepted ID types for your state and county before visiting."),
            DocItem("Eligibility", "Eligibility depends on registration status, age, residency, and other local rules.")
        )
    }

    var selectedDoc by remember { mutableStateOf<DocItem?>(null) }

    when (rootScreen) {
        RootScreen.LOGIN -> {
            LoginScreen(
                onContinue = { email ->
                    val cleanName = email.substringBefore("@").trim()
                    loggedInUser = if (cleanName.isBlank()) {
                        "User"
                    } else {
                        cleanName.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase() else it.toString()
                        }
                    }
                    rootScreen = RootScreen.APP
                    currentTab = AppTab.HOME
                }
            )
        }

        RootScreen.APP -> {
            Scaffold(
                bottomBar = {
                    if (currentTab != AppTab.DOC_DETAIL) {
                        BottomBar(
                            currentTab = currentTab,
                            onTabChange = { currentTab = it }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        AppTab.HOME -> HomeScreen(
                            userName = loggedInUser,
                            items = checklist,
                            onToggleItem = { index ->
                                val current = checklist[index]
                                checklist[index] = current.copy(done = !current.done)
                            }
                        )

                        AppTab.PLANNER -> PlannerScreen(
                            slots = plannerSlots,
                            selectedIndex = selectedPlannerSlot,
                            onSelectSlot = { selectedPlannerSlot = it }
                        )

                        AppTab.TRACKER -> TrackerScreen(
                            trackerSteps = trackerSteps,
                            currentStep = trackerIndex,
                            onNext = {
                                if (trackerIndex < trackerSteps.lastIndex) {
                                    trackerIndex += 1
                                }
                            },
                            onBack = {
                                if (trackerIndex > 0) {
                                    trackerIndex -= 1
                                }
                            }
                        )

                        AppTab.HUB -> HubScreen(
                            userName = loggedInUser,
                            notificationsEnabled = notificationsEnabled,
                            lightMode = lightMode,
                            onToggleNotifications = {
                                notificationsEnabled = !notificationsEnabled
                            },
                            onToggleLightMode = {
                                lightMode = !lightMode
                            },
                            onOpenDocs = {
                                currentTab = AppTab.DOCS
                            }
                        )

                        AppTab.DOCS -> DocsScreen(
                            docs = docs,
                            onBack = { currentTab = AppTab.HUB },
                            onOpenDoc = {
                                selectedDoc = it
                                currentTab = AppTab.DOC_DETAIL
                            }
                        )

                        AppTab.DOC_DETAIL -> DocDetailScreen(
                            doc = selectedDoc,
                            onBack = { currentTab = AppTab.DOCS }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onContinue: (String) -> Unit) {
    var email by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(56.dp))

        Text(
            text = "Cast your vote today.\nHassle free.",
            fontSize = 24.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
            color = DarkText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(60.dp))

        TextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("Enter email", color = TextGray) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SurfaceGray,
                unfocusedContainerColor = SurfaceGray,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(22.dp))

        PrimaryButton("Continue with gmail") { onContinue(email) }

        Spacer(modifier = Modifier.height(14.dp))

        SecondaryButton("Login with Google") { onContinue(email) }

        Spacer(modifier = Modifier.height(12.dp))

        SecondaryButton("Login with Apple") { onContinue(email) }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DividerLine(Modifier.width(120.dp))
            Text(
                text = "or",
                modifier = Modifier.padding(horizontal = 10.dp),
                color = TextGray,
                fontSize = 14.sp
            )
            DividerLine(Modifier.width(120.dp))
        }

        Spacer(modifier = Modifier.height(26.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Already have an account? ",
                color = TextGray,
                fontSize = 14.sp
            )
            Text(
                text = "Log in",
                color = PrimaryBlue,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onContinue(email) }
            )
        }
    }
}

@Composable
fun HomeScreen(
    userName: String,
    items: List<ChecklistItem>,
    onToggleItem: (Int) -> Unit
) {
    val doneCount = items.count { it.done }
    val percent = if (items.isEmpty()) 0 else ((doneCount * 100f) / items.size).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        StatusClock()
        TopRightBadgeOnly()

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Hello, $userName.",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = DarkText
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "It looks like you’re $percent% ready to vote.",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = DarkText
        )

        Spacer(modifier = Modifier.height(26.dp))

        ReadinessRing(progress = percent / 100f, label = "$percent%")

        Spacer(modifier = Modifier.height(34.dp))

        items.forEachIndexed { index, item ->
            ChecklistRow(
                title = item.title,
                subtitle = item.subtitle,
                done = item.done,
                onClick = { onToggleItem(index) }
            )
        }
    }
}

@Composable
fun PlannerScreen(
    slots: List<String>,
    selectedIndex: Int,
    onSelectSlot: (Int) -> Unit
) {
    val crowdLabel = when (selectedIndex) {
        0 -> "Light"
        1 -> "Not as crowded"
        2 -> "Moderate"
        else -> "Busy"
    }

    val gaugeProgress = when (selectedIndex) {
        0 -> 0.18f
        1 -> 0.35f
        2 -> 0.58f
        else -> 0.82f
    }

    var address by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("Enter an address to find polling places.") }
    var electionName by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val pollingLocations = remember { mutableStateListOf<PollingLocation>() }
    val earlyVoteSites = remember { mutableStateListOf<PollingLocation>() }
    val dropOffLocations = remember { mutableStateListOf<PollingLocation>() }

    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    fun runLookup() {
        if (address.isBlank()) {
            statusText = "Please enter an address."
            return
        }

        if (CIVIC_API_KEY == "PASTE_YOUR_GOOGLE_CIVIC_API_KEY_HERE") {
            statusText = "Add your Google Civic API key in MainActivity.kt first."
            return
        }

        isLoading = true
        statusText = "Looking up polling places..."
        electionName = null
        pollingLocations.clear()
        earlyVoteSites.clear()
        dropOffLocations.clear()

        thread {
            val result = CivicApiHelper.lookupVoterInfo(address, CIVIC_API_KEY)

            mainHandler.post {
                isLoading = false
                electionName = result.electionName

                pollingLocations.addAll(result.pollingLocations)
                earlyVoteSites.addAll(result.earlyVoteSites)
                dropOffLocations.addAll(result.dropOffLocations)

                statusText = when {
                    result.errorMessage != null -> result.errorMessage
                    pollingLocations.isEmpty() &&
                            earlyVoteSites.isEmpty() &&
                            dropOffLocations.isEmpty() ->
                        "No polling locations found for this address."
                    else -> "Lookup complete."
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        item {
            StatusClock()
            PageHeader(title = "Plan the visit")

            Spacer(modifier = Modifier.height(18.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceGray),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Recommended timing",
                        fontSize = 12.sp,
                        color = TextGray
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = slots[selectedIndex],
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = crowdLabel,
                        fontSize = 13.sp,
                        color = TextGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            GaugeCard(progress = gaugeProgress)

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Find your polling place",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Address") },
                placeholder = { Text("123 Main St, City, State ZIP") },
                minLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { runLookup() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Find polling places")
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(statusText, color = TextGray)
                }
            } else {
                Text(
                    text = statusText,
                    color = if (statusText.contains("complete", true)) PrimaryBlue else TextGray
                )
            }

            if (!electionName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Election: $electionName",
                    fontWeight = FontWeight.SemiBold,
                    color = DarkText
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            slots.forEachIndexed { index, slot ->
                SelectableTimeRow(
                    text = slot,
                    selected = index == selectedIndex,
                    onClick = { onSelectSlot(index) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            PlannerActionRow("Polling place", "123 N. Street Atlanta, Georgia, 1234")
            PlannerActionRow("Get directions", "Light traffic, ETA 10 min.")
            PlannerActionRow("Accessibility", "Wheelchair supported")

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (pollingLocations.isNotEmpty()) {
            item {
                Text(
                    text = "Polling locations",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            items(pollingLocations) { location ->
                PollingLocationCard(location)
            }
        }

        if (earlyVoteSites.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Early voting sites",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            items(earlyVoteSites) { location ->
                PollingLocationCard(location)
            }
        }

        if (dropOffLocations.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ballot drop-off locations",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            items(dropOffLocations) { location ->
                PollingLocationCard(location)
            }
        }
    }
}

@Composable
fun TrackerScreen(
    trackerSteps: List<String>,
    currentStep: Int,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        StatusClock()
        PageHeader(title = "Track your ballot")

        Spacer(modifier = Modifier.height(28.dp))

        VerticalTimeline(
            items = trackerSteps,
            currentStep = currentStep
        )

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SecondarySmallButton("Back", onBack)
            PrimarySmallButton("Next", onNext)
        }
    }
}

@Composable
fun HubScreen(
    userName: String,
    notificationsEnabled: Boolean,
    lightMode: Boolean,
    onToggleNotifications: () -> Unit,
    onToggleLightMode: () -> Unit,
    onOpenDocs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        StatusClock()
        PageHeader(title = "MyHub", showBack = false)

        Spacer(modifier = Modifier.height(16.dp))

        ProfileCard(userName = userName)

        Spacer(modifier = Modifier.height(18.dp))

        SectionHeader("Preferences")
        ToggleRow("Notifications", notificationsEnabled, onToggleNotifications)
        ToggleRow("Light mode", lightMode, onToggleLightMode)
        StaticRow("Language", "English")

        Spacer(modifier = Modifier.height(18.dp))

        SectionHeader("Support")
        StaticRow("Locate help", null)
        StaticRow("Message us", null)
        StaticRow("About", null)
        StaticRow("Change password", null)

        Spacer(modifier = Modifier.height(18.dp))

        SectionHeader("Documentation")
        StaticRow("Open documentation", null, onClick = onOpenDocs)
    }
}

@Composable
fun DocsScreen(
    docs: List<DocItem>,
    onBack: () -> Unit,
    onOpenDoc: (DocItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        StatusClock()
        PageHeader(title = "MyHub", onBack = onBack)

        Spacer(modifier = Modifier.height(18.dp))

        SectionHeader("Documentation")

        docs.take(8).forEach { doc ->
            StaticRow(doc.title, null, onClick = { onOpenDoc(doc) })
        }
    }
}

@Composable
fun DocDetailScreen(
    doc: DocItem?,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        StatusClock()
        PageHeader(title = "Documentation", onBack = onBack)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = doc?.title ?: "No document selected",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = DarkText
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = doc?.body ?: "No content available.",
            fontSize = 15.sp,
            lineHeight = 23.sp,
            color = DarkText
        )
    }
}

@Composable
fun StatusClock() {
    Text(
        text = "8:27",
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = DarkText
    )
}

@Composable
fun TopRightBadgeOnly() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        MiniBadge()
    }
}

@Composable
fun MiniBadge() {
    Box(
        modifier = Modifier
            .size(22.dp)
            .border(1.dp, DarkText, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(DarkText, CircleShape)
        )
    }
}

@Composable
fun PageHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    showBack: Boolean = true
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(28.dp)
                .clip(CircleShape)
                .clickable(enabled = showBack && onBack != null) { onBack?.invoke() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (showBack) "←" else "",
                color = DarkText,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = DarkText
        )

        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            MiniBadge()
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(PrimaryBlue)
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFEDEDED))
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = DarkText, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PrimarySmallButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(PrimaryBlue)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondarySmallButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceGray)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = DarkText, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DividerLine(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(BorderGray)
    )
}

@Composable
fun ReadinessRing(
    progress: Float,
    label: String
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(118.dp)) {
                val stroke = 8.dp.toPx()

                drawArc(
                    color = BorderGray,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )

                drawArc(
                    color = PrimaryBlue,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }

            Text(
                text = label,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )
        }
    }
}

@Composable
fun ChecklistRow(
    title: String,
    subtitle: String,
    done: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(260.dp)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkText
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = TextGray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            CheckBadge(done)
        }

        DividerLine()
    }
}

@Composable
fun CheckBadge(done: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .border(1.5.dp, if (done) PrimaryBlue else BorderGray, CircleShape)
            .background(
                if (done) PrimaryBlue.copy(alpha = 0.08f) else Color.Transparent,
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (done) "✓" else "",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryBlue
        )
    }
}

@Composable
fun GaugeCard(progress: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TrafficGauge(progress)
            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                GaugeLegendDot(BusyRed)
                Text(text = "Busy", fontSize = 12.sp, color = DarkText)
                Spacer(modifier = Modifier.width(18.dp))
                GaugeLegendDot(SuccessGreen)
                Text(text = "Light", fontSize = 12.sp, color = DarkText)
            }
        }
    }
}

@Composable
fun TrafficGauge(progress: Float) {
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(width = 140.dp, height = 82.dp)) {
            val stroke = 22.dp.toPx()
            val diameter = size.minDimension * 1.55f
            val topLeft = Offset((size.width - diameter) / 2f, size.height - diameter)
            val arcSize = Size(diameter, diameter)

            val colors = listOf(
                BusyRed,
                WarningOrange,
                WarningYellow,
                LightGreen,
                SuccessGreen
            )

            var startAngle = 180f
            val sweepAngle = 36f

            colors.forEach { color ->
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle - 2f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                )
                startAngle += sweepAngle
            }

            val center = Offset(size.width / 2f, size.height - 6.dp.toPx())
            val radius = diameter / 2f - stroke / 2f
            val angle = Math.toRadians((180 + (180 * progress)).toDouble())

            val needleEnd = Offset(
                x = center.x + (cos(angle) * radius * 0.78f).toFloat(),
                y = center.y + (sin(angle) * radius * 0.78f).toFloat()
            )

            drawLine(
                color = DarkText,
                start = center,
                end = needleEnd,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )

            drawCircle(
                color = Color(0xFFE2B93B),
                radius = 8.dp.toPx(),
                center = center
            )

            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = center
            )
        }
    }
}

@Composable
fun GaugeLegendDot(color: Color) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .background(color, RoundedCornerShape(2.dp))
    )
    Spacer(modifier = Modifier.width(6.dp))
}

@Composable
fun SelectableTimeRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) PrimaryBlue.copy(alpha = 0.08f) else SurfaceGray)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) PrimaryBlue else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Text(
            text = text,
            color = if (selected) PrimaryBlue else DarkText,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun PlannerActionRow(
    title: String,
    subtitle: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .border(1.dp, DarkText, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title.first().toString(),
                    color = DarkText,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.width(250.dp)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = TextGray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "›",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkText
            )
        }

        DividerLine()
    }
}

@Composable
fun VerticalTimeline(
    items: List<String>,
    currentStep: Int
) {
    Column {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimelineNode(active = index <= currentStep)

                    if (index != items.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(62.dp)
                                .background(if (index < currentStep) PrimaryBlue else BorderGray)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = item,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (index <= currentStep) "Completed" else "Pending",
                        fontSize = 14.sp,
                        color = if (index <= currentStep) PrimaryBlue else TextGray
                    )

                    if (index != items.lastIndex) {
                        Spacer(modifier = Modifier.height(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineNode(active: Boolean) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .border(3.dp, if (active) PrimaryBlue else BorderGray, CircleShape)
            .background(
                if (active) PrimaryBlue.copy(alpha = 0.08f) else Color.Transparent,
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(if (active) PrimaryBlue else Color.White, CircleShape)
        )
    }
}

@Composable
fun ProfileCard(userName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, DarkText, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.firstOrNull()?.uppercase() ?: "U",
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.width(210.dp)) {
                Text(
                    text = userName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${userName.lowercase()}@gmail.com",
                    fontSize = 13.sp,
                    color = TextGray
                )
            }

            Text(
                text = "Edit",
                fontSize = 13.sp,
                color = TextGray
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = DarkText
    )
}

@Composable
fun ToggleRow(
    title: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = DarkText
            )

            Switch(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PrimaryBlue
                )
            )
        }

        DividerLine()
    }
}

@Composable
fun StaticRow(
    title: String,
    trailing: String?,
    onClick: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = onClick != null) { onClick?.invoke() }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.width(220.dp),
                fontSize = 16.sp,
                color = DarkText
            )

            if (trailing != null) {
                Text(
                    text = trailing,
                    fontSize = 14.sp,
                    color = TextGray
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Spacer(modifier = Modifier.width(50.dp))
            }

            Text(
                text = "›",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = DarkText
            )
        }

        DividerLine()
    }
}

@Composable
fun PollingLocationCard(location: PollingLocation) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderGray, RoundedCornerShape(10.dp))
                .padding(14.dp)
        ) {
            Text(
                text = if (location.name.isBlank()) "Location" else location.name,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = location.address,
                color = DarkText
            )

            if (location.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = location.notes,
                    color = TextGray,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun BottomBar(
    currentTab: AppTab,
    onTabChange: (AppTab) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
    ) {
        DividerLine()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BottomBarItem("Home", currentTab == AppTab.HOME) { onTabChange(AppTab.HOME) }
            BottomBarItem("Planner", currentTab == AppTab.PLANNER) { onTabChange(AppTab.PLANNER) }
            BottomBarItem("Tracker", currentTab == AppTab.TRACKER) { onTabChange(AppTab.TRACKER) }
            BottomBarItem("MyHub", currentTab == AppTab.HUB || currentTab == AppTab.DOCS) {
                onTabChange(AppTab.HUB)
            }
        }
    }
}

@Composable
fun BottomBarItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(if (selected) 10.dp else 8.dp)
                .background(
                    color = if (selected) DarkText else BorderGray,
                    shape = CircleShape
                )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            color = if (selected) DarkText else TextGray,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

object CivicApiHelper {

    fun lookupVoterInfo(address: String, apiKey: String): LookupResult {
        return try {
            val encodedAddress = URLEncoder.encode(address, "UTF-8")
            val urlString =
                "https://www.googleapis.com/civicinfo/v2/voterinfo?address=$encodedAddress&officialOnly=true&key=$apiKey"

            val response = httpGet(urlString)
            val json = JSONObject(response)

            val electionName = json.optJSONObject("election")?.optString("name")

            val polling = parseLocations(json.optJSONArray("pollingLocations"))
            val early = parseLocations(json.optJSONArray("earlyVoteSites"))
            val dropOff = parseLocations(json.optJSONArray("dropOffLocations"))

            LookupResult(
                pollingLocations = polling,
                earlyVoteSites = early,
                dropOffLocations = dropOff,
                electionName = electionName,
                errorMessage = null
            )
        } catch (e: Exception) {
            LookupResult(
                pollingLocations = emptyList(),
                earlyVoteSites = emptyList(),
                dropOffLocations = emptyList(),
                electionName = null,
                errorMessage = "Lookup failed: ${e.message ?: "Unknown error"}"
            )
        }
    }

    private fun parseLocations(array: JSONArray?): List<PollingLocation> {
        if (array == null) return emptyList()

        val results = mutableListOf<PollingLocation>()

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val addressObj = item.optJSONObject("address")

            val line1 = addressObj?.optString("line1").orEmpty()
            val line2 = addressObj?.optString("line2").orEmpty()
            val city = addressObj?.optString("city").orEmpty()
            val state = addressObj?.optString("state").orEmpty()
            val zip = addressObj?.optString("zip").orEmpty()

            val fullAddress = listOf(line1, line2)
                .filter { it.isNotBlank() }
                .joinToString(", ")
                .let { first ->
                    listOf(
                        first,
                        listOf(city, state, zip).filter { it.isNotBlank() }.joinToString(" ")
                    ).filter { it.isNotBlank() }.joinToString(", ")
                }

            val notes = buildString {
                val hours = item.optString("pollingHours")
                val start = item.optString("startDate")
                val end = item.optString("endDate")

                if (hours.isNotBlank()) append("Hours: $hours")
                if (start.isNotBlank() || end.isNotBlank()) {
                    if (isNotBlank()) append("  ")
                    append("Dates: $start - $end")
                }
            }

            results.add(
                PollingLocation(
                    name = item.optString("name"),
                    address = fullAddress,
                    notes = notes
                )
            )
        }

        return results
    }

    private fun httpGet(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        return try {
            val stream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val body = stream.bufferedReader().use { it.readText() }

            if (connection.responseCode !in 200..299) {
                throw IllegalStateException(body)
            }

            body
        } finally {
            connection.disconnect()
        }
    }
}
