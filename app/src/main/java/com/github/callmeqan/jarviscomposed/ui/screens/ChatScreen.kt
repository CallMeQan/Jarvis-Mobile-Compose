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
import android.graphics.Matrix
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.callmeqan.jarviscomposed.data.ChatMessage
import com.github.callmeqan.jarviscomposed.ui.components.BluetoothDevicePickerDialog
import com.github.callmeqan.jarviscomposed.ui.components.CameraCaptureButton
import com.github.callmeqan.jarviscomposed.ui.components.ChatAppBar
import com.github.callmeqan.jarviscomposed.ui.components.LlmModeDropUp
import com.github.callmeqan.jarviscomposed.ui.components.MessageBox
import com.github.callmeqan.jarviscomposed.ui.components.MessageInputField
import com.github.callmeqan.jarviscomposed.utils.RetrofitAPI
import com.github.callmeqan.jarviscomposed.utils.SharedViewModel
import com.github.callmeqan.jarviscomposed.utils.getPairedBluetoothDevices
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.tensorflow.lite.DataType
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.ByteBuffer
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
    var apiChatbot = viewModel.apiMode
    val stateDevice = viewModel.device

    // Snack bar
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
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
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
    var messages = remember { mutableStateListOf<ChatMessage>() } //chat log
    if (messages.isEmpty()) {
        messages = stateChatHistory
    }

    LaunchedEffect(Unit) {
//        val notGranted = permissions.filter {
//            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
//        }
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


    // Camera setup
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { ContextCompat.getMainExecutor(context) }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
        }
    }

    @Composable
    fun CameraPreview(
        imageCapture: ImageCapture,
        modifier: Modifier = Modifier,
    ) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val previewUseCase = Preview.Builder()
                        .build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            previewUseCase,
                            imageCapture
                        )
                    } catch (exc: Exception) {
                        Log.e("CameraPreview", "Use case binding failed", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )
    }

    fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    @Composable
    fun CameraCaptureOnce(onBitmapCaptured: (Bitmap) -> Unit) {
        val imageCapture = remember { ImageCapture.Builder().build() }
        var captured by remember { mutableStateOf(false) }

        Box(Modifier.fillMaxSize()) {
            CameraPreview(imageCapture = imageCapture, modifier = Modifier.matchParentSize())

            Button(
                onClick = {
                    if (!captured) {
                        captured = true
                        imageCapture.takePicture(
                            executor,
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                    // As original image is rotated
                                    // we must capture and re-rotate it.
                                    // Must be done before putting into UI
                                    // as it will mess things up.
                                    var bitmap = imageProxy.toBitmap()
                                    bitmap = bitmap.rotate(90f)
                                    imageProxy.close()
                                    onBitmapCaptured(bitmap)
                                }

                                override fun onError(exc: ImageCaptureException) {
                                    Log.e("CameraCapture", "Capture failed", exc)
                                }
                            }
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(if (captured) "Captured" else "Capture")
            }
        }
    }

    // CNN model
    val tfliteModelName =
        "model_15kb_13e-2ms_dataset_v3.tflite" //should be in assets folder model_17kb
    val numClasses = 1
    val interpreter = Interpreter(
        FileUtil.loadMappedFile(context, tfliteModelName),
        Interpreter.Options() // TfLite Options
    )

    // Function for CNN model
    fun convertBitmapToByteBuffer(bitmapUnresized: Bitmap, width: Int, height: Int): ByteBuffer {
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(height, width, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        // Initialize TensorImage for float32 model
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmapUnresized)  // loads and applies the Bitmap
        val processedImage = imageProcessor.process(tensorImage)

        val buffer = processedImage.buffer
        return buffer
    }

    fun loadBitmapFromAssets(context: Context, fileName: String): Bitmap {
        // Mainly for testing purposes; fileName could
        // the name (e.g., "bright.png") of image in
        // app/src/main/assets/images directory.
        val assetManager = context.assets
        val inputStream = assetManager.open("test_images/$fileName")
        return BitmapFactory.decodeStream(inputStream)
    }

    fun runModel(context: Context, bitmap: Bitmap): Int {
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
            // val output = Array(1) { ByteArray(numClasses) }

            // For float32:
            val output = Array(1) { FloatArray(numClasses) }

            // Get the output and status
            interpreter.run(inputBuffer, output)
            val status: Int
            if (output[0][0] > 0.6) {
                status = 1
            } else {
                status = 0
            }

            // Write the message
            val msg = "It is ${round(output[0][0] * 100)}% dark (${output[0][0]})!"
            Log.d("InferenceResult: ", msg)
            messages.add(
                ChatMessage(
                    message = msg,
                    role = "assistant"
                )
            )
            // If dark return 1, bright return 0
            return status
        } catch (_: Exception) {
            scope.launch {
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

    fun onResultCNN(result: Int, severMsg: String = "") {
        apiChatbot = "chatbot/bluetooth_processor"
        viewModel.updateApi(apiChatbot)
        if (result == 1) {
            messages.add(
                ChatMessage(
                    message = "The place seems to be dark! Would you need some light?",
                    role = "assistant"
                )
            )
        } else if (result == 0) {
            messages.add(
                ChatMessage(
                    message = "The place seems to be bright! Would you want to turn off the light?",
                    role = "assistant"
                )
            )
        } else {
            messages.add(
                ChatMessage(
                    message = "There are some errors: $severMsg",
                    role = "assistant"
                )
            )
        }
    }

    // Request permissions if do not have yet
    fun requestPermissions() {
        val notGranted = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) != PackageManager.PERMISSION_GRANTED
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

    // Send message to ESP32
    suspend fun sendCommand2ESP32(input: String) {
        // When connected, send message to device
        val commands: List<String> = input.split(";")
            .filter { it.isNotBlank() }  // Removes the last empty string if any

        for (command in commands) {
            // Add command of Bluetooth processor
            messages.add(
                ChatMessage(
                    message = "Your command: $command",
                    role = "assistant"
                )
            )

            // If connected then send the message
            if (socket != null && socket!!.isConnected) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        socket!!.outputStream.write(command.toByteArray())
                        socket!!.outputStream.flush()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            delay(1000L) // pause for 3 seconds
        }
    }

    // Send command to server (check RetrofitAPI for the api code choices)
    fun sendMessageToServer(message: String, role: String = "user", api: String = "fcc") {
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
        } catch (_: Exception) {
            scope.launch {
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
        if (api == "chatbot/bluetooth_processor") {
            call = retrofitAPI.sendToCommandProcessor(chatMessage)
        } else if (api == "chatbot/function_call_chatbot") {
            call = retrofitAPI.sendToFunctionCallChatbot(chatMessage)
        } else {
            // (api == "chatbot/vanilla")
            call = retrofitAPI.sendToVanillaChatbot(chatMessage)
        }

        scope.launch {
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
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Message sent to API server",
                        actionLabel = "",
                        duration = SnackbarDuration.Short
                    )
                }

                // We are getting response from our body and passing it to our modal class.
                val responseBody: ChatMessage? = response.body()

                // On below line we are getting our data from modal class and adding it to our string.
                if (response.isSuccessful) {
                    val responseString =
                        "Response Code : " + "201" + "\n" + "message : " + responseBody!!.message + "\n" + "role : " + responseBody.role
                    // When logic, like if but funner
                    when (api) {
                        "chatbot/vanilla" -> {
                            messages.add(
                                ChatMessage(
                                    message = responseBody.message,
                                    role = "assistant"
                                )
                            )
                        }

                        "chatbot/function_call_chatbot" -> {
                            messages.add(
                                ChatMessage(
                                    message = responseBody.message,
                                    role = "assistant"
                                )
                            )
                        }

                        "chatbot/bluetooth_processor" -> {
                            // Messages' addition will be done in the function
                            scope.launch {
                                sendCommand2ESP32(responseBody.message)
                            }
                        }

                        else -> {
                            // Add command of Bluetooth processor
                            messages.add(
                                ChatMessage(
                                    message = "Wrong API server (server-side error)!",
                                    role = "assistant"
                                )
                            )
                        }
                    }


                    // Below line we are setting our string to our text view.
                    // This method is called when we get response from our api.
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = responseString,
                            actionLabel = "",
                            duration = SnackbarDuration.Short
                        )
                    }
                } else {
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
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Error found : " + t.message,
                        actionLabel = "",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        })
    }

    fun sendBtnOnClick() {
        val inputCopy = input
        if (inputCopy.isNotBlank()) {
            Log.d(TAG_NAME, "Message place - input: $inputCopy")
            messages.add(
                ChatMessage(
                    message = inputCopy,
                    role = "user"
                )
            )

            // Send Message to chatbot
            sendMessageToServer(
                message = inputCopy,
                role = "user",
                api = apiChatbot
            )
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
                // TODO: Add function to connect server, feed text to AI / google voice recognition
                messages.add(
                    ChatMessage(
                        message = recognizedText,
                        role = "user"
                    )
                )
                sendMessageToServer(
                    message = recognizedText,
                    role = "user",
                    api = apiChatbot
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
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
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

    val isLoggedIn = viewModel.uiState.collectAsState().value.loginSuccess
    var showLoginScreen by remember { mutableStateOf(false) }

    fun loginLogoutBtnOnClick() {
        if (isLoggedIn) {
            viewModel.logout(context)
        } else {
            showLoginScreen = true
        }
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
                showBluetoothConfig = true,
                isLoggedIn = isLoggedIn,
                loginLogoutBtnOnClick = { loginLogoutBtnOnClick() }
            )
        },
        containerColor = Color.Transparent,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues = paddingValues)
                .padding(bottom = 16.dp)
                .fillMaxSize(),
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

            var capturing by remember { mutableStateOf(false) }
            Row {
                CameraCaptureButton(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    // On click function to change camera status
                    capturing = !capturing
                }
                LlmModeDropUp(
                    viewModel = viewModel
                ) { code ->
                    viewModel.updateApi(newApi = code)
                    apiChatbot = code
                }
            }

            if (capturing) {
                Column {
                    CameraCaptureOnce { bmp ->
                        // Use captured bitmap
                        Log.d("MainActivity", "Bitmap captured: ${bmp.width}×${bmp.height}")

                        // Add bitmap to chat history
                        messages.add(
                            ChatMessage(message = "", image = bmp, role = "user")
                        )

                        // Inference CNN model and display request
                        val status = runModel(context = context, bitmap = bmp)
                        onResultCNN(status)
                    }
                }
            }

            MessageInputField(
                value = input,
                onValueChange = { input = it },
                sendBtnOnClick = {
                    scope.launch {
                        sendBtnOnClick()
                    }
                },
                micBtnOnClick = ::micBtnOnClick
            )
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

        if (showLoginScreen) {
            // Use your navigation logic here, or show LoginScreen directly if using Compose navigation
            onNavigate()
            showLoginScreen = false
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

