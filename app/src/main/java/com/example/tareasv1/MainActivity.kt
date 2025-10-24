package com.example.tareasv1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tareasv1.data.Tarea
import com.example.tareasv1.ui.theme.Tareasv1Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Tareasv1Theme {
                TareasApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TareasApp() {
    var tareas by remember { mutableStateOf(listOf<Tarea>()) }
    var showDialog by remember { mutableStateOf(false) }
    var tareaActual by remember { mutableStateOf<Tarea?>(null) }
    var textoTarea by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notas") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                tareaActual = null
                textoTarea = ""
                showDialog = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar Tarea")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(tareas) { tarea ->
                TareaItem(
                    tarea = tarea,
                    onEdit = {
                        tareaActual = tarea
                        textoTarea = tarea.cuerpo ?: ""
                        showDialog = true
                    },
                    onDelete = {
                        tareas = tareas - tarea
                    }
                )
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (tareaActual == null) "Agregar Tarea" else "Editar Tarea") },
            text = {
                TextField(
                    value = textoTarea,
                    onValueChange = { textoTarea = it },
                    label = { Text("Descripción") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (textoTarea.isNotBlank()) {
                            val newTarea = Tarea(textoTarea)
                            if (tareaActual == null) {
                                tareas = tareas + newTarea
                            } else {
                                val index = tareas.indexOf(tareaActual)
                                if (index != -1) {
                                    val mutableTareas = tareas.toMutableList()
                                    mutableTareas[index] = newTarea
                                    tareas = mutableTareas.toList()
                                }
                            }
                            showDialog = false
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun TareaItem(tarea: Tarea, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = tarea.cuerpo ?: "",
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = "Editar")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Eliminar")
        }
    }
}