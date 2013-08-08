package de.reneruck.android.beaconscanner;

import android.os.AsyncTask;
import android.os.Handler.Callback;
import android.util.Log;

public class WriteCharacteristicAsync extends AsyncTask<CharacteristicWriteData, Boolean, Void> {

	private static final String TAG = "WriteCharacteristicAsync";
	private HRPService service;
	private Callback callback;
	
	
	
	public WriteCharacteristicAsync(HRPService service, Callback callback) {
		this.service = service;
		this.callback = callback;
	}

	@Override
	protected Void doInBackground(
			CharacteristicWriteData... paramArrayOfParams) {
		int currentPos = 0;
		boolean isDone = false;
		
		while(!isDone) {
			if(HRPService.sendDone) {
				HRPService.sendDone = false;
				boolean result = service.writeCharacteristic(paramArrayOfParams[currentPos]);
				if(!result) {
					Log.e(TAG, "Error while writing characteristic " + paramArrayOfParams[currentPos].getCharacteristic());
					HRPService.sendDone = true;
				}
				currentPos++;
				if(currentPos == paramArrayOfParams.length) {
					isDone = true;
				}
			}
		}
		return null;
	}
	@Override
	protected void onPostExecute(Void result) {
		Log.d(TAG, "Async Task DONE!");
		this.callback.handleMessage(null);
		super.onPostExecute(result);
	}
}
