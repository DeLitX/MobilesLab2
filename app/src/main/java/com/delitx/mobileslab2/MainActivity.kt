package com.delitx.mobileslab2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.delitx.mobileslab2.models.Production
import com.delitx.mobileslab2.ui.MainViewModel
import com.delitx.mobileslab2.ui.theme.MobilesLab2Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MobilesLab2Theme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    ProductionScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun ProductionScreen(viewModel: MainViewModel) {
    val meanValue by viewModel.meanPriceFlow.collectAsState()
    val allItems by viewModel.allProductions.collectAsState()
    val harvestHigher25M by viewModel.harvestHigher25MFlow.collectAsState()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            InsertProductionItemScreen(Modifier.fillMaxWidth(), onInsert = viewModel::addItem)
        }
        item {
            Text(text = "Mean price of production per year: $meanValue")
        }
        item {
            Text(text = "Productions higher than 25M tonns per year")
        }
        itemsIndexed(harvestHigher25M) { _, production ->
            ProductionLayout(production = production, modifier = Modifier.fillMaxWidth())
        }
        item {
            Text(text = "All productions")
        }
        itemsIndexed(allItems) { _, production ->
            ProductionLayout(production = production, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun InsertProductionItemScreen(modifier: Modifier = Modifier, onInsert: (Production) -> Unit) {
    Column(modifier = modifier) {
        var year by remember { mutableStateOf("") }
        var mass by remember { mutableStateOf("") }
        var price by remember { mutableStateOf("") }
        TextField(value = year, onValueChange = { year = it }, label = { Text(text = "Year") })
        TextField(
            value = mass,
            onValueChange = { mass = it },
            label = { Text(text = "Mass in tonns") }
        )
        TextField(
            value = price,
            onValueChange = { price = it },
            label = { Text(text = "Price in $") }
        )
        val isInputValid = run {
            try {
                require(year.toInt() in 1..2022)
                require(price.toInt() >= 0)
                require(mass.toInt() >= 0)
                true
            } catch (e: Exception) {
                false
            }
        }
        Button(
            onClick = {
                onInsert(
                    Production(
                        year = year.toInt(),
                        mass = mass.toInt(),
                        price = price.toInt()
                    )
                )
            },
            enabled = isInputValid
        ) {
            Text(text = "Insert")
        }
    }
}

@Composable
fun ProductionLayout(production: Production, modifier: Modifier = Modifier) {
    Text(
        text = "Year: ${production.year}   Mass: ${production.mass} tonn  Price: ${production.price}$",
        modifier = modifier
    )
}
