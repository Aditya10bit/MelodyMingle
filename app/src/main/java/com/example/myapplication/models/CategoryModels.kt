package com.example.myapplication.models

data class CategoryModels(
    val name: String,
    val coverUrl: String,
    var songs: List<String>
) {
    constructor() : this("", "", listOf())
}
