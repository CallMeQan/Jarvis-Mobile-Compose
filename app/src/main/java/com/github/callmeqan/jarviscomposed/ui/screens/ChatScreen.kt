package com.github.callmeqan.jarviscomposed.ui.screens

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.graphics.Color as GraphicColor
import com.github.callmeqan.jarviscomposed.data.ChatMessage
import com.github.callmeqan.jarviscomposed.ui.components.BluetoothDevicePickerDialog
import com.github.callmeqan.jarviscomposed.ui.components.ChatAppBar
import com.github.callmeqan.jarviscomposed.ui.components.MessageBox
import com.github.callmeqan.jarviscomposed.ui.components.MessageInputField
import com.github.callmeqan.jarviscomposed.utils.RetrofitAPI
import com.github.callmeqan.jarviscomposed.utils.SharedViewModel
import com.github.callmeqan.jarviscomposed.utils.getPairedBluetoothDevices
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.round

private const val TAG_NAME = "ChatScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    bluetoothAdapter: BluetoothAdapter,
    viewModel: SharedViewModel,
    onNavigate: () -> Unit
) {
    // Get state from View Model
    val stateURL = viewModel.url
    val stateChatHistory = viewModel.chatHistory
    val stateDevice = viewModel.device

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current // Like `this` keyword in normal java class

    // Bluetooth connection state
    var connectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var socket by remember { mutableStateOf<BluetoothSocket?>(null) }
    var isConnecting by remember { mutableStateOf(false) }
    var connectionError by remember { mutableStateOf<String?>(null) }

    // Bluetooth permission
    val permissions =
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.INTERNET,
        )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { results ->
            Log.d(TAG_NAME, "Result permissions: $results")
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "You have not granted Bluetooth Permission",
                    actionLabel = "",
                    duration = SnackbarDuration.Short
                )
            }
        }
    )

    // For showing Bluetooth picker dialog
    var showBluetoothDialog by remember { mutableStateOf(false) }
    var pairedDevices = remember { mutableStateListOf<BluetoothDevice>() }

    val topBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topBarState)

    // Chatbot UIs init
    var input by remember { mutableStateOf("") }
    var messages = remember { mutableStateListOf<ChatMessage>() }
    if (messages.isEmpty()) {
        messages = stateChatHistory
    }

    LaunchedEffect(Unit) {
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (stateURL == "") {
            // Navigate to setting if stateURL is blank
            onNavigate()
        }
    }

    LaunchedEffect(bluetoothAdapter.isEnabled) {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            context.startActivity(enableBtIntent)
        }
    }

    // CNN model
    val TFLITE_MODEL_NAME = "model_15kb_16ms_dataset_v3.tflite" //should be in assets folder
    val interpreter = Interpreter(
        FileUtil.loadMappedFile(context, TFLITE_MODEL_NAME),
        Interpreter.Options() // TfLite Options
    )
    val NUM_CLASSES = 1

    // Function for CNN model
    fun convertBitmapToByteBuffer(bitmapIn: Bitmap, width: Int, height: Int): ByteBuffer {
        // these value can be different for each channel if they are not then you may have single value instead of an array
        val bitmap = Bitmap.createScaledBitmap(bitmapIn, width, height, false)

        // For Int8
        // val inputImage = ByteBuffer.allocateDirect(width * height * 3 * 1)
        // For float32:
        val inputImage = ByteBuffer.allocateDirect(4 * width * height * 3)
        inputImage.order(ByteOrder.nativeOrder())

        val intValues = IntArray(width * height)
        bitmap.getPixels(intValues, 0, width, 0, 0, width, height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = intValues[y * width + x]
                // Actually import android.graphics.Color but changed
                // to GraphicColor as Compose Color is different
                val r = GraphicColor.red(pixel)
                val g = GraphicColor.green(pixel)
                val b = GraphicColor.blue(pixel)

                // Putting in BRG order because this model demands input in this order
                // inputImage.put(b.toByte())
                // inputImage.put(g.toByte())
                // inputImage.put(r.toByte())
                inputImage.putFloat(b.toFloat())
                inputImage.putFloat(g.toFloat())
                inputImage.putFloat(r.toFloat())
            }
        }
        inputImage.rewind()
        return inputImage
    }
    fun loadBitmapFromAssets(context: Context, fileName: String): Bitmap {
        // Mainly for testing purposes; fileName could
        // the name (e.g., "bright.png") of image in
        // app/src/main/assets/images directory.
        val assetManager = context.assets
        val inputStream = assetManager.open("test_images/" + fileName)
        return BitmapFactory.decodeStream(inputStream)
    }
    fun runModel(bitmap: Bitmap): Int {
        /* Sample use:
        val bitmap = loadBitmapFromAssets(context, input)
        runModel(bitmap)
        */
        try {
            // Get input buffer; convertBitmapToByteBuffer for more info
            val width = 224
            val height = 224

            val inputBuffer = convertBitmapToByteBuffer(bitmap, width, height)
            inputBuffer.rewind()

            // For Int8:
            // val output = Array(1) { ByteArray(NUM_CLASSES) }

            // For float32:
            val output = Array(1) { FloatArray(NUM_CLASSES) }

            // Get the output and status
            interpreter.run(inputBuffer, output)
            val status: Int

            // Threshold: more than 0.6 will trigger action
            if (output[0][0] >= 0.60) status = 1;
            else status = 0;

            // Write the message
            val msg = "It is ${round(output[0][0] * 100).toString()}% dark! ${output[0][0].toString()}"
            Log.d("InferenceResult: ", msg)
            messages.add(
                ChatMessage(
                    message = msg,
                    role = "assistant"
                )
            )
            // If dark return 1, bright return 0
            return status
        }
        catch (e: Exception) {
            scope.launch{
                snackbarHostState.showSnackbar(
                    message = "Cannot inference model!",
                    actionLabel = "",
                    duration = SnackbarDuration.Short
                )
            }
            // If error return -1
            return -1
        }
    }

    // Request permissions if do not have yet
    fun requestPermissions() {
        val notGranted = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            permissionLauncher.launch(notGranted.toTypedArray())
        }

        Log.d(TAG_NAME, "Missing permissions:")
        for (permission in notGranted.toList()) {
            Log.d(TAG_NAME, "   - $permission")
        }
        Log.d(TAG_NAME, "All permissions:")
        for (permission in permissions.toList()) {
            Log.d(TAG_NAME, "   - $permission")
        }
    }

    // Send command to server (check RetrofitAPI for the api code choices)
    fun sendCommand2Server(message: String, role: String = "user", api: String = "fcc") {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS) // Time waiting for connection
            .readTimeout(60, TimeUnit.SECONDS)    // Time for reading data
            .writeTimeout(60, TimeUnit.SECONDS)   // Time for writing data
            .build()

        // On below line we are creating a retrofit
        // Builder and passing our base url
        val retrofit: Retrofit
        try {
            retrofit = Retrofit.Builder()
                .baseUrl(viewModel.url + "/")

                // Custom timeout config (AI model takes a long time to respond)
                .client(okHttpClient)

                // As we are sending data in json format so we have to add Gson converter factory
                .addConverterFactory(GsonConverterFactory.create())

                // At last we are building our retrofit builder.
                .build()
        }
        catch (e: Exception) {
            scope.launch{
                snackbarHostState.showSnackbar(
                    message = "Invalid URL!",
                    actionLabel = "",
                    duration = SnackbarDuration.Short
                )
            }
            return Unit
        }

        // Below line is to create an instance for our retrofit api class.
        val retrofitAPI = retrofit.create(RetrofitAPI::class.java)

        // Passing data from our text fields to our modal class.
        val chatMessage = ChatMessage(message, role)

        // Calling a method to create a post and passing our modal class.
        val call: Call<ChatMessage?>?
        if (api == "bc") {
            call = retrofitAPI.sendToCommandProcessor(chatMessage)
        }
        else {
            call = retrofitAPI.sendToFunctionCallChatbot(chatMessage)
        }

        scope.launch{
            snackbarHostState.showSnackbar(
                message = "Message has being sent to server",
                actionLabel = "",
                duration = SnackbarDuration.Short
            )
        }

        // On below line we are executing our method.
        call!!.enqueue(object : Callback<ChatMessage?> {
            override fun onResponse(call: Call<ChatMessage?>, response: Response<ChatMessage?>) {
                // This method is called when we get response from our api.
                scope.launch{
                    snackbarHostState.showSnackbar(
                        message = "Message sent to API server",
                        actionLabel = "",
                        duration = SnackbarDuration.Short
                    )
                }

                // We are getting response from our body and passing it to our modal class.
                val responseBody: ChatMessage? = response.body()

                // On below line we are getting our data from modal class and adding it to our string.
                if (response.isSuccessful){
                    val responseString = "Response Code : " + "201" + "\n" + "message : " +responseBody!!.message + "\n" + "role : " + responseBody!!.role
                    if (api == "fcc") {
                        messages.add(
                            ChatMessage(
                                message = responseBody.message,
                                role = "assistant"
                            )
                        )
                    }
                    else if (api == "bc") {
                        // Add command of Bluetooth processor
                        messages.add(
                            ChatMessage(
                                message = responseBody.message,
                                role = "user"
                            )
                        )
                    }
                    else {
                        // Add command of Bluetooth processor
                        messages.add(
                            ChatMessage(
                                message = "Wrong API server (server-side error)!",
                                role = "assistant"
                            )
                        )
                    }


                    // Below line we are setting our string to our text view.
                    // This method is called when we get response from our api.
                    scope.launch{
                        snackbarHostState.showSnackbar(
                            message = responseString,
                            actionLabel = "",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
                else {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Null response from Server",
                            actionLabel = "",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            }

            override fun onFailure(call: Call<ChatMessage?>, t: Throwable) {

                // Setting text to our text view when we get error response from API.
                scope.launch{
                    snackbarHostState.showSnackbar(
                        message = "Error found : " + t.message,
                        actionLabel = "",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        })
    }

    // Connect to device (in coroutine)
    suspend fun connectToDevice(device: BluetoothDevice): BluetoothSocket? {
        return withContext(Dispatchers.IO) {
            try {
                val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP UUID
                val tmpSocket = device.createRfcommSocketToServiceRecord(uuid)
                tmpSocket.connect()
                tmpSocket // Connected!
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun sendBtnOnClick() {
//        val inputCopy = input
        val inputCopy = ""
        val bitmap = loadBitmapFromAssets(context, input)
        runModel(bitmap)
        if (inputCopy.isNotBlank()) {
            Log.d(TAG_NAME, "Message place - input: $inputCopy")
            messages.add(
                ChatMessage(
                    message = inputCopy,
                    role = "user"
                )
            )

            // TODO: If isToAI = False (could be toggled)
            // TODO: 3 modes - Casual, Func call and Command LLM
            // TODO: Func call (fcc), Command (bc) and other
            sendCommand2Server(
                message = inputCopy,
                role = "user",
            )
            // When connected, send message to device
            if (socket != null && socket!!.isConnected) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        socket!!.outputStream.write(inputCopy.toByteArray())
                        socket!!.outputStream.flush()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            input = ""
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = matches?.getOrNull(0)
            if (recognizedText != null) {
                Log.i(TAG_NAME, "Recognized text: $recognizedText")

                // TODO: Add function to connect server, feed text to AI

                messages.add(
                    ChatMessage(
                        message = recognizedText,
                        role = "user"
                    )
                )
            }
        }
    }

    fun micBtnOnClick() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
        }
        speechLauncher.launch(intent)
    }

    fun bluetoothBtnOnClick() {
        // Helper function to check if all permissions are granted
        val hasRequiredPermissions = permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (hasRequiredPermissions) {
            // Get devices
            pairedDevices = getPairedBluetoothDevices(bluetoothAdapter)
            showBluetoothDialog = true
        } else {
            requestPermissions()
        }
    }

    fun settingBtnOnClick() {
        onNavigate()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier
            .navigationBarsPadding()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = ScaffoldDefaults
            .contentWindowInsets
            .exclude(WindowInsets.navigationBars)
            .exclude(WindowInsets.ime),
        topBar = {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                //throw Error("No permission")
            }
            ChatAppBar(
                statusTxt = when {
                    isConnecting -> "Connecting..."
                    socket != null && socket!!.isConnected -> "Connected: ${connectedDevice?.name ?: ""}"
                    connectionError != null -> "Error: $connectionError"
                    else -> "Not connected"
                },
                settingBtnOnClick = { settingBtnOnClick() },
                bluetoothBtnOnClick = { bluetoothBtnOnClick() },
                showBluetoothConfig = true
            )
        },
        containerColor = Color.Transparent,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues = paddingValues)
                .padding(bottom = 16.dp)
                .fillMaxSize(),
//            verticalArrangement = Arrangement.Bottom
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                reverseLayout = true,
            ) {
                items(messages.reversed()) { message ->
                    MessageBox(message = message)
                }
            }

            MessageInputField(
                value = input,
                onValueChange = { input = it },
                sendBtnOnClick = ::sendBtnOnClick,
                micBtnOnClick = ::micBtnOnClick
            )
        }
    }

    // Bluetooth device picker dialog
    if (showBluetoothDialog) {
        BluetoothDevicePickerDialog(
            devices = pairedDevices,
            onDismiss = { showBluetoothDialog = false },
            onDeviceSelected = { device ->
                showBluetoothDialog = false
                // Start connecting, update state
                isConnecting = true
                connectionError = null
                connectedDevice = device

                // Update view model
                viewModel.connectTo(device)

                // Try to connect in background
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val result = connectToDevice(device)
                        socket = result
                        isConnecting = false
                        connectionError = if (result == null) "Failed to connect" else null
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        )
    }
}