package com.example.bluetoothfiletransfer
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.util.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.Context

class FileTransferService : Service() {

    companion object {
        private const val TAG = "FileTransferService"
        private val APP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP UUID
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val deviceAddress = intent?.getStringExtra("DEVICE_ADDRESS")
        val filePath = intent?.getStringExtra("FILE_PATH")

        if (deviceAddress.isNullOrEmpty() || filePath.isNullOrEmpty()) {
            Log.e(TAG, "Invalid device address or file path")
            Toast.makeText(this, "Invalid device address or file path", Toast.LENGTH_SHORT).show()
            stopSelf()
            return START_NOT_STICKY
        }

        val device: BluetoothDevice? = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress)
        if (device == null) {
            Log.e(TAG, "Device not found")
            Toast.makeText(this, "Device not found", Toast.LENGTH_SHORT).show()
            stopSelf()
            return START_NOT_STICKY
        }

        Thread {
            try {
                val socket = connectToDevice(device, applicationContext)
                if (socket != null) {
                    sendFile(socket, File(filePath))
                    Log.i(TAG, "File sent successfully")
                    Toast.makeText(this, "File sent successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e(TAG, "Socket is null. Could not connect to the device.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in file transfer: ${e.message}", e)
                Toast.makeText(this, "Error in file transfer: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            stopSelf()
        }.start()

        return START_NOT_STICKY
    }

//    private fun connectToDevice(device: BluetoothDevice): BluetoothSocket? {
//        try {
//            Log.d(TAG, "Connecting to device: ${device.name} (${device.address})")
//            val socket = device.createRfcommSocketToServiceRecord(APP_UUID)
//            socket.connect()
//            Log.d(TAG, "Connected to device: ${device.name}")
//            return socket
//        } catch (e: Exception) {
//            Log.e(TAG, "Failed to connect to device: ${e.message}", e)
//            return null
//        }
//    }
private fun connectToDevice(device: BluetoothDevice, context: Context): BluetoothSocket? {
    // Check Bluetooth Connect permission for Android 12+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Bluetooth Connect permission not granted")
            // Optionally, request permission here or handle the lack of permission
            return null
        }
    }

    try {
        Log.d(TAG, "Connecting to device: ${device.name} (${device.address})")

        // Check Bluetooth Admin permission
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_ADMIN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Bluetooth Admin permission not granted")
            return null
        }

        val socket = device.createRfcommSocketToServiceRecord(APP_UUID)
        socket.connect()
        Log.d(TAG, "Connected to device: ${device.name}")
        return socket
    } catch (e: SecurityException) {
        Log.e(TAG, "Security exception when connecting to device: ${e.message}", e)
        return null
    } catch (e: Exception) {
        Log.e(TAG, "Failed to connect to device: ${e.message}", e)
        return null
    }
}
    private fun sendFile(socket: BluetoothSocket, file: File) {
        if (!file.exists()) {
            Log.e(TAG, "File not found: ${file.absolutePath}")
            throw Exception("File not found: ${file.absolutePath}")
        }

        val outputStream: OutputStream = socket.outputStream
        val inputStream = FileInputStream(file)

        try {
            Log.d(TAG, "Starting file transfer...")
            val buffer = ByteArray(1024)
            var bytes: Int
            while (inputStream.read(buffer).also { bytes = it } != -1) {
                outputStream.write(buffer, 0, bytes)
            }
            outputStream.flush()
            Log.d(TAG, "File transfer completed.")
        } catch (e: Exception) {
            Log.e(TAG, "Error during file transfer: ${e.message}", e)
            throw e
        } finally {
            inputStream.close()
            outputStream.close()
            socket.close()
            Log.d(TAG, "Resources closed after file transfer.")
        }
    }

    override fun onBind(intent: Intent?) = null
}
