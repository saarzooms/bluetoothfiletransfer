import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.util.*

class FileTransferService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val deviceAddress = intent?.getStringExtra("DEVICE_ADDRESS")
        val device: BluetoothDevice? = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress)

        if (device != null) {
            if (hasBluetoothConnectPermission()) {
                try {
                    val socket = device.createRfcommSocketToServiceRecord(UUID.randomUUID())
                    socket.connect()
                    sendFile(socket, File("/sdcard/Download/sample.txt"))
                } catch (e: SecurityException) {
                    Toast.makeText(this, "Permission error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "BLUETOOTH_CONNECT permission is missing.", Toast.LENGTH_SHORT).show()
            }
        }
        return START_NOT_STICKY
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // No runtime permission required for versions below Android 12
        }
    }

    private fun sendFile(socket: BluetoothSocket, file: File) {
        val outputStream: OutputStream = socket.outputStream
        val inputStream = FileInputStream(file)

        val buffer = ByteArray(1024)
        var bytes: Int
        while (inputStream.read(buffer).also { bytes = it } != -1) {
            outputStream.write(buffer, 0, bytes)
        }

        inputStream.close()
        outputStream.close()
        socket.close()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
