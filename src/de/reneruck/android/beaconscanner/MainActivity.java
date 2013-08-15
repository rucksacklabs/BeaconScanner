package de.reneruck.android.beaconscanner;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Arrays;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.android.sdk.bt.gatt.BluetoothGattService;

public class MainActivity extends Activity {

	private static final String TAG = "MainActivity";
	private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int STATE_READY = 10;
    private static final int HRP_PROFILE_CONNECTED = 20;
    private static final int HRP_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;
    
	private Context mContext;
	private BluetoothAdapter mBtAdapter;
	private HRPService mService;
    private BluetoothDevice mDevice;
    private int mState;
	private List<BluetoothGattService> mServices;
	private SeekBar mSeekbarCI;
	private SeekBar mSeekbarSL;
	private SeekBar mSeekbarSR;
	private SeekBar mSeekbarBL;
	private TextView mCIValue;
	private TextView mSLValue;
	private TextView mSRValue;
	private TextView mBLValue;
	private boolean mInitSeekbars = false;
	private boolean mIsConnected = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
        mContext = getApplicationContext();
        
        AppContext context = (AppContext) getApplicationContext();
        
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
		((Button) findViewById(R.id.button_settings_update)).setEnabled(false);

		init();
	}
	
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }

    private void init() {
    	Log.d(TAG, "init() mService= " + mService);
    	Intent bindIntent = new Intent(this, HRPService.class);
    	startService(bindIntent);
    	bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    	IntentFilter filter = new IntentFilter();
    	filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
    	filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
    	this.registerReceiver(deviceStateListener, filter);
    }
    
    private BroadcastReceiver deviceStateListener = new BroadcastReceiver() {

		@Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final Intent mIntent = intent;
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice device = mIntent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int devState = mIntent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                Log.d(TAG, "BluetoothDevice.ACTION_BOND_STATE_CHANGED");
                if (device.equals(mDevice) && devState == BluetoothDevice.BOND_NONE) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            mDevice = null;
                        }
                    });
                }
            }
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = mIntent.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.ERROR);
                Log.d(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED" + "state is" + state);
                    runOnUiThread(new Runnable() {

						public void run() {
                            if ( state == STATE_OFF) {
                                mDevice=null;
                                mState = HRP_PROFILE_DISCONNECTED;
                                Log.d(TAG, "Device disconnected!");
                            }
                        }
                    });
            }
        }
    };
	
	private OnSeekBarChangeListener mSeekbarDragListener = new OnSeekBarChangeListener() {
		
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			if(HRPService.sendDone) {
				((Button) findViewById(R.id.button_settings_update)).setEnabled(true);
			}
		}
		
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
//			((Button) findViewById(R.id.button_settings_update)).setEnabled(false);
		}
		
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			if(mInitSeekbars) {
				switch (seekBar.getId()) {
				case R.id.seekBar_CI:
					mCIValue.setText(progress * 1.25 + " ms");
					break;
				case R.id.seekBar_SL:
					mSLValue.setText(Integer.toString(progress));
					break;
				case R.id.seekBar_SR:
					mSRValue.setText(progress + " Hz");
					break;
				case R.id.seekBar_BL:
					mBLValue.setText(progress + " bytes");
					break;
	
				default:
					break;
				}
			}
		}
	};
	private OnClickListener mUpdateButtonListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			if(mIsConnected) {
				WriteCharacteristicAsync writeCharacteristicAsync = new WriteCharacteristicAsync(mService, characteristicWritingCallback);
				writeCharacteristicAsync.execute(
				new CharacteristicWriteData(mDevice,
						HRPService.CONNECTION_CONTROL_SERVICE,
						HRPService.CONNECTION_CONTROL_CI_CHARAC, 
						BigInteger.valueOf(mSeekbarCI.getProgress()).toByteArray()),
				new CharacteristicWriteData(mDevice,
						HRPService.CONNECTION_CONTROL_SERVICE,
						HRPService.CONNECTION_CONTROL_SL_CHARAC, 
						BigInteger.valueOf(mSeekbarSL.getProgress()).toByteArray()),
				new CharacteristicWriteData(mDevice,
						HRPService.CONNECTION_CONTROL_SERVICE,
						HRPService.CONNECTION_CONTROL_SR_CHARAC, 
						BigInteger.valueOf(mSeekbarSR.getProgress()).toByteArray()),
				new CharacteristicWriteData(mDevice,
						HRPService.CONNECTION_CONTROL_SERVICE,
						HRPService.CONNECTION_CONTROL_BL_CHARAC, 
						BigInteger.valueOf(mSeekbarBL.getProgress()).toByteArray()));
				
				findViewById(R.id.progressBar1).setVisibility(View.VISIBLE);
				((Button) v).setEnabled(false);
			} else {
				Toast.makeText(getApplicationContext(), "Not connected to a Device!", Toast.LENGTH_SHORT).show();
			}
		}
	};

	private Callback characteristicWritingCallback = new Callback() {

		@Override
		public boolean handleMessage(Message msg) {
			runOnUiThread(new Runnable() {
				public void run() {
					findViewById(R.id.progressBar1).setVisibility(View.INVISIBLE);
					((Button)findViewById(R.id.button_settings_update)).setEnabled(true);
				}
			});
			return false;
		}
		
	};
	
	public static byte[] toByteArray(double value) {
	    byte[] bytes = new byte[8];
	    ByteBuffer.wrap(bytes).putDouble(value);
	    return bytes;
	}
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	
    	switch (requestCode) {
		case REQUEST_SELECT_DEVICE:
			
			
			if(resultCode != Activity.RESULT_CANCELED && data != null) {
				
				Bundle extras = data.getExtras();
				mDevice = extras.getParcelable(BluetoothDevice.EXTRA_DEVICE);
				
				((TextView)findViewById(R.id.device_name)).setText(mDevice.getName());
				((TextView)findViewById(R.id.device_address)).setText(mDevice.getAddress());
				
				initSeekbars();
				
				((Button) findViewById(R.id.button_settings_update)).setOnClickListener(mUpdateButtonListener );
				mIsConnected = true;
				getActionBar().setSubtitle("- Connected -");
			} else {
				Toast.makeText(getApplicationContext(), "Device selection cancelled", Toast.LENGTH_SHORT).show();
			}
			
			break;
		case REQUEST_ENABLE_BT:
			if(resultCode == Activity.RESULT_OK){
				Log.i(TAG, "Bluetooth activated");
			} else {
				Log.e(TAG, "Error while activating Bluetooth");
			}
			break;
		default:
			break;
		}
    }

	private void initSeekbars() {
		mSeekbarCI = (SeekBar)findViewById(R.id.seekBar_CI);
		mSeekbarSL = (SeekBar)findViewById(R.id.seekBar_SL);
		mSeekbarSR = (SeekBar)findViewById(R.id.seekBar_SR);
		mSeekbarBL = (SeekBar)findViewById(R.id.seekBar_BL);
		
		mSeekbarCI.setOnSeekBarChangeListener(mSeekbarDragListener);
		mSeekbarSL.setOnSeekBarChangeListener(mSeekbarDragListener);
		mSeekbarSR.setOnSeekBarChangeListener(mSeekbarDragListener);
		mSeekbarBL.setOnSeekBarChangeListener(mSeekbarDragListener);

		mSeekbarCI.setMax(3200);
		mSeekbarSL.setMax(10);
		mSeekbarSR.setMax(100);
		mSeekbarBL.setMax(60);
		
		mCIValue = (TextView)findViewById(R.id.CI_value);
		mSLValue = (TextView)findViewById(R.id.SL_value);
		mSRValue = (TextView)findViewById(R.id.SR_value);
		mBLValue = (TextView)findViewById(R.id.BL_value);
		
		mInitSeekbars = true;
	}
    
    @Override
    public void onDestroy() {
        unbindService(mServiceConnection);
        unregisterReceiver(deviceStateListener);
        stopService(new Intent(this, HRPService.class));
        super.onDestroy();
    }
	
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((HRPService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            mService.setActivityHandler(mHandler);
        }

        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
        }
    };
    
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
            case HRPService.HRP_CONNECT_MSG:
                Log.d(TAG, "mHandler.HRP_CONNECT_MSG");
                runOnUiThread(new Runnable() {
                    public void run() {
                        mState = HRP_PROFILE_CONNECTED;
                        Log.d(TAG, "Connected!!");
                    }
                });
                break;

            case HRPService.HRP_DISCONNECT_MSG:
                Log.d(TAG, "mHandler.HRP_DISCONNECT_MSG");
                runOnUiThread(new Runnable() {
                    public void run() {
                        mState = HRP_PROFILE_DISCONNECTED;
                        Log.d(TAG, "Disconnected!!");
                    }
                });
                break;
            case HRPService.SERVICES_DISCOVERED:
            	Log.d(TAG, "mHandler.SERVICES_DISCOVERED");
            	runOnUiThread(new Runnable() {
            		public void run() {
            			mState = STATE_READY;
//            			listServices((List)msg.obj);
            		}
            	});
            	break;
            case HRPService.HRP_READY_MSG:
                Log.d(TAG, "mHandler.HRP_READY_MSG");
                runOnUiThread(new Runnable() {
                    public void run() {
                        mState = STATE_READY;
                        Log.d(TAG, "Ready MSG");
                    }
                });
                break;
                
            case HRPService.HRP_VALUE_MSG:
            	Log.d(TAG, "mHandler.HRP_VALUE_MSG");
            	Bundle data = msg.getData();
            	int[] hrmValue = data.getIntArray(HRPService.HRM_VALUE);
//            	int hrmEEValue = data.getInt(HRPService.HRM_EEVALUE);
//            	ArrayList<Integer> hrmRRValue = data.getIntegerArrayList(HRPService.HRM_RRVALUE);
            	
            	Log.d(TAG, "HRP: " + hrmValue);
            	((TextView) findViewById(R.id.HRP_value)).setText(Arrays.toString(hrmValue) + " bpm");
            	break;
            default:
                super.handleMessage(msg);
            }
        }
    };
    
	private class ServiceAdapter extends BaseAdapter {

		private List<BluetoothGattService> mList;
		private LayoutInflater inflater;

		public ServiceAdapter(List<BluetoothGattService> list) {
			mList = list;
		}
		
		@Override
		public int getCount() {
			return mList.size();
		}

		@Override
		public BluetoothGattService getItem(int position) {
			return mList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return mList.get(position).hashCode();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			 View vg;

	            if (convertView != null) {
	                vg = (View) convertView;
	            } else {
	                vg = (View) inflater.inflate(android.R.layout.simple_list_item_1, null);
	            }

	            BluetoothGattService btService = mList.get(position);
	            ((TextView) vg).setText(btService.getUuid().toString());

	            return vg;
		}
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		
		switch (item.getItemId()) {
		case R.id.action_select_device:
			if(mState == HRP_PROFILE_CONNECTED) {
				mService.disconnect(mDevice);
			} else {
				
			 if (!mBtAdapter.isEnabled()) {
	                Log.i(TAG, "onClick - BT not enabled yet");
	                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
	            }
	            else {
	                Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
	                startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
	            }
			}
			break;

		default:
			break;
		}
		return true;
	}

}
