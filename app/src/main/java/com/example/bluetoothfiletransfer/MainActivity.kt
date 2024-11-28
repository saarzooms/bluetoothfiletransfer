package com.example.bluetoothfiletransfer
import FileTransferService
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.bluetoothfiletransfer.DeviceListActivity
import com.example.bluetoothfiletransfer.R

public class MainActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

    private val requiredPermissions = mutableListOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
        }
    }.toTypedArray()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permissions denied. App cannot function properly.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnEnableBluetooth: Button = findViewById(R.id.btn_enable_bluetooth)
        val btnSendFile: Button = findViewById(R.id.btn_send_file)
        val btnReceiveFile: Button = findViewById(R.id.btn_receive_file)

        // Check and request permissions on app start
        checkAndRequestPermissions()

        btnEnableBluetooth.setOnClickListener {
            enableBluetooth()
        }

        btnSendFile.setOnClickListener {
            if (arePermissionsGranted()) {
                val intent = Intent(this, DeviceListActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Permissions not granted!", Toast.LENGTH_SHORT).show()
            }
        }

        btnReceiveFile.setOnClickListener {
            if (arePermissionsGranted()) {
                val serviceIntent = Intent(this, FileTransferService::class.java)
                startService(serviceIntent)
            } else {
                Toast.makeText(this, "Permissions not granted!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun enableBluetooth() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
        } else if (!bluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
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
            startActivityForResult(enableBtIntent, 1)
        } else {
            Toast.makeText(this, "Bluetooth is already enabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun arePermissionsGranted(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkAndRequestPermissions() {
        if (!arePermissionsGranted()) {
            permissionLauncher.launch(requiredPermissions)
        }
    }
}
