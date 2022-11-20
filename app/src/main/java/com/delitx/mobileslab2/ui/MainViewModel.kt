package com.delitx.mobileslab2.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delitx.mobileslab2.data.ProductionRepository
import com.delitx.mobileslab2.models.Production
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ProductionRepository
) : ViewModel() {
    val allProductions = repository.allProductions
    val harvestHigher25MFlow = repository.harvestHigher25MFlow
    val meanPriceFlow = repository.meanPriceFlow

    fun addItem(item: Production) {
        viewModelScope.launch {
            repository.addItem(item)
        }
    }
}
