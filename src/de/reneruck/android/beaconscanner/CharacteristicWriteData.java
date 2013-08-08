package de.reneruck.android.beaconscanner;

import java.math.BigInteger;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;

public class CharacteristicWriteData {

	private BluetoothDevice device;
	private UUID service;
	private UUID characteristic;
	private byte[] data;
	public CharacteristicWriteData(BluetoothDevice device, UUID service,
			UUID characteristic, byte[] data) {
		this.device = device;
		this.service = service;
		this.characteristic = characteristic;
		this.data = data;
	}
	
	public BluetoothDevice getDevice() {
		return device;
	}
	public void setDevice(BluetoothDevice device) {
		this.device = device;
	}
	public UUID getService() {
		return service;
	}
	public void setService(UUID service) {
		this.service = service;
	}
	public UUID getCharacteristic() {
		return characteristic;
	}
	public void setCharacteristic(UUID characteristic) {
		this.characteristic = characteristic;
	}
	public byte[] getData() {
		return data;
	}
	public void setData(byte[] data) {
		this.data = data;
	}
}
