package nl.remyparzinski.heartrateassistant

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
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
    private val HEART_RATE_SERVICE_UUID = ParcelUuid(UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb"))

    fun startScan(
        scanPeriodMs: Long = 10000,
        onDeviceFound: (deviceName: String?, deviceAddress: String?) -> Unit,
        onScanFailed: (errorCode: Int) -> Unit
    ) {
        if (isScanning || bluetoothLeScanner == null) {
            return
        }

        val filter = ScanFilter.Builder()
            .setServiceUuid(HEART_RATE_SERVICE_UUID)
            .build()
        val filters = listOf(filter)

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result?.device?.let { device ->
                    val name = device.name ?: "Unknown Heart Rate Device"
                    val address = device.address
                    onDeviceFound(name, address)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                isScanning = false
                onScanFailed(errorCode)
            }
        }

        handler.postDelayed({
            if (isScanning) {
                stopScan(scanCallback)
            }
        }, scanPeriodMs)

        isScanning = true
        bluetoothLeScanner?.startScan(filters, settings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScan(callback: ScanCallback) {
        if (!isScanning) {
            return
        }

        isScanning = false
        bluetoothLeScanner?.stopScan(callback)
    }
}
















