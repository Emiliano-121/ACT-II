package com.example.tareasv1

import android.content.Context
import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tareasv1.data.Tarea
import com.example.tareasv1.ui.theme.Tareasv1Theme

// --- Paleta de Colores ---
val PurplePrimary = Color(0xFF996BF1)
val BlueTitle = Color(0xFF4B85E9)
val LightPurpleCard = Color(0xFFF2EFFF)
val AppBackgroundColor = Color(0xFFF8F8F8)
val SelectedFilter = Color(0xFF996BF1)
val UnselectedFilter = Color(0xFFB999FE)

enum class NoteFilter { NOTAS, RECORDATORIOS, OCULTAS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tareasv1Theme {
                Surface(modifier = Modifier.fillMaxSize(), color = AppBackgroundColor) {
                    AppNavigation() // El nuevo punto de entrada
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("notitas_prefs", Context.MODE_PRIVATE) }
    val isLoggedIn = remember { prefs.getBoolean("logged_in", false) }

    NavHost(navController = navController, startDestination = if (isLoggedIn) "notes" else "login") {
        composable("login") {
            LoginScreen(navController = navController, prefs = prefs)
        }
        composable("notes") {
            TareasApp() // Nuestra app de notas
        }
    }
}

@Composable
fun LoginScreen(navController: NavController, prefs: android.content.SharedPreferences) {
    var email by remember { mutableStateOf("") }
    var acceptedTerms by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Crea una cuenta", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BlueTitle)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Ingresa tu correo electrónico para registrarte", color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("correo@electronico.com") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { 
                errorMessage = null // Reset error message
                if (!acceptedTerms) {
                    errorMessage = "Debes aceptar los términos y condiciones"
                    return@Button
                }
                if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    errorMessage = "Por favor, ingresa un correo electrónico válido"
                    return@Button
                }

                // Guardar estado de sesión y navegar
                prefs.edit()
                    .putBoolean("logged_in", true)
                    .putString("user_email", email)
                    .apply()
                
                navController.navigate("notes") {
                    popUpTo("login") { inclusive = true } // Evita volver al login
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
        ) {
            Text("Continuar", fontSize = 18.sp)
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = acceptedTerms, onCheckedChange = { acceptedTerms = it })
            Spacer(modifier = Modifier.width(8.dp))
            ClickableText(text = AnnotatedString("Acepto los términos y condiciones"), onClick = { /* TODO */ })
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TareasApp() {
    var tareas by remember { mutableStateOf(listOf<Tarea>()) }
    var showDialog by remember { mutableStateOf(false) }
    var tareaToEdit by remember { mutableStateOf<Tarea?>(null) }
    var currentFilter by remember { mutableStateOf(NoteFilter.NOTAS) }

    val filteredTareas = tareas.filter {
        when (currentFilter) {
            NoteFilter.NOTAS -> !it.isHidden && !it.isReminder
            NoteFilter.RECORDATORIOS -> it.isReminder
            NoteFilter.OCULTAS -> it.isHidden
        }
    }
    val (favoriteTareas, normalTareas) = filteredTareas.partition { it.isFavorite }

    Scaffold(
        containerColor = AppBackgroundColor,
        topBar = { TopAppBar(title = { Text("Notitas", color = BlueTitle, fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackgroundColor)) },
        floatingActionButton = { FloatingActionButton(onClick = { tareaToEdit = null; showDialog = true }, containerColor = PurplePrimary) { Icon(Icons.Filled.Add, "Agregar Tarea", tint = Color.White) } }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp)) {
            FilterButtons(selected = currentFilter, onSelect = { currentFilter = it })
            Spacer(modifier = Modifier.height(16.dp))

            if (favoriteTareas.isNotEmpty()) {
                Text("Favoritas", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(favoriteTareas) { tarea ->
                        TareaCard(tarea, true, { tareaToEdit = tarea; showDialog = true }, { toggleFavorite(tarea, tareas) { tareas = it } }, { tareas = tareas - tarea })
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (normalTareas.isNotEmpty()) {
                Text("Notas", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(normalTareas) { tarea ->
                        TareaCard(tarea, false, { tareaToEdit = tarea; showDialog = true }, { toggleFavorite(tarea, tareas) { tareas = it } }, { tareas = tareas - tarea })
                    }
                }
            }
        }
    }

    if (showDialog) {
        EditTareaDialog(tareaToEdit, { showDialog = false }) { newTarea ->
            if (tareaToEdit == null) { tareas = tareas + newTarea }
            else { val i = tareas.indexOf(tareaToEdit); if (i != -1) { val mut = tareas.toMutableList(); mut[i] = newTarea; tareas = mut.toList() } }
            showDialog = false
        }
    }
}

@Composable
fun FilterButtons(selected: NoteFilter, onSelect: (NoteFilter) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NoteFilter.values().forEach { filter ->
            Button(onClick = { onSelect(filter) }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = if (selected == filter) SelectedFilter else UnselectedFilter)) {
                Text(filter.name.lowercase().replaceFirstChar { it.uppercase() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTareaDialog(tarea: Tarea?, onDismiss: () -> Unit, onSave: (Tarea) -> Unit) {
    var text by remember { mutableStateOf(tarea?.cuerpo ?: "") }
    var isReminder by remember { mutableStateOf(tarea?.isReminder ?: false) }
    var isHidden by remember { mutableStateOf(tarea?.isHidden ?: false) }

    val switchColors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PurplePrimary)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (tarea == null) "Nueva Nota" else "Editar Nota") },
        text = {
            Column {
                TextField(value = text, onValueChange = { text = it }, label = { Text("Escribe algo...") }, colors = TextFieldDefaults.colors(focusedIndicatorColor = PurplePrimary, cursorColor = PurplePrimary, focusedLabelColor = PurplePrimary))
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isReminder, onCheckedChange = { isReminder = it }, colors = switchColors)
                    Spacer(Modifier.width(8.dp)); Text("Recordatorio")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = isHidden, onCheckedChange = { isHidden = it }, colors = switchColors)
                    Spacer(Modifier.width(8.dp)); Text("Ocultar Nota")
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(Tarea(text, tarea?.isFavorite ?: false, isHidden, isReminder)) }, colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)) { Text("Guardar") } },
        dismissButton = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = UnselectedFilter)) { Text("Cancelar") } }
    )
}

fun toggleFavorite(tarea: Tarea, currentTareas: List<Tarea>, updateTareas: (List<Tarea>) -> Unit) {
    val index = currentTareas.indexOf(tarea)
    if (index != -1) {
        val mutableTareas = currentTareas.toMutableList(); mutableTareas[index] = tarea.copy(isFavorite = !tarea.isFavorite); updateTareas(mutableTareas.toList())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TareaCard(tarea: Tarea, isLarge: Boolean, onCardClick: () -> Unit, onFavoriteToggle: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = (if (isLarge) Modifier.size(150.dp) else Modifier.fillMaxWidth()), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = LightPurpleCard), onClick = onCardClick) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = if (isLarge) Arrangement.SpaceBetween else Arrangement.Center) {
            Text(text = tarea.cuerpo ?: "", fontSize = if (isLarge) 22.sp else 18.sp, fontWeight = FontWeight.Bold)
            if (isLarge) Spacer(modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isLarge) Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onFavoriteToggle) { Icon(imageVector = if (tarea.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline, contentDescription = "Marcar como Favorita", tint = if (tarea.isFavorite) Color(0xFFFFD700) else Color.Gray) }
                IconButton(onClick = onDelete) { Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar Nota", tint = Color.Gray) }
            }
        }
    }
}
