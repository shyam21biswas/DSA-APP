package com.example.dsaadmin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dsaadmin.ui.theme.DSAAdminTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DSAAdminTheme {
                //AdminHomeScreen()

                MyApp()
//                val navController = rememberNavController()
//                NavHost(navController = navController, startDestination = "ved") {
//                    composable("ved") { ved(navController) }
//                    composable("myapp") { MyApp() }
//                    composable("admin") { AdminHomeScreen() }
//
//                }
               // BarChartScreen()


            }
        }
    }
}


@Composable
fun MyApp() {
    val navController = rememberNavController()
    val user = FirebaseAuth.getInstance().currentUser
    FirebaseFirestore.getInstance().firestoreSettings = FirebaseFirestoreSettings.Builder()
        .setPersistenceEnabled(true)
        .build()



    NavHost(navController = navController, startDestination = if (user != null) "home" else "signin") {
        composable("signin") { SignInScreen(navController) }
        composable("home") { HomeScreen(navController, FirebaseAuth.getInstance().currentUser) }
    }
}

@Composable
fun ved (navController: NavHostController)
{
    //val navController = rememberNavController()

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally , verticalArrangement = Arrangement.Center)
    {

        Button(
                onClick = {
                    navController.navigate("myapp")
                    {popUpTo("ved") { inclusive = true }}

                },
            colors = ButtonDefaults.buttonColors( Color.Green),
            modifier = Modifier.padding(16.dp)
        ) {
            Text("DSA TRacker" , color = Color.White )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                navController.navigate("admin")
                {popUpTo("ved") { inclusive = true }}

            },
            colors = ButtonDefaults.buttonColors( Color.Green),
            modifier = Modifier.padding(16.dp)
        ) {
            Text("admin" , color = Color.White )
        }


    }

}
