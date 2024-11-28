package com.example.bluetoothfiletransfer

import FileTransferService
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class DeviceListActivity : AppCompatActivity() {

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
                val device = deviceList?.get(position)
                if (device != null) {
                    val intent = Intent(this, FileTransferService::class.java).apply {
                        putExtra("DEVICE_ADDRESS", device.address)
                    }
                    startService(intent)
                    Toast.makeText(this, "Connecting to ${device.name}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Permission denied to access bonded devices.", Toast.LENGTH_SHORT).show()
        }
    }
}
