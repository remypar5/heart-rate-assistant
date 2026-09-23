package nl.remyparzinski.heartrateassistant

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.util.UUID

class HeartRateService : Service() {
    private val binder = LocalBinder()
    private var bluetoothGatt: BluetoothGatt? = null
    var currentHeartRate: Int = 0
        private set // Readonly outside of this class

    inner class LocalBinder : Binder() {
        fun getService(): HeartRateService = this@HeartRateService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val deviceAddress = intent?.getStringExtra(EXTRA_DEVICE_ADDRESS)

        startForegroundServiceNotification()

        if (!deviceAddress.isNullOrEmpty()) {
            connectToDevice(deviceAddress)
        }

        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(address: String) {
        val bluetoothAdapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        val device = bluetoothAdapter.getRemoteDevice(address)

        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                TODO("Implement retry logic")
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(HEART_RATE_SERVICE_UUID)
                val characteristic = service?.getCharacteristic(HEART_RATE_MEASUREMENT_CHAR_UUID)

                if (characteristic != null) {
                    // 1. Enable Android OS Notifications
                    gatt.setCharacteristicNotification(characteristic, true)

                    // 2. Write Client Characteristic Configuration Descriptor (CCCD)
                    val descriptor = characteristic.getDescriptor(CCCD_UUID)
                    if (descriptor != null) {
                        gatt.writeDescriptor(
                            descriptor,
                            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        )
                    }
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == HEART_RATE_MEASUREMENT_CHAR_UUID) {
                parseHeartRate(value)
            }
        }
    }

    private fun parseHeartRate(data: ByteArray) {
        if (data.isEmpty()) {
            return
        }

        val flags = data[0].toInt()
        val is16Bit = (flags and 0x01) != 0

        currentHeartRate = if (is16Bit) {
            ((data[2].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
        } else {
            data[1].toInt() and 0xFF
        }

        updateNotification("Heart rate: $currentHeartRate BPM")
    }

    private fun startForegroundServiceNotification() {
        val channelId = "HR_CHANNEL"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Heart rate monitor",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)

        val notification = createNotification("Connecting to sensor...")

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun updateNotification(text: String) {
        val notification = createNotification(text)
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, "HR_CHANNEL")
            .setContentTitle("Heart rate monitor active")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
    }

    companion object {
        const val EXTRA_DEVICE_ADDRESS = "device_address"
        private const val NOTIFICATION_ID = 1001

        private val HEART_RATE_SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        private val HEART_RATE_MEASUREMENT_CHAR_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    }
}






















