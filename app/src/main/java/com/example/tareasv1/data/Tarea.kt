package com.example.tareasv1.data

data class Tarea(
    var cuerpo: String?,
    var isFavorite: Boolean = false,
    var isHidden: Boolean = false,
    var isReminder: Boolean = false
)
