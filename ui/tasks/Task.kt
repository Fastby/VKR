package com.example.mydiplom.ui.tasks


data class Task (
    val id: Long,
    val name: String,
    val description: String,
    val completionTime: Int,
    val status: Int
)

data class TaskDto(
    val name: String,
    val description: String,
    val completionTime: Int,
    val userIds: List<Long>

)

