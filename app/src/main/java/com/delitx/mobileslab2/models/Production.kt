package com.delitx.mobileslab2.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Production(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val year: Int,
    val mass: Int,
    val price: Int
)
