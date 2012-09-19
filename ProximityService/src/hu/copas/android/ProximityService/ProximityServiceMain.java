package hu.copas.android.ProximityService;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

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
        final CheckBox cbServiceRunning = (CheckBox)(findViewById(R.id.cbServiceRunning));
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
        Spinner spinner_control = (Spinner)(findViewById(R.id.spinner_control));
        spinner_control.setSelection(settingsReader.getInt("control", 0));
        spinner_control.setOnItemSelectedListener( new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				settingsWriter.putInt("control", arg2);
				settingsWriter.commit();
				ProximityServiceHelper.toggleService(getApplicationContext(), cbServiceRunning.isChecked());
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
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
        					ProximityServiceHelper.toggleService(getApplicationContext(), cbServiceRunning.isChecked());
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