package hu.copas.android.ProximityService;

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class ProximityServiceHelper extends Activity {

	final private static int proximityServiceNotificationId = 1;
	final public static int proximityServiceIcon = R.drawable.proximity;
	final public static int proximityServiceIconOn = R.drawable.proximityon;
	
	public static void showNotification(Context context, int icon, String tickerText, String contentTitle, String contentText) {
		final NotificationManager notificationManager = (NotificationManager)(context.getSystemService(NOTIFICATION_SERVICE));
		Notification n;
		PendingIntent pi;
		pi = PendingIntent.getActivity(context, 0, new Intent(context, ProximityServiceMain.class), 0);
		n = new Notification(icon, tickerText, System.currentTimeMillis());
		n.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
		//if (!"".equals(contentTitle + contentText))
			// n.flags |= Notification.FLAG_FOREGROUND_SERVICE;
		n.setLatestEventInfo(context, contentTitle, contentText, pi);
		notificationManager.notify(proximityServiceNotificationId, n);
	}
	
	public static void cancelNotification(Context context) {
		final NotificationManager notificationManager = (NotificationManager)(context.getSystemService(NOTIFICATION_SERVICE));
		notificationManager.cancel(proximityServiceNotificationId);
	}

	public static SharedPreferences settingsReader(Context context) {
		return context.getSharedPreferences("preferences", 0);
	}
	
	public static SharedPreferences.Editor settingsWriter(Context context) {
		return settingsReader(context).edit();
	}
		
    public static boolean isServiceRunning(Context context) {
		boolean result = false;
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        List<RunningServiceInfo> rsi = am.getRunningServices(Integer.MAX_VALUE);
        for (RunningServiceInfo runningServiceInfo : rsi) {
			if (runningServiceInfo.service.getClassName().equals(ProximityService.class.getName())) {
				result = true;
			}
		}
        return result;
	}

    public static void toggleService(Context context, boolean start) {
    	Intent intentService = new Intent(context, ProximityService.class);
        // ServiceConn serviceConn = new ServiceConn();
        if (start) {
	        if (!isServiceRunning(context)) {
	        	// bindService(intentService, serviceConn, Context.BIND_AUTO_CREATE);
	        	context.startService(intentService);
        		// unbindService(serviceConn);
        	}
        } else {
        	if (isServiceRunning(context)) {
	        	context.stopService(intentService);
	        	/*
	        	if (boundService != null) {
	        		unbindService(serviceConn);
	        	}
	        	*/
        	}
        }
    }
        
}
