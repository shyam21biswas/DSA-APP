package com.example.dsaadmin

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape


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
    //val status: String,
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
    var wasPreviouslyComplete by remember { mutableStateOf(false) }

// Get questions from ViewModel
    //val questionss = viewModel.questions.collectAsState().value

// Filter company questions
    val companyQuestions = questions.filter { "company" in it.tags }

// Check if all company questions are completed now
   // val allCompanyCompleted = companyQuestions.isNotEmpty() && companyQuestions.all { it.isCompleted }


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
            .get()
            .await()

        questions = snapshot.map { doc ->
            Question(
                id = doc.id,
                title = doc.getString("title") ?: "Untitled",
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
                .weight(1f),
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Overall Progress",
                    style = MaterialTheme.typography.h5,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$totalSolved / $totalQuestions Solved",
                    style = MaterialTheme.typography.body1,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = overallProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = Color(0xFF4CAF50)
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
                val percentage = if (company.total > 0) company.solved.toFloat() / company.total else 0f
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
                            LaunchedEffect(Unit) {

                                    hasShownDialog = true

                            }
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.check),
                                contentDescription = "Completed",
                                modifier = Modifier.size(40.dp),
                                tint = Color.Black
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        val coroutineScope = rememberCoroutineScope()
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
                            .padding(vertical = 4.dp)
                            .clickable {

                                Toast.makeText(context, "Question clicked", Toast.LENGTH_SHORT).show()
                                selectedQuestion = question
                                // Toggle status and update Firestore
//                                val newStatus = !isDone
//                                questionStatusMap = questionStatusMap.toMutableMap().apply {
//                                    put(question.id, newStatus)
//                                }
//                                user?.let {
//                                    firestore.collection("users")
//                                        .document(user.uid)
//                                        .update("questionsStatus.${question.id}", newStatus)
//                                }
//                                coroutineScope.launch {
//                                    saveQuestionsStatus(context, questionStatusMap)
//                                }
                            },
                        elevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = question.title, style = MaterialTheme.typography.body1)
                                Text(
                                    text = "tags | ${question.tags.joinToString("  ").capitalize()} | Difficulty: ${question.difficulty}",
                                    style = MaterialTheme.typography.caption
                                )
                            }
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(
                                    id = if (isDone) R.drawable.done else R.drawable.not__done
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp).clickable {
                                    val newStatus = !isDone
                                    questionStatusMap = questionStatusMap.toMutableMap().apply {
                                        put(question.id, newStatus)
                                    }
                                    user?.let {
                                        firestore.collection("users")
                                            .document(user.uid)
                                            .update("questionsStatus.${question.id}", newStatus)
                                    }
                                    coroutineScope.launch {
                                        saveQuestionsStatus(context, questionStatusMap)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    val colorlist =  listOf(
        Color(0xFF4CAF50),
        Color(0xFFE91E63),
        Color(0xFFE0A952)
    )
    if (selectedQuestion != null) {
        AlertDialog(
            shape = RoundedCornerShape(16.dp),

            onDismissRequest = { selectedQuestion = null },
            title = {
                Text(text = selectedQuestion!!.title, fontWeight = FontWeight.Bold)
            },
            text = {
                FlowRow(

                    modifier = Modifier.fillMaxWidth().padding(2.dp)
                ) {
                    selectedQuestion!!.tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(horizontal = 8.dp),
                            elevation = 4.dp,
                            color = Color(colorlist.random().value)
                        ) {
                            Text(
                                text = tag,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.body2
                            )

                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedQuestion = null }) {
                    Text("Close")
                }
            }
        )
    }

    //when all question are solved ......
    if (hasShownDialog) {
        AlertDialog(
            onDismissRequest = { hasShownDialog = false },
            title = {
                Text("🎉 Congratulations!", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("You’ve completed all questions from the Company Tag!\nTime to dominate those interviews. 💼🔥")
            },
            confirmButton = {
                TextButton(onClick = { hasShownDialog = false }) {
                    Text("Awesome! 🚀")
                }
            }
        )
    }

}



@Composable
fun SignInScreen(navController: NavController) {
    val context = LocalContext.current
    var selectedQuestion by remember { mutableStateOf<Question?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener { authResult ->
                    if (authResult.isSuccessful) {
                        val user = FirebaseAuth.getInstance().currentUser
                        user?.let {
                            val firestore = FirebaseFirestore.getInstance()
                            val userRef = firestore.collection("users").document(user.uid)

                            userRef.get().addOnSuccessListener { document ->
                                if (!document.exists()) {
                                    // Create user document with default data
                                    val userData = mapOf("questionsStatus" to mapOf<String, Boolean>())
                                    userRef.set(userData)
                                        .addOnSuccessListener {
                                            Log.d("Firestore", "User document created")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("Firestore", "Failed to create user document", e)
                                        }
                                }
                            }
                        }
                        navController.navigate("home")
                    } else {
                        Toast.makeText(context, "Sign-in failed", Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("958727059701-ccqo9qce5aro0tc9hllhci7h7cgq8tfo.apps.googleusercontent.com") // Add this from google-services.json
            .requestEmail()
            .build()

        val client = GoogleSignIn.getClient(context, gso)
        val signInIntent = client.signInIntent
        launcher.launch(signInIntent)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}


