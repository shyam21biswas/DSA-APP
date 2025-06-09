package com.example.dsaadmin

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.dsaadmin.UserPreferences.saveQuestionsStatus
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import androidx.compose.foundation.layout.FlowRow

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import androidx.compose.material3.Button


data class Company(
    val id: String,
    val name: String,
    val logoUrl: String,
    val solved: Int = 0,
    val total: Int = 0
)

data class Question(
    val id: String,
    val title: String,
    val link: String? = "",
    val leetnumber: String? = "Ve",
    val tags: List<String> = emptyList(),
    val difficulty: String
)



@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(navController: NavController, user: FirebaseUser?) {
    val firestore = remember { FirebaseFirestore.getInstance() }

    var companies by remember { mutableStateOf<List<Company>>(emptyList()) }
    var selectedCompanyId by remember { mutableStateOf<String?>(null) }
    var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var questionStatusMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    var totalSolved by remember { mutableStateOf(0) }
    var totalQuestions by remember { mutableStateOf(0) }
    var selectedQuestion by remember { mutableStateOf<Question?>(null) }

    //the is extra one
    var hasShownDialog by remember { mutableStateOf(false) }
    val completedCompanyIds = remember { mutableStateMapOf<String, Boolean>() }
    val completedCompanies = remember { mutableStateListOf<String>() }

    //for a particular question click
    var confirmCompletionQuestion by remember { mutableStateOf<Question?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var showStatsDialog by remember { mutableStateOf(false) }
    //both are used to take note
    var selectedQuestionNote by remember { mutableStateOf<Question?>(null) }
    var takenote by remember { mutableStateOf(false) }

    //for open congo...
    var lastquestionsolved by remember { mutableStateOf<Boolean>(false) }

    //curent date time
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    val currentTimestamp = sdf.format(Date())

    //view model for recent question solved
    val viewModel: RecentSolvedViewModel = viewModel()


    //check this....

    val context = LocalContext.current

    // Load user's solved questions on login
    LaunchedEffect(user?.uid) {
        user?.let {
            // First, try loading from DataStore
            val cachedStatus = UserPreferences.loadQuestionsStatus(context)
            questionStatusMap = cachedStatus

            // Then try syncing with Firestore
            try {
                val userDoc = firestore.collection("users").document(user.uid).get().await()
                val remoteStatus = userDoc.get("questionsStatus") as? Map<String, Boolean>
                if (remoteStatus != null) {
                    questionStatusMap = remoteStatus
                    saveQuestionsStatus(context, remoteStatus) // Cache it locally
                }
            } catch (e: Exception) {
                Log.e("Firestore", "Failed to fetch from Firestore", e)
            }
        }
    }
    // Load companies and their question counts from Firestore
    LaunchedEffect(questionStatusMap) {
        try {
            val companySnapshot = firestore.collection("companies").get().await()

            if (companySnapshot.isEmpty) {
                companies = emptyList()
                return@LaunchedEffect
            }


            val companyList = mutableListOf<Company>()
            var solvedSum = 0
            var totalSum = 0

            for (companyDoc in companySnapshot.documents) {
                val questionsSnapshot = firestore.collection("companies")
                    .document(companyDoc.id)
                    .collection("questions")
                    //.orderBy("uploadedAt", Query.Direction.DESCENDING) // ✅ fetch oldest first
                    .get()
                    .await()

                val total = questionsSnapshot.size()
                val solved = questionsSnapshot.count { questionStatusMap[it.id] == true }

                val name = companyDoc.getString("name") ?: "Unknown"
                val logoUrl = companyDoc.getString("logoUrl") ?: ""

                val company = Company(
                    id = companyDoc.id,
                    name = name,
                    logoUrl = logoUrl,
                    solved = solved,
                    total = total
                )

                companyList.add(company)
                solvedSum += solved
                totalSum += total
            }

            companies = companyList
            totalSolved = solvedSum
            totalQuestions = totalSum
        } catch (e: Exception) {
            Log.e("Firestore", "Error loading companies", e)
        }
    }

    // Fetch questions for selected company
    LaunchedEffect(selectedCompanyId, questionStatusMap) {
        if (selectedCompanyId == null) {
            questions = emptyList()
            return@LaunchedEffect
        }

        val snapshot = firestore.collection("companies")
            .document(selectedCompanyId!!)
            .collection("questions")
            //.orderBy("leetnumber",Query.Direction.ASCENDING)
            .get()
            .await()

        questions = snapshot.map { doc ->
            Question(
                id = doc.id,
                title = doc.getString("title") ?: "Untitled",
                link = doc.getString("link"),
                leetnumber = doc.getString("leetnumber"),
                //status = if (questionStatusMap[doc.id] == true) "Done" else "To Do",

                tags = doc.get("tags") as? List<String> ?: emptyList(),

                difficulty = doc.getString("difficulty") ?: "Unknown"
            )
        }
    }

    val overallProgress = if (totalQuestions > 0) totalSolved.toFloat() / totalQuestions else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .padding(16.dp)
    ) {
        // Overall progress card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f).clickable { showStatsDialog = true },
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

//
                Text(
                    text = "Overall Progress",
                    style = MaterialTheme.typography.body1
                )


                ArchProgressBarWithInfo(
                    progress = overallProgress,
                    totalQuestions = totalQuestions,
                    totalSolved = totalSolved
                )


            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(companies) { company ->
                val percentage =
                    if (company.total > 0) company.solved.toFloat() / company.total else 0f
                val progressColor = when {
                    percentage <= 0.33f -> Color.Red
                    percentage <= 0.70f -> Color.Yellow
                    else -> Color(0xFF4CAF50)
                }
                Card(
                    modifier = Modifier
                        .width(150.dp)
                        .clickable { selectedCompanyId = company.id },
                    elevation = 4.dp,
                    backgroundColor = if (selectedCompanyId == company.id) Color(0xFFE0F7FA) else Color.White
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = company.logoUrl,
                            contentDescription = "${company.name} Logo",
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = company.name, style = MaterialTheme.typography.subtitle1)
                        Text(
                            text = "${company.solved} / ${company.total}",
                            style = MaterialTheme.typography.body2
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (percentage < 1f) {
                            Canvas(modifier = Modifier.size(40.dp)) {
                                drawArc(
                                    color = Color.Gray,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 4f),
                                    size = Size(size.width, size.height)
                                )
                                drawArc(
                                    color = progressColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f * percentage,
                                    useCenter = false,
                                    style = Stroke(width = 4f),
                                    size = Size(size.width, size.height)
                                )
                            }
                        } else {


                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.check),
                                contentDescription = "Completed",
                                modifier = Modifier.size(40.dp),
                                tint = Color.Black
                            )

                            // Trigger dialog only once when completion is detected

                            // some how when the app start it wont get any company id "!completedCompanies.contains(company.id)"
                            //got false .... handle the extra store things of complte company data.  in future they may be bug
                            LaunchedEffect(company.id, company.solved, company.total) {
                                if (company.solved == company.total &&
                                    company.id == selectedCompanyId &&
                                    //!completedCompanies.contains(company.id)
                                    lastquestionsolved
                                ) {
                                    hasShownDialog = true
                                    lastquestionsolved = false
                                    //completedCompanies.add(company.id)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        //val coroutineScope = rememberCoroutineScope()


        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f)
        ) {
            if (selectedCompanyId == null) {
                item {
                    Text(
                        text = "Select a company to view questions",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(questions) { question ->
                    val isDone = questionStatusMap[question.id] == true


                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 8.dp)
                            .clickable {
                                Toast.makeText(context, "Question clicked", Toast.LENGTH_SHORT).show()
                                selectedQuestion = question
                            },
                        elevation = 4.dp,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.Black),

                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                // Question Title
                                Text(
                                    text = question.title,
                                    style = MaterialTheme.typography.h6,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Tags and Difficulty
                               /* Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                        // Tag Chips
                                        question.tags.take(2).forEach { tag ->
                                            val chipColor = Color(0xFF020202)



                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        chipColor,
                                                        shape = RoundedCornerShape(16.dp)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = tag,
                                                    style = MaterialTheme.typography.caption,
                                                    color = Color.White,

                                                    )
                                            }
                                        }



//                                        // Difficulty Label
//                                        val (diffColor, diffText) = when (question.difficulty) {
//                                            "Easy" -> Color(0xFF4CAF50) to "EASY"
//                                            "Medium" -> Color(0xFFFFA000) to "MEDIUM"
//                                            else -> Color(0xFFD32F2F) to "HARD"
//                                        }
//
//                                        Text(
//                                            text = diffText,
//                                            color = diffColor,
//                                            fontWeight = FontWeight.Bold,
//                                            style = MaterialTheme.typography.subtitle2,
//                                            modifier = Modifier.padding(start = 8.dp)
//                                        )
                                    }*/
                                }
                            // Difficulty Label

                                val (diffColor, diffText) = when (question.difficulty) {
                                    "Easy" -> Color(0xFF4CAF50) to "EASY"
                                    "Medium" -> Color(0xFFFFA000) to "MEDIUM"
                                    else -> Color(0xFFD32F2F) to "HARD"
                                }


                                Text(
                                    text = diffText,
                                    color = diffColor,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.subtitle2,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            Spacer(modifier = Modifier.width(8.dp))

//
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(
                                    id = if (isDone) R.drawable.done else R.drawable.not__done
                                ),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable {
                                        if (!isDone) {
                                            // Ask confirmation before marking as done

                                            confirmCompletionQuestion = question

                                        } else {
                                            // Directly unmark without confirmation
                                            questionStatusMap =
                                                questionStatusMap.toMutableMap().apply {
                                                    put(question.id, false)
                                                }
                                            user?.let {
                                                firestore.collection("users")
                                                    .document(user.uid)
                                                    .update("questionsStatus.${question.id}", false)
                                            }
                                            coroutineScope.launch {
                                                saveQuestionsStatus(context, questionStatusMap)
                                            }
                                        }
                                    }
                            )

                        }
                    }
                }
            }
        }
    }


    //main decription diaglog..............................................
    val colorList = listOf(
        Color(0xFF4CAF50),
        Color(0xFFE91E63),
        Color(0xFFE0A952)
    )

    if (selectedQuestion != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x66000000)) // semi-transparent dark backdrop
                .clickable(enabled = false) {} // prevent clicks behind dialog
        ) {
             //Glow Effect Box
//            Box(
//                modifier = Modifier
//                    .align(Alignment.Center)
//                    .size(300.dp)
//                    .graphicsLayer {
//                        shadowElevation = 50f
//                        shape = RoundedCornerShape(20.dp)
//                        clip = false
//                    }
//                    .background(
//                        Color(0xFF4CAF50).copy(alpha = 0.7f),
//                        shape = RoundedCornerShape(20.dp)
//                    )
//            )
            AlertDialog(
                shape = RoundedCornerShape(16.dp),

                onDismissRequest = { selectedQuestion = null },
                title = {
                    Column(modifier = Modifier.fillMaxWidth()) {


                        Text(
                            text = "LeetCode #${selectedQuestion!!.leetnumber ?: "Not Available"}",
                            style = MaterialTheme.typography.body1.copy(color = Color.Gray),
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(selectedQuestion!!.link))
                                context.startActivity(intent)
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedQuestion!!.title,
                            style = MaterialTheme.typography.h6 .copy(fontWeight = FontWeight.Bold)
                            ,modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(selectedQuestion!!.link))
                                context.startActivity(intent)
                            }
                        )

                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Tags:",
                            style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Medium)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            maxItemsInEachRow = 3,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)


                        ) {
                            selectedQuestion!!.tags.forEachIndexed { index, tag ->
                                val color = colorList[index % colorList.size]
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = 4.dp,
                                    color = color
                                ) {
                                    Text(
                                        text = tag,
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.body2.copy(color = Color.White)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, end = 8.dp)
                    ) {
                        TextButton(onClick = {

                            selectedQuestionNote = selectedQuestion
                            takenote = true
                        }) {
                            Text("Take/See Notes")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { selectedQuestion = null }) {
                            Text("Close")
                        }
                    }
                }
            )
        }
    }

    //when all question are solved ......
//    if (hasShownDialog) {
//        AlertDialog(
//            shape = RoundedCornerShape(16.dp),
//            onDismissRequest = { hasShownDialog = false },
//            title = {
//                Text("🎉 Congratulations!", fontWeight = FontWeight.Bold)
//            },
//            text = {
//                Text("You’ve completed all questions from the Company Tag!\nTime to dominate those interviews. 💼🔥")
//            },
//            confirmButton = {
//                TextButton(onClick = { hasShownDialog = false }) {
//                    Text("Awesome! 🚀")
//                }
//            }
//        )
//    }

    if (hasShownDialog) {
        AlertDialog(
            onDismissRequest = { hasShownDialog = false },
            confirmButton = {
                TextButton(onClick = { hasShownDialog = false }) {
                    Text("Awesome! 🚀" , color = Color.Black ,)
                }
            },
            //backgroundColor = Color(0xFFFEAE3F), // Match with image background 0xFFFFB74D
            backgroundColor = Color(0xFFABC9AA), // Match with image background 0xFFFFB74D


            title = {
                // Top Image Box
                Box(
                    //modifier = Modifier.fillMaxWidth()
                    //.height(200.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.man4),
                        contentDescription = "Milestone",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.height(150.dp).width(350.dp)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Image Bo


                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "🎉 Congratulations!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You’ve completed all questions from the Company Tag!\nTime to dominate those interviews. 💼🔥",
                        textAlign = TextAlign.Center,
                        color = Color.Black
                    )
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }



    //question click
    //2. add a function that will show you the  last 5 question solved............
    if (confirmCompletionQuestion != null) {

        AlertDialog(
            shape = RoundedCornerShape(16.dp),
            onDismissRequest = { confirmCompletionQuestion = null },
            title = { Text("Mark as Completed?") },
            text = { Text("Are you sure you’ve completed this question?") },
            confirmButton = {
                TextButton(onClick = {

                    confirmCompletionQuestion?.let { question ->
                        questionStatusMap = questionStatusMap.toMutableMap().apply {
                            put(question.id, true)
                        }
                        //RecentSolvedManager.addSolvedQuestion(question.title)
                        viewModel.addSolvedQuestion(user!!.uid, question.title)

                        user?.let {
                            firestore.collection("users")
                                .document(user.uid)
                                .update("questionsStatus.${question.id}", true)
                        }
                        coroutineScope.launch {
                            saveQuestionsStatus(context, questionStatusMap)
                        }
                    }
                    incrementTodaySolved(user!!.uid)



                    confirmCompletionQuestion = null

                    lastquestionsolved = true


                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmCompletionQuestion = null }) {
                    Text("Cancel")
                }
            }
        )
    }

//    if (lastquestionsolved) {
//        LaunchedEffect(Unit) {
//            delay(4000) // 2 seconds delay
//            lastquestionsolved = false
//        }}


    if (showStatsDialog) {
        StatsDialog(userId = user!!.uid) {
            showStatsDialog = false
        }
    }




    if (takenote && selectedQuestionNote != null) {
        NoteDialog(
            userId = user?.uid.orEmpty(),
            questionId = selectedQuestionNote!!.id,
            onDismiss = {
                takenote = false
                selectedQuestionNote = null
            }
        )
    }


}



@Composable
fun SignInScreenf(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val oneTapClient = Identity.getSignInClient(context)
    var userName by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var navigateToHome by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        try {
            val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
            val idToken = credential.googleIdToken

            if (idToken != null) {
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            userName = user?.displayName

                            val firestore = FirebaseFirestore.getInstance()
                            val userRef = firestore.collection("users").document(user!!.uid)

                            userRef.get().addOnSuccessListener { document ->
                                if (!document.exists()) {
                                    val userData = mapOf("questionsStatus" to mapOf<String, Boolean>())
                                    userRef.set(userData)
                                        .addOnSuccessListener {
                                            Log.d("Firestore", "User document created")
                                        }
                                        .addOnFailureListener {
                                            Log.e("Firestore", "Failed to create user document", it)
                                        }
                                }
                            }

                            Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()



                        } else {
                            Toast.makeText(context, "Sign-in failed", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (userName == null) {
                Text(
                    text = "Welcome!",
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4F76BB)
                )

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = {
                        val signInRequest = BeginSignInRequest.builder()
                            .setGoogleIdTokenRequestOptions(
                                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                                    .setSupported(true)
                                    .setServerClientId("958727059701-ccqo9qce5aro0tc9hllhci7h7cgq8tfo.apps.googleusercontent.com")
                                    .setFilterByAuthorizedAccounts(false)
                                    .build()
                            )
                            .build()

                        oneTapClient.beginSignIn(signInRequest)
                            .addOnSuccessListener { result ->
                                val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent).build()
                                launcher.launch(intentSenderRequest)
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
                            }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4285F4),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(text = "Sign in with Google", fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Sign in with email",
                    modifier = Modifier.clickable { navController.navigate("manualEmail") },
                    color = Color(0xFF4F76BB)
                )
            } else {
                Text(
                    text = "Welcome, $userName!!",
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF145ACB)
                )
                navigateToHome = true

            }
        }
    }

    if (navigateToHome) {
        LaunchedEffect(Unit) {
            delay(2000)
            navController.navigate("home")
            {popUpTo("signin") { inclusive = true }}

            navigateToHome = false
        }
    }

}




@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ArchProgressBarWithInfo(
    progress: Float,
    totalQuestions: Int,
    totalSolved: Int,
    modifier: Modifier = Modifier
    ) {
        var currvalur  by remember { mutableStateOf(false) }
        val color1 = if (progress <= 0.33f) Color(0xFFD72525) else if (progress <= 0.66f) Color(
            0xFFFFC107
        ) else Color(0xFF4CAF50)
        val color2 = if (progress <= 0.33f) Color(0xFFFFFFFF)
        else if (progress <= 0.66f) Color(
            0xFFFFFFFF
        ) else if (progress == 1f) Color(0xFF4CAF50) else Color(0xFFFFFFFF)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .width(200.dp)
                        .height(100.dp)
                        .padding(8.dp)
                ) {
                    val strokeWidth = 12.dp.toPx()
                    val diameter = size.width
                    val topLeft = Offset(0f, 0f)
                    val sweepAngle = 180f * progress.coerceIn(0f, 1f)

                    // Background Arch
                    drawArc(
                        color = Color.LightGray.copy(alpha = 0.2f),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )

                    // Foreground Arch with gradient
                    drawArc(
                        brush = Brush.linearGradient(
                            colors = listOf(color1, color2)
                        ),
                        startAngle = 180f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                }
                // Center text
                Column(verticalArrangement = Arrangement.Bottom) {
                    Text(
                        text = "",
                        style = MaterialTheme.typography.h6.copy(color = Color(0xFF000000))
                    )
                    AnimatedContent(
                        targetState = currvalur,
                        transitionSpec = {
                            (fadeIn(tween(300)) + scaleIn(initialScale = 0.8f) with
                                    fadeOut(tween(300)) + scaleOut(targetScale = 0.8f)).using(
                                SizeTransform(clip = false)
                            )
                        },
                        contentAlignment = Alignment.Center
                    ) { state ->
                        if (state)
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { currvalur = false }
                            )
                        else
                            Text(
                                text = "$totalSolved / $totalQuestions",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { currvalur = true }
                            )
                    }
//                    if(currvalur)
//                    Text(
//                        text = "${(progress * 100).toInt()}%",
//                        fontSize = 24.sp,
//                        fontWeight = FontWeight.Bold, modifier = Modifier.clickable { currvalur = false }
//                    )
//                    else
//                    Text(
//                        text = "$totalSolved / $totalQuestions",
//                        fontSize = 24.sp,
//                        fontWeight = FontWeight.Bold, modifier = Modifier.clickable { currvalur = true }
//                    )
                }
            }



            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "$totalSolved / $totalQuestions",
                    style = MaterialTheme.typography.h6.copy(color = Color(0xFF000000))
                )
                Text(
                    text = "Champ keep it up 👋",
                    style = MaterialTheme.typography.body1
                )
            }
        }
    }



//stat of progress...................
fun fetchLast7DaysStats(
    userId: String,
    onResult: (Map<String, Int>) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Properly collect last 7 dates
    val last7Dates = (0..6).map {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -it) }
    }.map {
        dateFormat.format(it.time)
    }.reversed()

    db.collection("users").document(userId)
        .collection("dailyStats")
        .get()
        .addOnSuccessListener { snapshot ->
            val allData = snapshot.documents.mapNotNull {
                val date = it.id
                val count = it.getLong("questionsSolved")?.toInt()
                if (count != null && date in last7Dates) date to count else null
            }.toMap()

            val result = last7Dates.associateWith { allData[it] ?: 0 }
            onResult(result)
        }
        .addOnFailureListener {
            onResult(emptyMap())
        }
}


//not in used.......................
@Composable
fun BarChart(stats: Map<String, Int>, modifier: Modifier = Modifier) {
        val maxVal = stats.values.maxOrNull()?.takeIf { it > 0 } ?: 1
        val barWidth = 24.dp
        val spacing = 16.dp
        val labelStyle = MaterialTheme.typography.caption

        Column(modifier = modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val barSpacing =
                        (canvasWidth - (barWidth.toPx() * stats.size)) / (stats.size + 1)

                    stats.entries.forEachIndexed { index, (date, value) ->
                        val left = barSpacing + index * (barWidth.toPx() + barSpacing)
                        val barHeight = (value / maxVal.toFloat()) * canvasHeight

                        drawRect(
                            color = Color(0xFF00C9FF),
                            topLeft = Offset(left, canvasHeight - barHeight),
                            size = Size(barWidth.toPx(), barHeight)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Date labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                stats.keys.forEach { date ->
                    Text(
                        text = date.takeLast(2), // just show day part (dd)
                        style = labelStyle,
                        modifier = Modifier.width(barWidth)
                    )
                }
            }
        }
    }


//store number of question solved
fun incrementTodaySolved(userId: String) {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val docRef = FirebaseFirestore.getInstance()
        .collection("users").document(userId)
        .collection("dailyStats").document(date)

    docRef.update("questionsSolved", FieldValue.increment(1))
        .addOnFailureListener {
            // If doc doesn’t exist, set to 1
            docRef.set(mapOf("questionsSolved" to 1))
        }
}


    

//not in used   but desgin isung scracht..............
@Composable
fun StatsScreen(userId: String, onDismiss: () -> Unit) {
    var stats by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    LaunchedEffect(Unit) {
        fetchLast7DaysStats(userId) {
            stats = it
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📊 Daily Solved Stats", style = MaterialTheme.typography.h6)
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(Modifier.height(16.dp))
        if (stats.isNotEmpty()) {
            BarChart(stats)
        } else {
            Text("No stats available yet.")
        }
    }
}

@Composable
fun StatsDialog(userId: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colors.background,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 700.dp) // Limit max height

                .padding(16.dp)

        ) {


                //StatsScreen(userId = userId, onDismiss = onDismiss)
                BarChartScreen(userId = userId, onDismiss = onDismiss)


        }
    }
}




@Composable
fun CongratulationsDialog(hasShownDialog: Boolean, onDismiss: () -> Unit) {
    if (hasShownDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Awesome! 🚀")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Replace with your image
                    Image(
                        painter = painterResource(id = R.drawable.winner2),
                        contentDescription = "Trophy",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 16.dp)
                    )
                    Text(
                        text = "🎉 Congratulations!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You’ve completed all questions from the Company Tag!\nTime to dominate those interviews. 💼🔥",
                        textAlign = TextAlign.Center
                    )
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun MilestoneDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {  },
            confirmButton = {
                TextButton(onClick = { }) {
                    Text("Awesome!🚀" , fontSize = 20.sp , color = Color.Black , fontWeight = FontWeight.Bold ,)
                }
            },
            backgroundColor = Color(0xFFABC9AA), // Match with image background 0xFFFFB74D

             title = {
                // Top Image Box
                Box(
                    //modifier = Modifier.fillMaxWidth()
                        //.height(200.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.man4),
                        contentDescription = "Milestone",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.height(125.dp).width(350.dp)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Image Bo


                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "🎉 Congratulations!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You’ve completed all questions from the Company Tag!\nTime to dominate those interviews. 💼🔥",
                        textAlign = TextAlign.Center,
                        color = Color.Black
                    )
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

