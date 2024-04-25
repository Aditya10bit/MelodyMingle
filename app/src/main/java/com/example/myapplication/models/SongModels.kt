package com.example.myapplication.models

import android.icu.text.CaseMap.Title

data class SongModels(
    val id : String,
    val title: String,
    val subtitle: String,
    val url: String,
    val coverUrl: String,
){
    constructor() : this("", "", "","","")
}
