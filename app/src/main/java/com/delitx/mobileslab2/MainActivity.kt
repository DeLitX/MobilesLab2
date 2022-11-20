package com.delitx.mobileslab2

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.delitx.mobileslab2.models.ContactModel
import com.delitx.mobileslab2.models.Production
import com.delitx.mobileslab2.ui.MainViewModel
import com.delitx.mobileslab2.ui.theme.MobilesLab2Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val contactRequester =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                onContactPermissionsGranted()
                onContactPermissionsGranted = {}
            }
        }
    private var onContactPermissionsGranted: () -> Unit = {}

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

    @Composable
    fun ProductionScreen(viewModel: MainViewModel) {
        val meanValue by viewModel.meanPriceFlow.collectAsState()
        val allItems by viewModel.allProductions.collectAsState()
        val harvestHigher25M by viewModel.harvestHigher25MFlow.collectAsState()
        var contacts by remember { mutableStateOf(listOf<ContactModel>()) }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
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
            item {
                Button(onClick = {
                    val action = {
                        contacts = getContacts(applicationContext).filter { it.name.last() == 'a' }
                    }
                    if (ContextCompat.checkSelfPermission(
                            applicationContext,
                            android.Manifest.permission.READ_CONTACTS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        action()
                    } else {
                        onContactPermissionsGranted = action
                        contactRequester.launch(android.Manifest.permission.READ_CONTACTS)
                    }
                }) {
                    Text(text = "Get filtered contacts")
                }
            }
            itemsIndexed(contacts) { _, contact ->
                Text(text = "Name: ${contact.name}  Number: ${contact.mobileNumber}")
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

    fun getContacts(ctx: Context): List<ContactModel> {
        val list: MutableList<ContactModel> = mutableListOf()
        val contentResolver: ContentResolver = ctx.contentResolver
        val cursor: Cursor =
            contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null)
                ?: return emptyList()
        if (cursor.count > 0) {
            while (cursor.moveToNext()) {
                val idColumnIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val hasPhoneNumberColumnIndex =
                    cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                if (idColumnIndex < 0 || hasPhoneNumberColumnIndex < 0) {
                    continue
                }
                val id: String =
                    cursor.getString(idColumnIndex)
                if (cursor.getInt(hasPhoneNumberColumnIndex) > 0) {
                    val cursorInfo: Cursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        arrayOf(id),
                        null
                    ) ?: continue
                    while (cursorInfo.moveToNext()) {
                        val displayNameColumnIndex =
                            cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        val numberColumnIndex =
                            cursorInfo.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (displayNameColumnIndex < 0 || numberColumnIndex < 0) {
                            continue
                        }
                        val info = ContactModel(
                            id = id,
                            name = cursor.getString(displayNameColumnIndex),
                            mobileNumber = cursorInfo.getString(numberColumnIndex)
                        )
                        list.add(info)
                    }
                    cursorInfo.close()
                }
            }
            cursor.close()
        }
        return list
    }
}
