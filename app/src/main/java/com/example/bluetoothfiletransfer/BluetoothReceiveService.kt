package com.example.bluetoothfiletransfer

import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*

class BluetoothReceiveService : Service() {

    companion object {
        private const val TAG = "BluetoothReceiveService"
        private const val SERVICE_NAME = "BluetoothFileTransfer"
        private val APP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Common SPP UUID
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Thread {
            try {
                val serverSocket = setupServerSocket()
                if (serverSocket != null) {
                    Log.d(TAG, "Waiting for incoming connections...")
                    val socket = serverSocket.accept() // Blocking call until a connection is made
                    handleFileReceive(socket)
                    serverSocket.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in receiving file: ${e.message}", e)
                Toast.makeText(this, "Error in receiving file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            stopSelf()
        }.start()
        return START_NOT_STICKY
    }

    private fun setupServerSocket(): BluetoothServerSocket? {
        return try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                Log.e(TAG, "Bluetooth is not available or not enabled")
                Toast.makeText(this, "Bluetooth is not available or enabled", Toast.LENGTH_SHORT).show()
                null
            } else {
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
//                    return
                }
                bluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, APP_UUID)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up server socket: ${e.message}", e)
            null
        }
    }

    private fun handleFileReceive(socket: BluetoothSocket) {
        try {
            val inputStream: InputStream = socket.inputStream
            val receivedFile = File(filesDir, "received_file.txt") // Save file in app's internal storage
            val outputStream = FileOutputStream(receivedFile)

            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            outputStream.close()
            inputStream.close()
            socket.close()

            Log.d(TAG, "File received successfully: ${receivedFile.absolutePath}")
            Toast.makeText(this, "File received successfully: ${receivedFile.absolutePath}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error receiving file: ${e.message}", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
