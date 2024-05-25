package com.example.bleapps

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale


class BLEDeviceAdapter(private val clickListener: (BluetoothDevice) -> Unit) :
    RecyclerView.Adapter<BLEDeviceAdapter.BLEDeviceViewHolder>() {

    data class BLEDevice(
        val device: BluetoothDevice,
        var rssi: Int,
        var connected: Boolean = false
    )

    private val devices = mutableListOf<BLEDevice>()
    private val filteredDevices = mutableListOf<BLEDevice>()
    private var currentQuery: String = ""

    @Synchronized
    fun addDevice(device: BluetoothDevice, rssi: Int) {
        val bleDevice = devices.find { it.device.address == device.address }
        if (bleDevice != null) {
            bleDevice.rssi = rssi
        } else {
            devices.add(BLEDevice(device, rssi))
        }
        filter(currentQuery)
    }

    @Synchronized
    fun updateDeviceConnectionState(device: BluetoothDevice, connected: Boolean) {
        devices.find { it.device.address == device.address }?.connected = connected
        filter(currentQuery)
    }

    @SuppressLint("MissingPermission")
    @Synchronized
    fun filter(query: String) {
        currentQuery = query
        filteredDevices.clear()
        if (query.isEmpty()) {
            filteredDevices.addAll(devices)
        } else {
            val lowercaseQuery = query.lowercase(Locale.getDefault())
            filteredDevices.addAll(devices.filter {
                (it.device.name?.lowercase(Locale.getDefault())?.contains(lowercaseQuery)
                    ?: false) ||
                        it.device.address.lowercase(Locale.getDefault()).contains(lowercaseQuery)
            })
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BLEDeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.device_item, parent, false)
        return BLEDeviceViewHolder(view)
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: BLEDeviceViewHolder, position: Int) {
        val bleDevice = filteredDevices[position]
        holder.deviceName.text = bleDevice.device.name ?: "Unknown Device"
        holder.deviceAddress.text = bleDevice.device.address
        holder.deviceRssi.text = "RSSI: ${bleDevice.rssi}"
        holder.deviceState.text = if (bleDevice.connected) "Connected" else "Disconnected"
        holder.itemView.setOnClickListener {
            clickListener(bleDevice.device)
        }
    }

    override fun getItemCount(): Int {
        return filteredDevices.size
    }

    class BLEDeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceName: TextView = itemView.findViewById(R.id.device_name)
        val deviceAddress: TextView = itemView.findViewById(R.id.device_address)
        val deviceRssi: TextView = itemView.findViewById(R.id.device_rssi)
        val deviceState: TextView = itemView.findViewById(R.id.device_state)
    }
}