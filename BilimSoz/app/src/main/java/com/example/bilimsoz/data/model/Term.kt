//  Bilimsoz
//
//  Created by Miras Khalenov  on 15.05.2025.
//
package com.example.bilimsoz.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Term(
    val id: String = "",
    val kazakh: String,
    val russian: String,
    val english: String,
    val description: String,
    val subject: String = ""
) {
    fun getDisplayName(language: AppLanguage): String {
        return when (language) {
            AppLanguage.KAZAKH -> kazakh
            AppLanguage.RUSSIAN -> russian
            AppLanguage.ENGLISH -> english
        }
    }
}

@Serializable
data class GoogleSheetsResponse(
    val range: String,
    val majorDimension: String,
    val values: List<List<String>>
)

enum class AppLanguage(val code: String, val displayName: String) {
    KAZAKH("kk", "Қазақша"),
    RUSSIAN("ru", "Русский"),
    ENGLISH("en", "English")
}

enum class Subject(val nameRu: String, val nameKk: String, val nameEn: String, val iconName: String) {
    MATHEMATICS("Математика", "Математика", "Mathematics", "calculate"),
    ALGEBRA("Алгебра", "Алгебра", "Algebra", "functions"),
    GEOMETRY("Геометрия", "Геометрия", "Geometry", "pentagon"),
    PHYSICS("Физика", "Физика", "Physics", "science"),
    BIOLOGY("Биология", "Биология", "Biology", "nature"),
    CHEMISTRY("Химия", "Химия", "Chemistry", "biotech"),
    COMPUTER_SCIENCE("Информатика", "Информатика", "Computer Science", "computer"),
    GEOGRAPHY("География", "География", "Geography", "public"),
    NATURAL_SCIENCE("Естествознание", "Жаратылыстану", "Natural Science", "eco");

    fun getDisplayName(language: AppLanguage): String {
        return when (language) {
            AppLanguage.KAZAKH -> nameKk
            AppLanguage.RUSSIAN -> nameRu
            AppLanguage.ENGLISH -> nameEn
        }
    }
}