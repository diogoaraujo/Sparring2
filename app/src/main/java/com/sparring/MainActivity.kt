package com.sparring

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sparring.R
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class MainActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothDevice: BluetoothDevice? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var connectedThread: ConnectedThread? = null

    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var messageTextView: TextView

    private val handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_READ -> {
                    val readMessage = msg.obj as String
                    messageTextView.text = readMessage
                }
            }
        }
    }

    companion object {
        const val MESSAGE_READ = 1
        const val PERMISSION_REQUEST_BLUETOOTH = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectButton = findViewById(R.id.connectButton)
        disconnectButton = findViewById(R.id.disconnectButton)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
        messageTextView = findViewById(R.id.messageTextView)

        connectButton.setOnClickListener {
            connectToDevice()
        }

        disconnectButton.setOnClickListener {
            disconnectDevice()
        }

        sendButton.setOnClickListener {
            sendMessage()
        }

        // Verificar e solicitar permissões Bluetooth em tempo de execução
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkBluetoothPermissions()
        }
    }

    private fun connectToDevice() {
        // Substitua "SeuDispositivoBluetooth" pelo nome do seu dispositivo Bluetooth
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        bluetoothDevice = bluetoothAdapter?.bondedDevices?.find { it.name == "SeuDispositivoBluetooth" }

        if (bluetoothDevice != null) {
            connectToDevice(bluetoothDevice!!)
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (checkBluetoothPermissions()) {
            ConnectThread(this, device).start()
        }
    }

    private fun disconnectDevice() {
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket?.close()
                messageTextView.text = "Desconectado"
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun sendMessage() {
        val message = messageEditText.text.toString()
        if (connectedThread != null) {
            connectedThread?.write(message.toByteArray())
        }
    }

    private fun checkBluetoothPermissions(): Boolean {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_BLUETOOTH)
            return false
        }

        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_BLUETOOTH -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    // Permissões concedidas, conecte-se ao dispositivo Bluetooth
                    connectToDevice()
                } else {
                    // Permissões negadas, você pode lidar com isso de acordo com sua lógica de aplicativo
                    Toast.makeText(this, "Permissões necessárias não concedidas", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private inner class ConnectThread(private val activity: MainActivity, private val device: BluetoothDevice) : Thread() {
        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            try {
                // Verificar permissões antes de criar o socket
                if (checkBluetoothPermissions()) {
                    device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                } else {
                    null
                }
            } catch (e: IOException) {
                null
            } catch (e: SecurityException) {
                null
            }
        }

        public override fun run() {
            // Verificar permissões antes de cancelar a descoberta
            if (checkBluetoothPermissions()) {
                if (ActivityCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                bluetoothAdapter?.cancelDiscovery()
            } else {
                // Lidar com o caso em que as permissões não foram concedidas
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Permissões Bluetooth não concedidas", Toast.LENGTH_SHORT).show()
                }
                return
            }

            try {
                if (mmSocket != null) {
                    mmSocket!!.connect()
                    connectedThread = ConnectedThread(mmSocket!!)
                    connectedThread?.start()
                } else {
                    // Lidar com o caso em que o socket é nulo (falha na criação ou permissões)
                    // Pode lançar uma exceção ou mostrar uma mensagem de erro
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Falha ao criar o socket Bluetooth ou permissões não concedidas", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException) {
                try {
                    mmSocket?.close()
                } catch (closeException: IOException) {
                    closeException.printStackTrace()
                }
                e.printStackTrace()
            }
        }

        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        private fun checkBluetoothPermissions(): Boolean {
            val permissionsToRequest = mutableListOf<String>()

            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH)
            }

            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADMIN)
            }

            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }

            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(this@MainActivity, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_BLUETOOTH)
                return false
            }

            return true
        }
    }


    private inner class ConnectedThread(socket: BluetoothSocket) : Thread() {
        private val mmInStream: InputStream = socket.inputStream
        private val mmOutStream: OutputStream = socket.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024)

        public override fun run() {
            var numBytes: Int

            while (true) {
                try {
                    numBytes = mmInStream.read(mmBuffer)
                    val readMessage = String(mmBuffer, 0, numBytes)
                    val message = obtainMessage(MESSAGE_READ, numBytes, -1, readMessage)
                    message.sendToTarget()
                } catch (e: IOException) {
                    break
                }
            }
        }

        fun write(bytes: ByteArray) {
            try {
                mmOutStream.write(bytes)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        fun cancel() {
            try {
                mmInStream.close()
                mmOutStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


    private fun obtainMessage(what: Int, arg1: Int, arg2: Int, obj: Any?): Message {
        val message = handler.obtainMessage(what, arg1, arg2, obj)
        return message
    }
}