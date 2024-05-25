package com.example.bleapps

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class MainActivity : Activity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bleDeviceAdapter: BLEDeviceAdapter
    private var bluetoothGatt: BluetoothGatt? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        bleDeviceAdapter = BLEDeviceAdapter { device ->
            connectToDevice(device)
        }
        recyclerView.adapter = bleDeviceAdapter

        val searchBar: EditText = findViewById(R.id.search_bar)
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                bleDeviceAdapter.filter(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        checkPermissions()
    }

    private fun checkPermissions() {
        val requiredPermissions = arrayOf(
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT
        )

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 1)
        } else {
            startBLEScan()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBLEScan() {
        bluetoothAdapter.bluetoothLeScanner.startScan(bleScanCallback)
    }

    private val bleScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            runOnUiThread {
                if (result.device.name != null) {
                    synchronized(this@MainActivity) {
                        bleDeviceAdapter.addDevice(result.device, result.rssi)
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.forEach {
                runOnUiThread {
                    if (it.device.name != null) {
                        synchronized(this@MainActivity) {
                            bleDeviceAdapter.addDevice(it.device, it.rssi)
                        }
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Scan failed: $errorCode", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            val device = gatt?.device ?: return
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                runOnUiThread {
                    bleDeviceAdapter.updateDeviceConnectionState(device, true)
                    Toast.makeText(
                        this@MainActivity,
                        "Connected to ${device.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                gatt.discoverServices()
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                runOnUiThread {
                    bleDeviceAdapter.updateDeviceConnectionState(device, false)
                    Toast.makeText(
                        this@MainActivity,
                        "Disconnected from ${device.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt?.services?.forEach { service ->
                    // Handle discovered services here
                    // For example, you can log them or interact with the characteristics
                    logServiceAndCharacteristics(service)
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                characteristic?.let {
                    val value = it.value
                    // Handle the characteristic value here
                }
            }
        }

        // Additional overrides for writing characteristics, notifications, etc.
    }

    private fun logServiceAndCharacteristics(service: BluetoothGattService) {
        val serviceUuid = service.uuid
        val characteristics = service.characteristics
        characteristics.forEach { characteristic ->
            val characteristicUuid = characteristic.uuid
            // Log or handle the characteristic UUID
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startBLEScan()
        } else {
            Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
        }
    }
}