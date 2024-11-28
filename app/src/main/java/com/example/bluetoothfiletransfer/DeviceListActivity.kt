package com.example.bluetoothfiletransfer

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File

class DeviceListActivity : AppCompatActivity() {

    private var selectedDeviceAddress: String? = null
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val filePath = getFilePath(uri)
            if (filePath != null && selectedDeviceAddress != null) {
                startFileTransfer(selectedDeviceAddress!!, filePath)
            } else {
                Toast.makeText(this, "Invalid file or device address", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        emptyArray()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            displayBondedDevices()
        } else {
            Toast.makeText(this, "Permissions denied. Cannot list devices.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)
        checkAndRequestPermissions()
//        val deviceList: List<BluetoothDevice>? = BluetoothAdapter.getDefaultAdapter()?.bondedDevices?.toList()
//        val deviceNames = deviceList?.map { "${it.name}\n${it.address}" } ?: emptyList()
//
//        val listView: ListView = findViewById(R.id.device_list)
//        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceNames)
//        listView.adapter = adapter
//
//        listView.setOnItemClickListener { _, _, position, _ ->
//            selectedDeviceAddress = deviceList?.get(position)?.address
//            openFilePicker()
//        }
    }
    private fun checkAndRequestPermissions() {
        if (!arePermissionsGranted()) {
            permissionLauncher.launch(requiredPermissions)
        } else {
            displayBondedDevices()
        }
    }

    private fun arePermissionsGranted(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun displayBondedDevices() {
        val listView: ListView = findViewById(R.id.device_list)

        try {
            val deviceList: List<BluetoothDevice>? = bluetoothAdapter?.bondedDevices?.toList()
            val deviceNames = deviceList?.map { "${it.name}\n${it.address}" } ?: emptyList()

            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceNames)
            listView.adapter = adapter

            listView.setOnItemClickListener { _, _, position, _ ->
//                val device = deviceList?.get(position)
//                if (device != null) {
//                    val intent = Intent(this, FileTransferService::class.java).apply {
//                        putExtra("DEVICE_ADDRESS", device.address)
//                    }
//                    startService(intent)
//                    Toast.makeText(this, "Connecting to ${device.name}", Toast.LENGTH_SHORT).show()
//                }
                selectedDeviceAddress = deviceList?.get(position)?.address
                openFilePicker()
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Permission denied to access bonded devices.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun openFilePicker() {
        filePickerLauncher.launch("*/*") // Allow all file types
    }

    private fun getFilePath(uri: Uri): String? {
        var filePath: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            val fileName = cursor.getString(nameIndex)
            filePath = File(cacheDir, fileName).absolutePath

            // Save the file to the cache directory
            contentResolver.openInputStream(uri)?.use { inputStream ->
                File(filePath!!).outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        return filePath
    }

    private fun startFileTransfer(deviceAddress: String, filePath: String) {
        val intent = Intent(this, FileTransferService::class.java).apply {
            putExtra("DEVICE_ADDRESS", deviceAddress)
            putExtra("FILE_PATH", filePath)
        }
        startService(intent)
    }
}
