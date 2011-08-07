package hu.copas.android.ProximityService;

import java.util.Date;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class ProximityService extends Service {

	final private static int proximityServiceNotificationId = 1;
	final private static int PROXIMITY_SCREEN_OFF_WAKE_LOCK = 32;
	final private static int proximityServiceIcon = R.drawable.proximity;
	final private static int proximityServiceIconOn = R.drawable.proximityon;
	PowerManager.WakeLock proximityWakeLock;
	KeyguardManager keyGuardManager;
	public Intent serviceIntent;

	//BroadcastReceiver phoneStateChangeReceiver = null;
	private BroadcastReceiver screenOffReceiver = null;
	private BroadcastReceiver userPresentReceiver = null;
	
	private boolean receivers_registered;
	private boolean event_controlled;
	private boolean show_icon;
	
    public class LocalBinder extends Binder {
    	ProximityService getService() {
            return ProximityService.this;
        }
    }
    
    private final IBinder mBinder = new LocalBinder();
    
    @Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	private NotificationManager notificationManager;
	public void showNotification(int icon, String tickerText, String contentTitle, String contentText) {
		Notification n;
		PendingIntent pi;
		pi = PendingIntent.getActivity(this, 0, new Intent(this, ProximityServiceMain.class), 0);
		//Intent mainActivityIntent = new Intent(getApplicationContext(), ProximityServiceMain.class);
        //pi = PendingIntent.getActivity(getApplicationContext(), 0, mainActivityIntent, 0);
		n = new Notification(icon, tickerText, System.currentTimeMillis());
		n.flags |= Notification.FLAG_ONGOING_EVENT;
		n.setLatestEventInfo(this, contentTitle, contentText, pi);
		notificationManager.notify(proximityServiceNotificationId, n);
		if (!show_icon)
			cancelNotification();
	}
	
	public void cancelNotification() {
		notificationManager.cancel(proximityServiceNotificationId);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		keyGuardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		proximityWakeLock = pm.newWakeLock(PROXIMITY_SCREEN_OFF_WAKE_LOCK, "PTWLTAG");
		proximityWakeLock.setReferenceCounted(false);
		
		receivers_registered = false;
		screenOffReceiver = new ScreenOffReceiver();
		userPresentReceiver = new UserPresentReceiver();
	}

	@Override
	public void onDestroy() {
		toggleReceivers(false);
		toggleProximityWakeLock(false);
		showNotification(proximityServiceIcon, getString(R.string.service_stopped), "", "");
		cancelNotification();
		super.onDestroy();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startid) {
		serviceIntent = intent;
		String ec = "general.";
		if (intent != null) {
			event_controlled = intent.getBooleanExtra("event_controlled", false);
			if (event_controlled)
				ec = "event controlled.";
			show_icon = intent.getBooleanExtra("show_icon", false);
		}
		showNotification(proximityServiceIcon, getString(R.string.service_started), getString(R.string.service_running), "Proximity sensing: " + ec);
		if (!event_controlled)
			toggleProximityWakeLock(true);
		else
			toggleReceivers(true);
		return START_STICKY;
	}

	private void toggleProximityWakeLock(boolean on) {
		if (on) {
			if (!proximityWakeLock.isHeld()) {
				proximityWakeLock.acquire();
				Log.i(getString(R.string.app_name), "proximityWakeLock aquired.");
				if (show_icon)
					showNotification(proximityServiceIconOn, getString(R.string.service_started), getString(R.string.service_active), "Proximity sensing activated.");
			}
		} else {
			if (proximityWakeLock.isHeld()) {
				int i = 0;
				while (proximityWakeLock.isHeld() && i < 100) {
					proximityWakeLock.release();
					i++;
				}
				if (proximityWakeLock.isHeld())
					Log.e(getString(R.string.app_name), "BUG: proximityWakeLock could not be released after " + String.valueOf(i) + " attempts." );
				else
					Log.i(getString(R.string.app_name), "proximityWakeLock released after " + String.valueOf(i) + " number of attempts." );
					if (show_icon)
						showNotification(proximityServiceIcon, getString(R.string.service_started), getString(R.string.service_active), "Proximity sensing deactivated.");
			}
		}
	}
	
	public class ScreenOffReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			Log.i(getString(R.string.app_name), "Received event: ACTION_SCREEN_OFF");
			int i = 5;
			while (i > 0 && !keyGuardManager.inKeyguardRestrictedInputMode())
				try {
					i--;
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			if (keyGuardManager.inKeyguardRestrictedInputMode())
				toggleProximityWakeLock(true);
			else
				Log.i(getString(R.string.app_name), "Not activated sensing since phone is still not locked.");
		}
	}
	
	public class UserPresentReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(getString(R.string.app_name), "Received event: ACTION_USER_PRESENT");
			toggleProximityWakeLock(false);
		}
	}
	
	public void toggleReceivers(boolean start) {
		if (start) {
			if (!receivers_registered) {
				//registerReceiver(phoneStateChangeReceiver, new IntentFilter(android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED));
				registerReceiver(screenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
				registerReceiver(userPresentReceiver, new IntentFilter(Intent.ACTION_USER_PRESENT));
				receivers_registered = true;
			}
		} else {
			if (receivers_registered) {
				unregisterReceiver(screenOffReceiver);
				unregisterReceiver(userPresentReceiver);
				receivers_registered = false;
			}
		}
	}
	
}
