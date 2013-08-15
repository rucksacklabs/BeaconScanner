/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.reneruck.android.beaconscanner;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.samsung.android.sdk.bt.gatt.BluetoothGatt;
import com.samsung.android.sdk.bt.gatt.BluetoothGattAdapter;
import com.samsung.android.sdk.bt.gatt.BluetoothGattCallback;
import com.samsung.android.sdk.bt.gatt.BluetoothGattCharacteristic;
import com.samsung.android.sdk.bt.gatt.BluetoothGattDescriptor;
import com.samsung.android.sdk.bt.gatt.BluetoothGattService;

public class HRPService extends Service {

	public static final UUID HRP_SERVICE = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
	public static final UUID DEVICE_INFORMATION = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb");
	public static final UUID HEART_RATE_MEASUREMENT_CHARAC = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");
	
	public static final UUID CONNECTION_CONTROL_SERVICE = UUID.fromString("00001111-0000-1000-8000-00805f9b34fb");
	public static final UUID CONNECTION_CONTROL_CI_CHARAC = UUID.fromString("00000029-0000-1000-8000-00805f9b34fb");
	public static final UUID CONNECTION_CONTROL_SL_CHARAC = UUID.fromString("00001113-0000-1000-8000-00805f9b34fb");
	public static final UUID CONNECTION_CONTROL_SR_CHARAC = UUID.fromString("00001114-0000-1000-8000-00805f9b34fb");
	public static final UUID CONNECTION_CONTROL_BL_CHARAC = UUID.fromString("00001115-0000-1000-8000-00805f9b34fb");
	
	public static final UUID CCC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
	public static final UUID BODY_SENSOR_LOCATION = UUID.fromString("00002A38-0000-1000-8000-00805f9b34fb");
	public static final UUID SERIAL_NUMBER_STRING = UUID.fromString("00002A25-0000-1000-8000-00805f9b34fb");
	public static final UUID MANUFATURE_NAME_STRING = UUID.fromString("00002A29-0000-1000-8000-00805f9b34fb");
	public static final UUID ICDL = UUID.fromString("00002A2A-0000-1000-8000-00805f9b34fb");
	public static final UUID HeartRate_ControlPoint = UUID.fromString("00002A39-0000-1000-8000-00805f9b34fb");
	public static final UUID DIS_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
	public static final UUID FIRMWARE_REVISON_UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");

	static final String TAG = "HRPService";
	public static final int HRP_CONNECT_MSG = 1;
	public static final int HRP_DISCONNECT_MSG = 2;
	public static final int HRP_READY_MSG = 3;
	public static final int HRP_VALUE_MSG = 4;
	public static final int SERVICES_DISCOVERED = 5;
	public static final int GATT_DEVICE_FOUND_MSG = 5;

	/** Source of device entries in the device list */
	public static final int DEVICE_SOURCE_SCAN = 0;
	public static final int DEVICE_SOURCE_BONDED = 1;
	public static final int DEVICE_SOURCE_CONNECTED = 2;
	public static final int RESET_ENERGY_EXPANDED = 1;
	/** Intent extras */
	public static final String EXTRA_DEVICE = "DEVICE";
	public static final String EXTRA_RSSI = "RSSI";
	public static final String EXTRA_SOURCE = "SOURCE";
	public static final String EXTRA_ADDR = "ADDRESS";
	public static final String EXTRA_CONNECTED = "CONNECTED";
	public static final String EXTRA_STATUS = "STATUS";
	public static final String EXTRA_UUID = "UUID";
	public static final String EXTRA_VALUE = "VALUE";
	public static final String EXTRA_SCAN_RECORD = "RECORD";
	public static final String BSL_VALUE = "com.siso.ble.hrpservice.bslval";
	public static final String HRM_VALUE = "com.siso.ble.hrpservice.hrmval";
	public static final String SERIAL_STRING = "com.siso.ble.hrpservice.serialstring";
	public static final String MANF_NAME = "com.siso.ble.hrpservice.manfname";
	public static final String ICDL_VALUE = "com.siso.ble.hrpservice.icdl";
	public static final String HRM_EEVALUE = "com.siso.ble.hrpservice.eeval";
	public static final String HRM_RRVALUE = "com.siso.ble.hrpservice.rrval";

	public static final int ADV_DATA_FLAG = 0x01;
	public static final int LIMITED_AND_GENERAL_DISC_MASK = 0x03;
	public static final int FIRST_BITMASK = 0x01;
	public static final int SECOND_BITMASK = FIRST_BITMASK << 1;
	public static final int THIRD_BITMASK = FIRST_BITMASK << 2;
	public static final int FOURTH_BITMASK = FIRST_BITMASK << 3;
	public static final int FIFTH_BITMASK = FIRST_BITMASK << 4;
	public static final int SIXTH_BITMASK = FIRST_BITMASK << 5;
	public static final int SEVENTH_BITMASK = FIRST_BITMASK << 6;
	public static final int EIGTH_BITMASK = FIRST_BITMASK << 7;

	public static boolean sendDone = true;
	
	private BluetoothAdapter mBtAdapter = null;
	public BluetoothGatt mBluetoothGatt = null;
	private Handler mActivityHandler = null;
	private Handler mDeviceListHandler = null;
	public boolean isNoti = false;

	/**
	 * Profile service connection listener
	 */
	public class LocalBinder extends Binder {
		HRPService getService() {
			return HRPService.this;
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return binder;
	}

	private final IBinder binder = new LocalBinder();

	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate() called");
		if (mBtAdapter == null) {
			mBtAdapter = BluetoothAdapter.getDefaultAdapter();
			if (mBtAdapter == null)
				return;
		}

		if (mBluetoothGatt == null) {
			BluetoothGattAdapter.getProfileProxy(this, mProfileServiceListener, BluetoothGattAdapter.GATT);
		}
	}

	public void setActivityHandler(Handler mHandler) {
		Log.d(TAG, "Activity Handler set");
		mActivityHandler = mHandler;
	}

	public void setDeviceListHandler(Handler mHandler) {
		Log.d(TAG, "Device List Handler set");
		mDeviceListHandler = mHandler;
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy() called");
		if (mBtAdapter != null && mBluetoothGatt != null) {
			BluetoothGattAdapter.closeProfileProxy(BluetoothGattAdapter.GATT, mBluetoothGatt);
		}
		super.onDestroy();
	}

	private BluetoothProfile.ServiceListener mProfileServiceListener = new BluetoothProfile.ServiceListener() {
		@SuppressLint("NewApi")
		@Override
		public void onServiceConnected(int profile, BluetoothProfile proxy) {
			if (profile == BluetoothGattAdapter.GATT) {
				mBluetoothGatt = (BluetoothGatt) proxy;
				mBluetoothGatt.registerApp(mGattCallbacks);
			}
		}

		@Override
		public void onServiceDisconnected(int profile) {
			if (profile == BluetoothGattAdapter.GATT) {
				if (mBluetoothGatt != null)
					mBluetoothGatt.unregisterApp();

				mBluetoothGatt = null;
			}
		}
	};

	
	/**
	 * GATT client callbacks
	 */
	private BluetoothGattCallback mGattCallbacks = new BluetoothGattCallback() {

		@Override
		public void onCharacteristicWrite(BluetoothGattCharacteristic paramBluetoothGattCharacteristic, int paramInt) {
			Log.d(TAG, "OnCharacteristicWrite UUDI: " + paramBluetoothGattCharacteristic.getUuid() + " Result: " + resultCodeToString(paramInt));
			HRPService.sendDone = true;
		}

		private String resultCodeToString(int paramInt) {
			switch (paramInt) {
				case BluetoothGatt.GATT_SUCCESS:
					return "SUCCESS";
				case BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH:
					return "GATT_INVALID_ATTRIBUTE_LENGTH";
				case BluetoothGatt.GATT_WRITE_NOT_PERMITTED:
					return "GATT_WRITE_NOT_PERMITTED";
				case BluetoothGatt.GATT_ALREADY_OPEN:
					return "GATT_ALREADY_OPEN";
				case BluetoothGatt.GATT_ERROR:
					return "GATT_ERROR";
				case BluetoothGatt.GATT_INVALID_OFFSET:
					return "GATT_INVALID_OFFSET";
				case BluetoothGatt.GATT_INTERNAL_ERROR:
					return "GATT_INTERNAL_ERROR";
			default:
				return "UNKNOWN";
			}
		}

		@Override
		public void onScanResult(BluetoothDevice device, int rssi, byte[] scanRecord) {
			// Log.d(TAG, "onScanResult() - device=" + device + ", rssi=" +
			// rssi);
			// analyseScanResult(scanRecord);
			boolean checkIfBroadcastMode = checkIfBroadcastMode(scanRecord);
			if (!checkIfBroadcastMode) {
				Bundle mBundle = new Bundle();
				Message msg = Message.obtain(mDeviceListHandler, GATT_DEVICE_FOUND_MSG);
				mBundle.putParcelable(BluetoothDevice.EXTRA_DEVICE, device);
				mBundle.putInt(EXTRA_RSSI, rssi);
				mBundle.putString(EXTRA_UUID, getUUID(scanRecord));
				mBundle.putInt(EXTRA_SOURCE, DEVICE_SOURCE_SCAN);
				msg.setData(mBundle);
				msg.sendToTarget();
			} else {
				Log.i(TAG, "device =" + device + " is in Brodacast mode, hence not displaying");
			}
		}

		private String getUUID(byte[] scanRecord) {
			byte[] payload_uuid = Arrays.copyOfRange(scanRecord, 2, 18);
			return new BigInteger(1, payload_uuid).toString(16);
		}

		private void analyseScanResult(byte[] scanRecord) {
			byte[] payload_uuid = Arrays.copyOfRange(scanRecord, 2, 18);
			byte[] payload_rest = Arrays.copyOfRange(scanRecord, 19, scanRecord.length);

			String binary = toBinary(scanRecord);

			String scanrRecord_hex_string = new BigInteger(1, scanRecord).toString(16);
			String uuid_hex_string = new BigInteger(1, payload_uuid).toString(16);

			String content = new String(payload_rest);
			UUID nameUUIDFromBytes = UUID.nameUUIDFromBytes(payload_uuid);
			UUID UUIDFromBytes = UUID.nameUUIDFromBytes(payload_uuid);
			Log.d(TAG, "scan record hex: " + scanrRecord_hex_string);
			Log.d(TAG, "UUID: " + nameUUIDFromBytes.toString());
			// Log.d(TAG, "Content: " + content);
			Log.d(TAG, "UUID Hex: " + uuid_hex_string);
			Log.d(TAG, "-----------------------------------------------------------");

		}

		String toBinary(byte[] bytes) {
			StringBuilder sb = new StringBuilder(bytes.length * Byte.SIZE);
			for (int i = 0; i < Byte.SIZE * bytes.length; i++)
				sb.append((bytes[i / Byte.SIZE] << i % Byte.SIZE & 0x80) == 0 ? '0' : '1');
			return sb.toString();
		}

		@Override
		public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
			Log.d(TAG, "onConnectionStateChange (" + device.getAddress() + ")");
			if (newState == BluetoothProfile.STATE_CONNECTED && mBluetoothGatt != null) {
				Bundle mBundle = new Bundle();
				Message msg = Message.obtain(mActivityHandler, HRP_CONNECT_MSG);
				mBundle.putString(BluetoothDevice.EXTRA_DEVICE, device.getAddress());
				msg.setData(mBundle);
				msg.sendToTarget();
				mBluetoothGatt.discoverServices(device);
			}
			if (newState == BluetoothProfile.STATE_DISCONNECTED && mBluetoothGatt != null) {
				Message msg = Message.obtain(mActivityHandler, HRP_DISCONNECT_MSG);
				msg.sendToTarget();
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothDevice device, int status) {
			Message msg = Message.obtain(mActivityHandler, SERVICES_DISCOVERED);
			List services = mBluetoothGatt.getServices(device);
			msg.obj = services;
			msg.sendToTarget();
			subscribeToHeartbeat(device);
//			DummyReadForSecLevelCheck(device);
		}

	

		public void onCharacteristicChanged(BluetoothGattCharacteristic characteristic) {
			Log.i(TAG, "onCharacteristicChanged");
			Bundle mBundle = new Bundle();
			Message msg = Message.obtain(mActivityHandler, HRP_VALUE_MSG);
			
			int length = characteristic.getValue().length;
			
			int[] hrmValues = new int[characteristic.getValue().length -2];
			if(isContentArray(characteristic.getValue()[0])) {

				for (int i = 1; i < characteristic.getValue().length-1; i++) {
					hrmValues[i - 1] = characteristic.getValue()[i];
				}
				
				mBundle.putIntArray(HRM_VALUE, hrmValues);
				msg.setData(mBundle);
				msg.sendToTarget();
			}
			
			
			
//			int eeval = -1;
//			ArrayList<Integer> rrinterval = new ArrayList<Integer>();
//			if (isHeartRateInUINT16(characteristic.getValue()[0])) {
//				Log.d(TAG, "HeartRateInUINT16");
//				hrmval = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1);
//			} else {
//				Log.d(TAG, "HeartRateInUINT8");
//				hrmval = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
//			}
//			Log.i(TAG, "checking eeval and rr int");
//			if (isEEpresent(characteristic.getValue()[0])) {
//				if (isHeartRateInUINT16(characteristic.getValue()[0])) {
//					eeval = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 3);
//					if (isRRintpresent(characteristic.getValue()[0])) {
//						int startoffset = 5;
//						for (int i = startoffset; i < length; i += 2) {
//							rrinterval.add(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, i));
//						}
//					}
//				} else {
//					eeval = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 2);
//					if (isRRintpresent(characteristic.getValue()[0])) {
//						int startoffset = 4;
//						for (int i = startoffset; i < length; i += 2) {
//							rrinterval.add(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, i));
//						}
//					}
//				}
//			} else {
//				if (isHeartRateInUINT16(characteristic.getValue()[0])) {
//					if (isRRintpresent(characteristic.getValue()[0])) {
//						int startoffset = 3;
//						for (int i = startoffset; i < length; i += 2) {
//							rrinterval.add(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, i));
//						}
//					}
//				} else {
//					if (isRRintpresent(characteristic.getValue()[0])) {
//						int startoffset = 2;
//						for (int i = startoffset; i < length; i += 2) {
//							rrinterval.add(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, i));
//						}
//					}
//				}
//
//			}
//			mBundle.putInt(HRM_VALUE, hrmval);
//			mBundle.putInt(HRM_EEVALUE, eeval);
//			mBundle.putIntegerArrayList(HRM_RRVALUE, rrinterval);
//			msg.setData(mBundle);
//			msg.sendToTarget();
		}

		private boolean isContentArray(byte flag) {
			return true;
		}

		public void onCharacteristicRead(BluetoothGattCharacteristic charac, int status) {
			UUID charUuid = charac.getUuid();
//			if (charUuid.equals(FIRMWARE_REVISON_UUID))
//				return;
			Bundle mBundle = new Bundle();
			Message msg = Message.obtain(mActivityHandler, HRP_VALUE_MSG);
			Log.i(TAG, "onCharacteristicRead");
			if (charUuid.equals(BODY_SENSOR_LOCATION))
				mBundle.putByteArray(BSL_VALUE, charac.getValue());
			if (charUuid.equals(SERIAL_NUMBER_STRING))
				mBundle.putString(SERIAL_STRING, charac.getStringValue(0));
			if (charUuid.equals(MANUFATURE_NAME_STRING))
				mBundle.putByteArray(MANF_NAME, charac.getValue());
			if (charUuid.equals(ICDL))
				mBundle.putByteArray(ICDL_VALUE, charac.getValue());
			msg.setData(mBundle);
			msg.sendToTarget();
		}

		public void onDescriptorRead(BluetoothGattDescriptor descriptor, int status) {
			Log.i(TAG, "onDescriptorRead");
			BluetoothGattCharacteristic mHRMcharac = descriptor.getCharacteristic();
			enableNotification(true, mHRMcharac);
		}
	};

	private void subscribeToHeartbeat(BluetoothDevice device) {
		Log.d(TAG, "Trying to subscribe to heartbeat");
		enableHRNotification(device);
	}
	
	/*
	 * Broadcast mode checker API
	 */
	public boolean checkIfBroadcastMode(byte[] scanRecord) {
		int offset = 0;
		while (offset < (scanRecord.length - 2)) {
			int len = scanRecord[offset++];
			if (len == 0)
				break; // Length == 0 , we ignore rest of the packet
			// TODO: Check the rest of the packet if get len = 0

			int type = scanRecord[offset++];
			switch (type) {
			case ADV_DATA_FLAG:

				if (len >= 2) {
					// The usual scenario(2) and More that 2 octets scenario.
					// Since this data will be in Little endian format, we
					// are interested in first 2 bits of first byte
					byte flag = scanRecord[offset++];
					/*
					 * 00000011(0x03) - LE Limited Discoverable Mode and LE
					 * General Discoverable Mode
					 */
					if ((flag & LIMITED_AND_GENERAL_DISC_MASK) > 0)
						return false;
					else
						return true;
				} else if (len == 1) {
					continue;// ignore that packet and continue with the rest
				}
			default:
				offset += (len - 1);
				break;
			}
		}
		return false;
	}

	public void DummyReadForSecLevelCheck(BluetoothDevice device) {
		boolean result = false;
		if (mBluetoothGatt != null && device != null) {
			BluetoothGattService disService = mBluetoothGatt.getService(device, DIS_UUID);
			if (disService == null) {
				showMessage("Dis service not found!");
				return;
			}

			BluetoothGattCharacteristic firmwareIdCharc = disService.getCharacteristic(FIRMWARE_REVISON_UUID);
			if (firmwareIdCharc == null) {
				showMessage("firmware revison charateristic not found!");
				return;
			}
			result = mBluetoothGatt.readCharacteristic(firmwareIdCharc);
			if (result == false) {
				showMessage("firmware revison reading is failed!");
			}
		}
	}

	private boolean isRRintpresent(byte flags) {
		if ((flags & FIFTH_BITMASK) != 0)
			return true;
		return false;
	}

	private boolean isEEpresent(byte flags) {
		if ((flags & FOURTH_BITMASK) != 0)
			return true;
		return false;
	}

	private boolean isHeartRateInUINT16(byte flags) {
		Log.d(TAG, "isHeartRateInUINT16");
		if ((flags & FIRST_BITMASK) != 0)
			return true;
		return false;
	}

	public boolean enableNotification(boolean enable, BluetoothGattCharacteristic characteristic) {
		if (mBluetoothGatt == null)
			return false;
		if (!mBluetoothGatt.setCharacteristicNotification(characteristic, enable))
			return false;

		BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(CCC);
		if (clientConfig == null)
			return false;

		if (enable) {
			Log.i(TAG, "enable notification");
			clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		} else {
			Log.i(TAG, "disable notification");
			clientConfig.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
		}
		return mBluetoothGatt.writeDescriptor(clientConfig);
	}

	public void getBodySensorLoc(BluetoothDevice device) {
		Log.i(TAG, "getBodySensorLoc");
		BluetoothGattService mHRP = mBluetoothGatt.getService(device, HRP_SERVICE);
		if (mHRP == null) {
			Log.e(TAG, "getBodySensorLoc: mHRP = null");

			return;
		}
		BluetoothGattCharacteristic mBSLcharac = mHRP.getCharacteristic(BODY_SENSOR_LOCATION);
		if (mBSLcharac == null) {
			Log.e(TAG, "getBodySensorLoc: mBSLcharac = null");
			return;
		}
		mBluetoothGatt.readCharacteristic(mBSLcharac);
	}

	public void enableHRNotification(BluetoothDevice device) {
		boolean result = false;
		Log.i(TAG, "enableHRNotification ");
		isNoti = true;
		BluetoothGattService mHRP = mBluetoothGatt.getService(device, HRP_SERVICE);
		if (mHRP == null) {
			Log.e(TAG, "HRP service not found!");
			return;
		}
		BluetoothGattCharacteristic mHRMcharac = mHRP.getCharacteristic(HEART_RATE_MEASUREMENT_CHARAC);
		if (mHRMcharac == null) {
			Log.e(TAG, "HEART RATE MEASUREMENT charateristic not found!");
			return;
		}
		BluetoothGattDescriptor mHRMccc = mHRMcharac.getDescriptor(CCC);
		if (mHRMccc == null) {
			Log.e(TAG, "CCC for HEART RATE MEASUREMENT charateristic not found!");
			return;
		}
		result = mBluetoothGatt.readDescriptor(mHRMccc);
		if (result == false) {
			Log.e(TAG, "readDescriptor() is failed");
			return;
		}

	}

	public void ResetEnergyExpended(BluetoothDevice device) {
		Log.i(TAG, "ResetEnergyExpended ");
		BluetoothGattService mHRP = mBluetoothGatt.getService(device, HRP_SERVICE);
		if (mHRP == null) {
			Log.e(TAG, "HRP service not found!");
			return;
		}
		BluetoothGattCharacteristic mHRCPcharac = mHRP.getCharacteristic(HeartRate_ControlPoint);
		if (mHRCPcharac == null) {
			Log.e(TAG, "HEART RATE Copntrol Point charateristic not found!");
			return;
		}

		byte[] value = new byte[1];
		value[0] = (byte) RESET_ENERGY_EXPANDED;
		mHRCPcharac.setValue(value);
		mBluetoothGatt.writeCharacteristic(mHRCPcharac);
	}

	public void read_uuid_read_25(BluetoothDevice device) {
		Log.i(TAG, "read 0x2A25 uuid charachteristic");
		BluetoothGattService mDI = mBluetoothGatt.getService(device, DEVICE_INFORMATION);
		if (mDI == null) {
			Log.e(TAG, "Device Information Service Not Found!!!");
			return;
		}
		BluetoothGattCharacteristic mSNS = mDI.getCharacteristic(SERIAL_NUMBER_STRING);
		if (mSNS == null) {
			Log.e(TAG, "Serial Number String Characteristic Not Found!!!");
			return;
		}
		mBluetoothGatt.readCharacteristic(mSNS);
	}

	public void read_uuid_read_29(BluetoothDevice device) {
		Log.i(TAG, "read 0x2A29 uuid charachteristic");
		BluetoothGattService mDI = mBluetoothGatt.getService(device, DEVICE_INFORMATION);
		if (mDI == null) {
			Log.e(TAG, "Device Information Service Not Found!!!");
			return;
		}
		BluetoothGattCharacteristic mMNS = mDI.getCharacteristic(MANUFATURE_NAME_STRING);
		if (mMNS == null) {
			Log.e(TAG, "Manufacture Name String Characteristic Not Found!!!");
			return;
		}
		mBluetoothGatt.readCharacteristic(mMNS);
	}

	public void read_uuid_read_2A(BluetoothDevice device) {
		Log.i(TAG, "read 0x2A2A uuid charachteristic");
		BluetoothGattService mDI = mBluetoothGatt.getService(device, DEVICE_INFORMATION);
		if (mDI == null) {
			Log.e(TAG, "Device Information Service Not Found!!!");
			return;
		}
		BluetoothGattCharacteristic mICDL = mDI.getCharacteristic(ICDL);
		if (mICDL == null) {
			Log.e(TAG, "IEEE Characteristic data string Not Found!!!");
			return;
		}
		mBluetoothGatt.readCharacteristic(mICDL);
	}

	public void connect(BluetoothDevice device, boolean autoconnect) {
		if (mBluetoothGatt != null) {
			mBluetoothGatt.connect(device, autoconnect);
		}
	}

	public void disconnect(BluetoothDevice device) {
		if (mBluetoothGatt != null) {
			mBluetoothGatt.cancelConnection(device);
		}
	}

	public void scan(boolean start) {
		if (mBluetoothGatt == null)
			return;
		if (start) {
			mBluetoothGatt.startScan();
		} else {
			mBluetoothGatt.stopScan();
		}
	}

	public void removeBond(BluetoothDevice device) {
		if (mBluetoothGatt != null) {
			mBluetoothGatt.removeBond(device);
		}
	}

	public boolean isBLEDevice(BluetoothDevice device) {
		return mBluetoothGatt.isBLEDevice(device);
	}

	private void showMessage(String msg) {
		Log.e(TAG, msg);
	}

	private boolean subscribe(BluetoothGattCharacteristic characteristic, boolean enable) {
		if (!mBluetoothGatt.setCharacteristicNotification(characteristic, enable))
			return false;
		BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(CCC);
		if (clientConfig == null)
			return false;
		if (enable) {
			clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		} else {
			clientConfig.setValue(new byte[] { 0x00, 0x00 });
		}
		return mBluetoothGatt.writeDescriptor(clientConfig);
	}

	public void disableHRNotification(BluetoothDevice device) {
		BluetoothGattService mHRP = mBluetoothGatt.getService(device, HRP_SERVICE);
		if (mHRP == null) {
			Log.e(TAG, "HRP service not found!");
			return;
		}
		BluetoothGattCharacteristic mHRMcharac = mHRP.getCharacteristic(HEART_RATE_MEASUREMENT_CHARAC);
		if (mHRMcharac == null) {
			Log.e(TAG, "HEART RATE MEASUREMENT charateristic not found!");
			return;
		}
		enableNotification(false, mHRMcharac);
	}

	public void disableNotification(BluetoothDevice device) {
		BluetoothGattService mHRP = mBluetoothGatt.getService(device, HRP_SERVICE);
		if (mHRP == null) {
			Log.e(TAG, "HRP service not found!");
			return;
		}
		BluetoothGattCharacteristic mHRMcharac = mHRP.getCharacteristic(HEART_RATE_MEASUREMENT_CHARAC);
		if (mHRMcharac == null) {
			Log.e(TAG, "HEART RATE MEASUREMENT charateristic not found!");
			return;
		}
		BluetoothGattDescriptor mHRMccc = mHRMcharac.getDescriptor(CCC);
		if (mHRMccc == null) {
			Log.e(TAG, "CCC for HEART RATE MEASUREMENT charateristic not found!");
			return;
		}
		byte[] value = mHRMccc.getValue();
		if (value != null && value[0] == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE[0]) {
			enableNotification(false, mHRMcharac);
		}
	}
	
	synchronized public boolean writeCharacteristic(BluetoothDevice device, UUID service, UUID characteristic, byte[] value) {
		BluetoothGattService gattService = mBluetoothGatt.getService(device, service);

		if(gattService != null) {
			BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(characteristic);
			
			if(gattCharacteristic != null) {
				
				Log.d(TAG, "Send " + toHexString(value) + " to " + characteristic);
				
				gattCharacteristic.setValue(value);
				boolean writeCharacteristic = mBluetoothGatt.writeCharacteristic(gattCharacteristic);
				return writeCharacteristic;
			} else {
				Log.e(TAG, "Characteristic " + characteristic + " not available");
			}
		} else {
			Log.e(TAG, "GATT service not available");
		}
		return false;
	}
	
	public static String toHexString(byte[] bytes) {
	    char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	    char[] hexChars = new char[bytes.length * 2];
	    int v;
	    for ( int j = 0; j < bytes.length; j++ ) {
	        v = bytes[j] & 0xFF;
	        hexChars[j*2] = hexArray[v/16];
	        hexChars[j*2 + 1] = hexArray[v%16];
	    }
	    return new String(hexChars);
	}

	public boolean writeCharacteristic(
			CharacteristicWriteData data) {
		return writeCharacteristic(data.getDevice(), data.getService(), data.getCharacteristic(), data.getData());
	}
}
