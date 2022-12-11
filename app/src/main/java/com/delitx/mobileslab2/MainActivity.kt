package com.delitx.mobileslab2

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.delitx.mobileslab2.models.ContactModel
import com.delitx.mobileslab2.models.Production
import com.delitx.mobileslab2.ui.MainViewModel
import com.delitx.mobileslab2.ui.bar_chart.BarChart
import com.delitx.mobileslab2.ui.bar_chart.BarChartData
import com.delitx.mobileslab2.ui.huffman_coding.getCharOccurRate
import com.delitx.mobileslab2.ui.huffman_coding.toHuffmanMap
import com.delitx.mobileslab2.ui.theme.MobilesLab2Theme
import dagger.hilt.android.AndroidEntryPoint
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val permissionsToGrant = listOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private var onPermissionReceived: () -> Unit = {}

    private val permissionRequester =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (!permissions.any { !it.value }) {
                onPermissionReceived()
                onPermissionReceived = {}
            }
        }

    private var onFileSelected: (Uri) -> Unit = {}
    private val fileSelector =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try {
                    onFileSelected(uri)
                    onFileSelected = {}
                } catch (e: IllegalArgumentException) {
                }
            }
        }

    private var onFileSaveSelected: (Uri) -> Unit = {}
    private val fileSaver =
        registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->
            if (uri != null) {
                try {
                    onFileSaveSelected(uri)
                    onFileSaveSelected = {}
                } catch (e: Throwable) {
                    val ex = e
                }
            }
        }

    private val contactRequester =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                onContactPermissionsGranted()
                onContactPermissionsGranted = {}
            }
        }
    private var onContactPermissionsGranted: () -> Unit = {}

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MobilesLab2Theme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val navController = rememberNavController()
                    Scaffold(bottomBar = { BottomNavigation(navController = navController) }) { paddings ->
                        Box(modifier = Modifier.padding(bottom = paddings.calculateBottomPadding())) {
                            NavigationGraph(navController = navController)
                        }
                    }
                }
            }
        }
    }

    sealed class BottomNavItem(var title: String, var icon: Int, var screen_route: String) {

        object Huffman : BottomNavItem("Huffman", R.drawable.ic_lock, "huffman")
        object DBAndContacts :
            BottomNavItem("DB and contacts", R.drawable.ic_contacts, "db_and_contacts")

        object Tutorial : BottomNavItem("Tutorial", R.drawable.ic_info, "tutorial")
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun HuffmanMainUI() {
        var text by rememberSaveable { mutableStateOf("") }
        var encodedText by rememberSaveable { mutableStateOf("") }
        var encodingCodes by rememberSaveable { mutableStateOf<Map<Char, String>>(mapOf()) }
        var probabilities by rememberSaveable {
            mutableStateOf<List<Pair<Char, Int>>>(
                listOf()
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(1f)
                .padding(horizontal = 20.dp)
        ) {
            item {
                TextField(value = text, onValueChange = { text = it })
                Button(onClick = {
                    whenPermissionsGranted {
                        selectFile { uri ->
                            text = readFile(uri)
                        }
                    }
                }) {
                    Text(text = "Load text from file")
                }
                Text(text = "Start length:${text.toBinary().length}")
                Text(text = "Start binary:" + text.toBinary())
                Button(onClick = {
                    val map = text.toHuffmanMap()
                    encodedText = map.encodeText(text.toList())
                    encodingCodes = map.codesTable
                    probabilities = text.getCharOccurRate()
                }) {
                    Text(text = "Encode")
                }
                Text(text = "Encoded length:${encodedText.length}")
                Text(text = "Encoded text:$encodedText")
                Text(text = "Encode code table:")
            }
            items(encodingCodes.toList()) { (symbol, code) ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "\'$symbol\'",
                        modifier = Modifier.fillParentMaxWidth(0.5f)
                    )
                    Text(text = code)
                }
            }
            item {
                if (probabilities.isNotEmpty()) {
                    BarChart(
                        barChartData = BarChartData(
                            probabilities.map {
                                BarChartData.Bar(
                                    value = it.second.toFloat(),
                                    color = Color.Cyan,
                                    label = "\'${it.first}\'"
                                )
                            }
                        ),
                        modifier = Modifier
                            .height(300.dp)
                            .fillParentMaxHeight(1f)
                    )
                    Button(onClick = {
                        whenPermissionsGranted {
                            saveFile { uri ->
                                writeFile(
                                    uri,
                                    """
                                        Encoded text: $encodedText
                                        Encoding codes: $encodingCodes
                                    """.trimIndent()
                                )
                            }
                        }
                    }) {
                        Text(text = "Save result to file")
                    }
                }
            }
        }
    }

    private fun writeFile(uri: Uri, content: String) {
        contentResolver.openFileDescriptor(uri, "w")
            ?.use { descriptor ->
                FileOutputStream(descriptor.fileDescriptor).use { inputStream ->
                    inputStream.write(content.toByteArray())
                }
            }
    }

    private fun readFile(uri: Uri): String {
        val stringBuilder = StringBuilder()
        contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    stringBuilder.append('\n')
                    line = reader.readLine()
                }
            }
        }
        return stringBuilder.toString()
    }

    private fun saveFile(action: (Uri) -> Unit) {
        onFileSaveSelected = action
        fileSaver.launch("${System.currentTimeMillis()}.txt")
    }

    private fun selectFile(action: (Uri) -> Unit) {
        onFileSelected = action
        fileSelector.launch(arrayOf("text/plain"))
    }

    private fun whenPermissionsGranted(action: () -> Unit) {
        onPermissionReceived = action
        permissionRequester.launch(permissionsToGrant.toTypedArray())
    }

    @Composable
    fun NavigationGraph(navController: NavHostController) {
        NavHost(navController, startDestination = BottomNavItem.Huffman.screen_route) {
            composable(BottomNavItem.Huffman.screen_route) {
                HuffmanMainUI()
            }
            composable(BottomNavItem.DBAndContacts.screen_route) {
                ProductionScreen(viewModel)
            }
            composable(BottomNavItem.Tutorial.screen_route) {
                TutorialScreen()
            }
        }
    }

    @Composable
    fun TutorialScreen() {
        LazyColumn(modifier = Modifier.padding(horizontal = 20.dp)) {
            item {
                Text(text = getString(R.string.tutorial))
            }
            item {
                Text(text = "Автор додатку: Шабанов Дмитро ТТП-41")
            }
            item {
                Image(painter = painterResource(id = R.drawable.photo), contentDescription = null)
            }
        }
    }

    @Composable
    fun BottomNavigation(navController: NavController) {
        val items = listOf(
            BottomNavItem.Huffman,
            BottomNavItem.DBAndContacts,
            BottomNavItem.Tutorial
        )
        BottomNavigation(
            backgroundColor = MaterialTheme.colors.primary,
            contentColor = Color.Black
        ) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            items.forEach { item ->
                BottomNavigationItem(
                    icon = {
                        Icon(
                            painterResource(id = item.icon),
                            contentDescription = item.title
                        )
                    },
                    label = {
                        Text(
                            text = item.title,
                            fontSize = 9.sp
                        )
                    },

                    selectedContentColor = Color.Black,
                    unselectedContentColor = Color.Black.copy(0.4f),
                    alwaysShowLabel = true,
                    selected = currentRoute == item.screen_route,
                    onClick = {
                        navController.navigate(item.screen_route) {
                            navController.graph.startDestinationRoute?.let { screen_route ->
                                popUpTo(screen_route) {
                                    saveState = true
                                }
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun ProductionScreen(viewModel: MainViewModel) {
        val meanValue by viewModel.meanPriceFlow.collectAsState()
        val allItems by viewModel.allProductions.collectAsState()
        val harvestHigher25M by viewModel.harvestHigher25MFlow.collectAsState()
        var contacts by remember { mutableStateOf(listOf<ContactModel>()) }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
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

fun String.toBinary(): String {
    val resultBuilder = StringBuilder()
    for (char in this) {
        resultBuilder.append(Integer.toBinaryString(char.code))
    }
    return resultBuilder.toString()
}
