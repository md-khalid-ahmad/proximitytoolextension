package hu.copas.android.ProximityService;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;

public class ProximityServiceMain extends Activity {

	private ProximityService boundService = null;
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
		settingsReader = ProximityServiceHelper.settingsReader(this);
		settingsWriter = settingsReader.edit();
        setContentView(R.layout.main);
        CheckBox cbServiceRunning = (CheckBox)(findViewById(R.id.cbServiceRunning));
        cbServiceRunning.setChecked(ProximityServiceHelper.isServiceRunning(this));
        cbServiceRunning.setOnClickListener(
        		new OnClickListener() {
        			@Override
        			public void onClick(View v) {
        				settingsWriter.putBoolean("service_running", ((CheckBox)v).isChecked());
        				settingsWriter.commit();
        				ProximityServiceHelper.toggleService(getApplicationContext(), ((CheckBox)v).isChecked());
        			}
        		}
        );
        CheckBox cbEventControlled = (CheckBox)(findViewById(R.id.cbEventControlled));
        cbEventControlled.setChecked(settingsReader.getBoolean("event_controlled", true));
        cbEventControlled.setOnClickListener(
        		new OnClickListener() {
        			@Override
        			public void onClick(View v) {
        				settingsWriter.putBoolean("event_controlled", ((CheckBox)v).isChecked());
        				settingsWriter.commit();
        				if (ProximityServiceHelper.isServiceRunning(getApplicationContext())) {
        					ProximityServiceHelper.toggleService(getApplicationContext(), false);
        					ProximityServiceHelper.toggleService(getApplicationContext(), true);
        				}
        			}
        		}
        );
        CheckBox cbShowIcon = (CheckBox)(findViewById(R.id.cbShowIcon));
        cbShowIcon.setChecked(settingsReader.getBoolean("show_icon", true));
        cbShowIcon.setOnClickListener(
        		new OnClickListener() {
        			@Override
        			public void onClick(View v) {
        				settingsWriter.putBoolean("show_icon", ((CheckBox)v).isChecked());
        				settingsWriter.commit();
        				if (ProximityServiceHelper.isServiceRunning(getApplicationContext())) {
        					ProximityServiceHelper.toggleService(getApplicationContext(), false);
        					ProximityServiceHelper.toggleService(getApplicationContext(), true);
        				}
        			}
        		}
        );
        CheckBox cbAutoStart = (CheckBox)(findViewById(R.id.cbAutoStart));
        cbAutoStart.setChecked(settingsReader.getBoolean("auto_start", true));
        cbAutoStart.setOnClickListener(
        		new OnClickListener() {
        			@Override
        			public void onClick(View v) {
        				settingsWriter.putBoolean("auto_start", ((CheckBox)v).isChecked());
        				settingsWriter.commit();
        			}
        		}
        );
        // finish();
    }    
}