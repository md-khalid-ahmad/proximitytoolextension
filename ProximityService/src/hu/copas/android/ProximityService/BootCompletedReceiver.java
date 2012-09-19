package hu.copas.android.ProximityService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompletedReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		if (ProximityServiceHelper.settingsReader(arg0).getBoolean("auto_start", true))
			ProximityServiceHelper.toggleService(arg0, true);
	}

}
