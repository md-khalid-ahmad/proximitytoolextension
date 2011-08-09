package hu.copas.android.ProximityService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootCompletedReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		Context context = arg0;
		SharedPreferences settingsReader = context.getSharedPreferences("preferences", 0);
		if (settingsReader.getBoolean("auto_start", true)) {
			ProximityServiceHelper.toggleService(context, true);
		}
	}

}
