import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import java.util.UUID

class BleHeartRateScanner(context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private val bluetoothLeScanner: BluetoothLeScanner?
        get() = bluetoothAdapter?.bluetoothLeScanner

    private val handler = Handler(Looper.getMainLooper())
    private var isScanning = false

    // Officiële Bluetooth SIG Heart Rate Service UUID
    private val HEART_RATE_SERVICE_UUID = ParcelUuid(UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb"))

    @SuppressLint("MissingPermission")
    fun startScan(
        scanPeriodMs: Long = 10000,
        onDeviceFound: (deviceName: String?, deviceAddress: String) -> Unit,
        onScanFailed: (errorCode: Int) -> Unit
    ) {
        if (isScanning || bluetoothLeScanner == null) return

        // 1. Filter instellen: Zoek ALLEEN naar apparaten met de Heart Rate Service
        val filter = ScanFilter.Builder()
            .setServiceUuid(HEART_RATE_SERVICE_UUID)
            .build()
        val filters = listOf(filter)

        // 2. Settings: Low Latency zorgt voor snelle detectie (ideaal voor koppelen)
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        // 3. Callback verwerken
        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.device?.let { device ->
                    val name = device.name ?: "Onbekende Hartslagband"
                    val address = device.address // Dit is het MAC-adres
                    onDeviceFound(name, address)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                isScanning = false
                onScanFailed(errorCode)
            }
        }

        // 4. Automatische Stop na X seconden (voorkomt lege batterij)
        handler.postDelayed({
            if (isScanning) {
                stopScan(scanCallback)
            }
        }, scanPeriodMs)

        // 5. Start de scan
        isScanning = true
        bluetoothLeScanner?.startScan(filters, settings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScan(callback: ScanCallback) {
        if (!isScanning) return
        isScanning = false
        bluetoothLeScanner?.stopScan(callback)
    }
}