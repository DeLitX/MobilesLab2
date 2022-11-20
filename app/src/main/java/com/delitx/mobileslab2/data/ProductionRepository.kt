package com.delitx.mobileslab2.data

import com.delitx.mobileslab2.models.Production
import kotlinx.coroutines.flow.StateFlow

interface ProductionRepository {
    val allProductions: StateFlow<List<Production>>
    val harvestHigher25MFlow: StateFlow<List<Production>>
    val meanPriceFlow: StateFlow<Int>

    suspend fun addItem(production: Production)
}
