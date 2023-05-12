package com.example.jumpropecounter.Utils

import android.content.SharedPreferences

fun SharedPreferences.edit(actions: SharedPreferences.Editor.() -> Unit) {
    with (edit()) {
        actions(this)
        apply()
    }
}
