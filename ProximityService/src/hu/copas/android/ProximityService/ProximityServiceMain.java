package hu.copas.android.ProximityService;

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.Toast;

public class ProximityServiceMain extends Activity {

	private ProximityService boundService = null;
	private Intent intentService = null;
	private SharedPreferences settingsReader;
	private SharedPreferences.Editor settingsWriter;
	
	private class ServiceConn implements ServiceConnection {

		public void onServiceConnected(ComponentName name, IBinder service) {
			boundService = ((ProximityService.LocalBinder)service).getService();
			// boundService.mainIntent = ProximityServiceMain.this.getIntent();
		}

		public void onServiceDisconnected(ComponentName name) {
			boundService = null;
		}
		
	} 
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		settingsReader = getSharedPreferences("preferences", 0);
		settingsWriter = settingsReader.edit();
        setContentView(R.layout.main);
        CheckBox cbServiceRunning = (CheckBox)(findViewById(R.id.cbServiceRunning));
        cbServiceRunning.setChecked(isServiceRunning());
        CheckBox cbEventControlled = (CheckBox)(findViewById(R.id.cbEventControlled));
        cbEventControlled.setChecked(settingsReader.getBoolean("event_controlled", true));
		settingsWriter.putBoolean("event_controlled", cbEventControlled.isChecked());
		settingsWriter.commit();
        CheckBox cbShowIcon = (CheckBox)(findViewById(R.id.cbShowIcon));
        cbShowIcon.setChecked(settingsReader.getBoolean("show_icon", true));
		settingsWriter.putBoolean("show_icon", cbShowIcon.isChecked());
		settingsWriter.commit();
        cbServiceRunning.setOnClickListener(
        		new OnClickListener() {
        			@Override
        			public void onClick(View v) {
        				settingsWriter.putBoolean("service_running", ((CheckBox)v).isChecked());
        				settingsWriter.commit();
        				toggleService(((CheckBox)v).isChecked());
        			}
        		}
        );
        cbEventControlled.setOnClickListener(
        		new OnClickListener() {
        			@Override
        			public void onClick(View v) {
        				settingsWriter.putBoolean("event_controlled", ((CheckBox)v).isChecked());
        				settingsWriter.commit();
        				if (isServiceRunning()) {
        					toggleService(false);
        					toggleService(true);
        				}
        			}
        		}
        );
        cbShowIcon.setOnClickListener(
        		new OnClickListener() {
        			@Override
        			public void onClick(View v) {
        				settingsWriter.putBoolean("show_icon", ((CheckBox)v).isChecked());
        				settingsWriter.commit();
        				if (isServiceRunning()) {
        					toggleService(false);
        					toggleService(true);
        				}
        			}
        		}
        );
        // finish();
    }
    
    private boolean isServiceRunning() {
		boolean result = false;
        ActivityManager am = (ActivityManager) this.getSystemService (ACTIVITY_SERVICE);
        List<RunningServiceInfo> rsi = am.getRunningServices(Integer.MAX_VALUE);
        for (RunningServiceInfo runningServiceInfo : rsi) {
			if (runningServiceInfo.service.getClassName().equals(ProximityService.class.getName())) {
				result = true;
			}
		}
        return result;
	}

    private void toggleService(boolean start) {
        intentService = new Intent(getApplicationContext(), ProximityService.class);
        // ServiceConn serviceConn = new ServiceConn();
        if (start) {
	        if (!isServiceRunning()) {
	        	// bindService(intentService, serviceConn, Context.BIND_AUTO_CREATE);
	        	intentService.putExtra("event_controlled", ((CheckBox)findViewById(R.id.cbEventControlled)).isChecked());
	        	intentService.putExtra("show_icon", ((CheckBox)findViewById(R.id.cbShowIcon)).isChecked());
	        	startService(intentService);
        		// unbindService(serviceConn);
        	}
        } else {
        	if (isServiceRunning()) {
	        	stopService(intentService);
	        	/*
	        	if (boundService != null) {
	        		unbindService(serviceConn);
	        	}
	        	*/
        	}
        }
    }
    
}