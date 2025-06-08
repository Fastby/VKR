package com.example.mydiplom.ui.data

data class MockUser(val id: Long, val fullName: String)

object MockUsers {
    val users = listOf(
        MockUser(1L, "Алексей Иванов"),
        MockUser(2L, "Мария Петрова"),
        MockUser(3L, "Дмитрий Смирнов"),
        MockUser(4L, "Екатерина Кузнецова"),
        MockUser(5L, "Игорь Васильев"),
        MockUser(6L, "Анна Соколова"),
        MockUser(7L, "Павел Морозов"),
        MockUser(8L, "Ольга Николаева"),
        MockUser(9L, "Сергей Попов"),
        MockUser(10L, "Юлия Романова")
    )
}