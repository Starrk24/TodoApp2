package com.example.todoapp

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class TodoItem(
    val id: Int,
    val title: String,
    val description: String,
    val dueDate: String,
    val category: String,
    val isCompleted: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = getSharedPreferences("MojeZadaniaPrefs", Context.MODE_PRIVATE)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TodoApp(sharedPreferences)
                }
            }
        }
    }
}

@Composable
fun TodoApp(sharedPreferences: SharedPreferences) {
    // Inicjalizacja autoryzacji Firebase
    val auth = remember { FirebaseAuth.getInstance() }

    fun loadTasks(): List<TodoItem> {
        val savedList = mutableListOf<TodoItem>()
        val jsonString = sharedPreferences.getString("lista_zadan", "[]")
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                savedList.add(
                    TodoItem(
                        id = obj.optInt("id", 0),
                        title = obj.optString("title", ""),
                        description = obj.optString("description", ""),
                        dueDate = obj.optString("dueDate", ""),
                        category = obj.optString("category", "Inne"),
                        isCompleted = obj.optBoolean("isCompleted", false)
                    )
                )
            }
        } catch (e: Exception) { e.printStackTrace() }
        return savedList
    }

    fun saveTasks(tasks: List<TodoItem>) {
        val jsonArray = JSONArray()
        for (item in tasks) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("description", item.description)
            obj.put("dueDate", item.dueDate)
            obj.put("category", item.category)
            obj.put("isCompleted", item.isCompleted)
            jsonArray.put(obj)
        }
        sharedPreferences.edit().putString("lista_zadan", jsonArray.toString()).apply()
    }

    val todoList = remember { mutableStateListOf<TodoItem>().apply { addAll(loadTasks()) } }
    var nextId by rememberSaveable { mutableStateOf((todoList.maxOfOrNull { it.id } ?: -1) + 1) }

    val navController = rememberNavController()

    val onTaskChecked: (TodoItem, Boolean) -> Unit = { item, isChecked ->
        val index = todoList.indexOf(item)
        if (index != -1) {
            todoList[index] = item.copy(isCompleted = isChecked)
            saveTasks(todoList)
        }
    }

    val onTaskDeleted: (TodoItem) -> Unit = { item ->
        todoList.remove(item)
        saveTasks(todoList)
    }

    // --- NAWIGACJA ---
    NavHost(navController = navController, startDestination = "splash") {

        composable("splash") {
            SplashScreen(navController = navController, auth = auth)
        }

        // --- NOWE EKRANY LOGOWANIA I REJESTRACJI ---
        composable("login") {
            LoginScreen(navController = navController, auth = auth)
        }

        composable("register") {
            RegisterScreen(navController = navController, auth = auth)
        }

        composable("menu") {
            CircularMenuScreen(
                navController = navController,
                auth = auth,
                onCategorySelected = { category ->
                    navController.navigate("lista/$category")
                }
            )
        }

        composable("lista/{kategoria}") { backStackEntry ->
            val kategoria = backStackEntry.arguments?.getString("kategoria") ?: "Wszystkie"
            val filteredTasks = todoList.filter {
                !it.isCompleted && (kategoria == "Wszystkie" || it.category == kategoria)
            }
            val screenTitle = if (kategoria == "Wszystkie") "Wszystkie Zadania" else "Kategoria: $kategoria"

            TodoListScreen(
                title = screenTitle,
                todoList = filteredTasks,
                currentRoute = "lista",
                navController = navController,
                auth = auth,
                onNavigateToAdd = { navController.navigate("dodaj") },
                onNavigateToEdit = { taskId -> navController.navigate("edytuj/$taskId") },
                onCheckedChange = onTaskChecked,
                onDeleteClick = onTaskDeleted,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("ukonczone") {
            TodoListScreen(
                title = "Ukończone Zadania",
                todoList = todoList.filter { it.isCompleted },
                currentRoute = "ukonczone",
                navController = navController,
                auth = auth,
                onNavigateToAdd = { navController.navigate("dodaj") },
                onNavigateToEdit = { taskId -> navController.navigate("edytuj/$taskId") },
                onCheckedChange = onTaskChecked,
                onDeleteClick = onTaskDeleted,
                onBackClick = null
            )
        }

        composable("dodaj") {
            AddTaskScreen(
                itemToEdit = null,
                onSave = { title, desc, date, category ->
                    todoList.add(TodoItem(nextId++, title, desc, date, category))
                    saveTasks(todoList)
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() }
            )
        }

        composable("edytuj/{taskId}") { backStackEntry ->
            val taskIdString = backStackEntry.arguments?.getString("taskId")
            val taskId = taskIdString?.toIntOrNull()
            val itemToEdit = todoList.find { it.id == taskId }

            if (itemToEdit != null) {
                AddTaskScreen(
                    itemToEdit = itemToEdit,
                    onSave = { title, desc, date, category ->
                        val index = todoList.indexOf(itemToEdit)
                        if (index != -1) {
                            todoList[index] = itemToEdit.copy(title = title, description = desc, dueDate = date, category = category)
                            saveTasks(todoList)
                        }
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
        }
    }
}

// --- EKRAN POWITALNY (Z LOGIKĄ FIREBASE) ---
@Composable
fun SplashScreen(navController: NavController, auth: FirebaseAuth) {
    LaunchedEffect(key1 = true) {
        delay(2000)

        // Sprawdzamy, czy użytkownik jest już zalogowany
        val currentUser = auth.currentUser
        if (currentUser != null) {
            navController.navigate("menu") { popUpTo("splash") { inclusive = true } }
        } else {
            navController.navigate("login") { popUpTo("splash") { inclusive = true } }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Logo", modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Moja Lista Zadań", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

// --- NOWY EKRAN: LOGOWANIE ---
@Composable
fun LoginScreen(navController: NavController, auth: FirebaseAuth) {
    val context = LocalContext.current
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Logo", modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Witaj z powrotem!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Hasło") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    isLoading = true
                    // Firebase Logowanie
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Zalogowano pomyślnie!", Toast.LENGTH_SHORT).show()
                                navController.navigate("menu") { popUpTo("login") { inclusive = true } }
                            } else {
                                Toast.makeText(context, "Błąd: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(context, "Wypełnij wszystkie pola", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            else Text("Zaloguj się", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { navController.navigate("register") }) {
            Text("Nie masz konta? Zarejestruj się")
        }
    }
}

// --- NOWY EKRAN: REJESTRACJA ---
@Composable
fun RegisterScreen(navController: NavController, auth: FirebaseAuth) {
    val context = LocalContext.current
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Stwórz konto", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Hasło (min. 6 znaków)") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Powtórz hasło") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(context, "Wypełnij wszystkie pola", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (password != confirmPassword) {
                    Toast.makeText(context, "Hasła nie są identyczne!", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isLoading = true
                // Firebase Rejestracja
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Konto utworzone!", Toast.LENGTH_SHORT).show()
                            // Automatycznie przekieruj do aplikacji po rejestracji
                            navController.navigate("menu") {
                                popUpTo("register") { inclusive = true }
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            Toast.makeText(context, "Błąd: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            else Text("Zarejestruj się", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { navController.popBackStack() }) {
            Text("Wróć do logowania")
        }
    }
}

// --- FUNKCJA POMOCNICZA: WYLOGOWYWANIE ---
fun performLogout(navController: NavController, auth: FirebaseAuth) {
    auth.signOut()
    navController.navigate("login") {
        popUpTo(navController.graph.startDestinationId) { inclusive = true }
    }
}


// --- WSPÓLNY DOLNY PASEK NAWIGACJI ---
@Composable
fun TodoBottomBar(currentRoute: String, navController: NavController) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Start") },
            label = { Text("Menu") },
            selected = currentRoute == "menu",
            onClick = {
                if (currentRoute != "menu") {
                    navController.popBackStack(navController.graph.startDestinationId, inclusive = false)
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.List, contentDescription = "Zadania") },
            label = { Text("Zadania") },
            selected = currentRoute == "lista",
            onClick = {
                if (currentRoute != "lista") {
                    navController.navigate("lista/Wszystkie") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Ukończone") },
            label = { Text("Ukończone") },
            selected = currentRoute == "ukonczone",
            onClick = {
                if (currentRoute != "ukonczone") {
                    navController.navigate("ukonczone") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )
    }
}

// --- EKRAN GŁÓWNY: KOŁO WYBORU ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircularMenuScreen(
    navController: NavController,
    auth: FirebaseAuth,
    onCategorySelected: (String) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wybierz Kategorię") },
                actions = {
                    // Przycisk wylogowania w prawym górnym rogu
                    IconButton(onClick = { performLogout(navController, auth) }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Wyloguj")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        bottomBar = {
            TodoBottomBar(currentRoute = "menu", navController = navController)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(320.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { tapOffset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = tapOffset.x - center.x
                            val dy = tapOffset.y - center.y
                            val distance = sqrt(dx * dx + dy * dy)
                            val outerRadius = size.width / 2f
                            val innerRadius = outerRadius * 0.4f

                            if (distance <= innerRadius) {
                                onCategorySelected("Wszystkie")
                            } else if (distance <= outerRadius) {
                                var angle = atan2(dy.toDouble(), dx.toDouble()) * (180 / Math.PI)
                                if (angle < 0) angle += 360
                                val index = (angle / 90).toInt() % 4
                                val categories = listOf("Dom", "Praca", "Sport", "Hobby")
                                onCategorySelected(categories[index])
                            }
                        }
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerRadius = size.width / 2f
                val innerRadius = outerRadius * 0.4f

                val categories = listOf("Dom", "Praca", "Sport", "Hobby")
                val colors = listOf(Color(0xFFFFB74D), Color(0xFF64B5F6), Color(0xFF81C784), Color(0xFFF06292))

                for (i in 0 until 4) {
                    drawArc(
                        color = colors[i], startAngle = i * 90f, sweepAngle = 90f, useCenter = true,
                        size = Size(outerRadius * 2, outerRadius * 2), topLeft = Offset(center.x - outerRadius, center.y - outerRadius)
                    )

                    val angleRad = (i * 90f + 45f) * (Math.PI / 180f)
                    val textRadius = outerRadius * 0.7f
                    val textX = center.x + textRadius * cos(angleRad).toFloat()
                    val textY = center.y + textRadius * sin(angleRad).toFloat()

                    val textLayoutResult = textMeasurer.measure(text = categories[i], style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                    drawText(textLayoutResult = textLayoutResult, topLeft = Offset(textX - textLayoutResult.size.width / 2, textY - textLayoutResult.size.height / 2))
                }

                drawCircle(color = Color.White, radius = innerRadius, center = center)
                drawCircle(color = Color.Gray, radius = innerRadius, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))

                val centerTextLayoutResult = textMeasurer.measure(text = "Wszystkie", style = TextStyle(color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold))
                drawText(textLayoutResult = centerTextLayoutResult, topLeft = Offset(center.x - centerTextLayoutResult.size.width / 2, center.y - centerTextLayoutResult.size.height / 2))
            }
        }
    }
}


// --- EKRAN LISTY ZADAŃ ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    title: String,
    todoList: List<TodoItem>,
    currentRoute: String,
    navController: NavController,
    auth: FirebaseAuth,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (Int) -> Unit,
    onCheckedChange: (TodoItem, Boolean) -> Unit,
    onDeleteClick: (TodoItem) -> Unit,
    onBackClick: (() -> Unit)? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Wróć") }
                    }
                },
                actions = {
                    // Przycisk wylogowania w prawym górnym rogu
                    IconButton(onClick = { performLogout(navController, auth) }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Wyloguj")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        bottomBar = {
            TodoBottomBar(currentRoute = currentRoute, navController = navController)
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAdd) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj zadanie")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }
            items(todoList) { item ->
                TodoRow(item = item, onCheckedChange = { isChecked -> onCheckedChange(item, isChecked) }, onDeleteClick = { onDeleteClick(item) }, onEditClick = { onNavigateToEdit(item.id) })
            }
        }
    }
}

@Composable
fun TodoRow(item: TodoItem, onCheckedChange: (Boolean) -> Unit, onDeleteClick: () -> Unit, onEditClick: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = item.isCompleted, onCheckedChange = onCheckedChange)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.title, style = MaterialTheme.typography.titleMedium, textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null)
                if (item.dueDate.isNotBlank() || item.category.isNotBlank()) {
                    Text(text = "Do: ${item.dueDate} | Kat: ${item.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) { Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Opcje") }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Edytuj") }, onClick = { menuExpanded = false; onEditClick() }, leadingIcon = { Icon(imageVector = Icons.Default.Edit, contentDescription = null) })
                    DropdownMenuItem(text = { Text("Usuń") }, onClick = { menuExpanded = false; onDeleteClick() }, leadingIcon = { Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) })
                }
            }
        }
    }
}

// --- EKRAN FORMULARZA DODAWANIA / EDYCJI ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(itemToEdit: TodoItem?, onSave: (title: String, description: String, date: String, category: String) -> Unit, onCancel: () -> Unit) {
    var title by rememberSaveable { mutableStateOf(itemToEdit?.title ?: "") }
    var description by rememberSaveable { mutableStateOf(itemToEdit?.description ?: "") }
    var dueDate by rememberSaveable { mutableStateOf(itemToEdit?.dueDate ?: "") }
    val categories = listOf("Dom", "Praca", "Sport", "Hobby")
    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by rememberSaveable { mutableStateOf(if (itemToEdit?.category?.isNotEmpty() == true) itemToEdit.category else categories[0]) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val screenTitle = if (itemToEdit == null) "Nowe Zadanie" else "Edytuj Zadanie"

    Scaffold(topBar = { TopAppBar(title = { Text(screenTitle) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Tytuł zadania *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Opis") }, modifier = Modifier.fillMaxWidth().height(100.dp))
            OutlinedTextField(value = dueDate, onValueChange = { }, readOnly = true, label = { Text("Data wykonania") }, modifier = Modifier.fillMaxWidth(), trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.DateRange, contentDescription = "Wybierz datę") } })

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = { TextButton(onClick = { showDatePicker = false; datePickerState.selectedDateMillis?.let { millis -> dueDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(millis)) } }) { Text("OK") } },
                    dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Anuluj") } }
                ) { DatePicker(state = datePickerState) }
            }

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(value = selectedCategory, onValueChange = {}, readOnly = true, label = { Text("Kategoria") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    categories.forEach { category -> DropdownMenuItem(text = { Text(category) }, onClick = { selectedCategory = category; expanded = false }) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Anuluj") }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = { if (title.isNotBlank()) onSave(title, description, dueDate, selectedCategory) }, modifier = Modifier.weight(1f), enabled = title.isNotBlank()) { Text("Zapisz") }
            }
        }
    }
}