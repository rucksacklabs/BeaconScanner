package de.reneruck.android.beaconscanner;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

public class AppContext extends Application {

    private static final String TAG = "AppContext";
    private String valueToKeep;
    
    @Override
    public void onCreate() {
    	SharedPreferences preferences = getSharedPreferences("my_prefs", Context.MODE_PRIVATE);
    	this.valueToKeep = preferences.getString("user", "no user");
    	
    	super.onCreate();
    }
    
	public String getValueToKeep() {
		return valueToKeep;
	}

	public void setValueToKeep(String valueToKeep) {
		this.valueToKeep = valueToKeep;
	}
}
