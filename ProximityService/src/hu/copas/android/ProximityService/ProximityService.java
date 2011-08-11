package hu.copas.android.ProximityService;

import android.app.KeyguardManager;
import android.app.Service;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class ProximityService extends Service {

	final private static int PROXIMITY_SCREEN_OFF_WAKE_LOCK = 32;
	final private static int remoteAudioClass = BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE | BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES | BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO;

	private PowerManager.WakeLock proximityWakeLock;
	private KeyguardManager keyGuardManager;
	// public Intent serviceIntent;

	//BroadcastReceiver phoneStateChangeReceiver = null;
	private ScreenOffReceiver screenOffReceiver;
	private UserPresentReceiver userPresentReceiver;
	private HeadSetPlugReceiver headSetPlugReceiver;
	private BluetoothConnectReceiver bluetoothConnectReceiver;
	private BluetoothDisconnectReceiver bluetoothDisconnectReceiver;
	private boolean receiversRegistered = false;
	private boolean headsetConnected = false;
	private boolean bluetoothConnected = false;
	private boolean event_controlled;
	private boolean show_icon;
	private boolean headset_controlled;
	
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

	@Override
	public void onCreate() {
		super.onCreate();
		screenOffReceiver = new ScreenOffReceiver();
		userPresentReceiver = new UserPresentReceiver();
		headSetPlugReceiver = new HeadSetPlugReceiver();
		bluetoothConnectReceiver = new BluetoothConnectReceiver();
		bluetoothDisconnectReceiver = new BluetoothDisconnectReceiver();
		keyGuardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		proximityWakeLock = pm.newWakeLock(PROXIMITY_SCREEN_OFF_WAKE_LOCK, "PTWLTAG");
		proximityWakeLock.setReferenceCounted(false);
		registerReceiver(headSetPlugReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
		registerReceiver(bluetoothConnectReceiver, new IntentFilter(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED));
		registerReceiver(bluetoothDisconnectReceiver, new IntentFilter(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED));
		doStartService(false);
	}

	@Override
	public void onDestroy() {
		doStopService(false);
		unregisterReceiver(headSetPlugReceiver);
		unregisterReceiver(bluetoothConnectReceiver);
		unregisterReceiver(bluetoothDisconnectReceiver);
		super.onDestroy();
	}
	
	private void doStartService(boolean silent) {
		show_icon = ProximityServiceHelper.settingsReader(this).getBoolean("show_icon", true);
		headset_controlled = ProximityServiceHelper.settingsReader(this).getBoolean("headset_controlled", true);
		event_controlled = ProximityServiceHelper.settingsReader(this).getBoolean("event_controlled", true)
			&& !(headset_controlled && (headsetConnected || bluetoothConnected));
		String ec = "general.";
		if (event_controlled)
			ec = "event controlled.";
		if (!silent) {
			ProximityServiceHelper.showNotification(this, ProximityServiceHelper.proximityServiceIcon, getString(R.string.service_started), getString(R.string.service_running), "Proximity sensing: " + ec);
			if (!show_icon)
				ProximityServiceHelper.cancelNotification(this);
		}
		if (!event_controlled)
			toggleProximityWakeLock(true);
		else
			toggleReceivers(true);
	}
	
	private void doStopService(boolean silent) {
		toggleReceivers(false);
		toggleProximityWakeLock(false);
		if (!silent) {
			ProximityServiceHelper.showNotification(this, ProximityServiceHelper.proximityServiceIcon, getString(R.string.service_stopped), "", "");
			ProximityServiceHelper.cancelNotification(this);
		}
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startid) {
		return START_STICKY;
	}
	
	public void toggleReceivers(boolean start) {
		if (start) {
			if (!receiversRegistered) {
				//registerReceiver(phoneStateChangeReceiver, new IntentFilter(android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED));
				registerReceiver(screenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
				registerReceiver(userPresentReceiver, new IntentFilter(Intent.ACTION_USER_PRESENT));
				receiversRegistered = true;
				if (keyGuardManager.inKeyguardRestrictedInputMode())
					toggleProximityWakeLock(true);
			}
		} else {
			if (receiversRegistered) {
				unregisterReceiver(screenOffReceiver);
				unregisterReceiver(userPresentReceiver);
				receiversRegistered = false;
			}
		}
	}
	
	public void toggleProximityWakeLock(boolean on) {
		if (on) {
			if (!proximityWakeLock.isHeld()) {
				proximityWakeLock.acquire();
				Log.i(getString(R.string.app_name), "proximityWakeLock aquired.");
				if (show_icon)
					ProximityServiceHelper.showNotification(this, ProximityServiceHelper.proximityServiceIconOn, getString(R.string.service_started), getString(R.string.service_active), "Proximity sensing activated.");
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
						ProximityServiceHelper.showNotification(this, ProximityServiceHelper.proximityServiceIcon, getString(R.string.service_started), getString(R.string.service_active), "Proximity sensing deactivated.");
			}
		}
	}

	public class ScreenOffReceiver extends BroadcastReceiver {
		
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			if (event_controlled) {
				Log.i(getString(R.string.app_name), "Received event: ACTION_SCREEN_OFF");
				int i = 30;
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

	}
	
	public class UserPresentReceiver extends BroadcastReceiver {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i(getString(R.string.app_name), "Received event: ACTION_USER_PRESENT");
			if (event_controlled) {
				toggleProximityWakeLock(false);
			}
		}

	}

	public class HeadSetPlugReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context arg0, Intent arg1) {
			Intent intent = arg1;
			/*
			Log.i(getString(R.string.app_name), "Received event: ACTION_HEADSET_PLUG."
					+ " state:" + String.valueOf(intent.getIntExtra("state", 0))
					+ " microphone:" + String.valueOf(intent.getIntExtra("microphone", 0))
					+ " name:" + intent.getStringExtra("name")
					);
			*/
			if (headset_controlled && intent != null) {
				headsetConnected = intent.getIntExtra("state", 0) != 0;
				if (headsetConnected)
					headsetConnected = intent.getIntExtra("microphone", 0) != 0;
				String s = "";
				if (headsetConnected) {
					s += " Wired headset with microphone connected.";
				} else
					s += " No wired headset connected.";
				Log.i(getString(R.string.app_name), "Received event: ACTION_HEADSET_PLUG." + s);
				doStopService(true);
				doStartService(true);
			}
		}
	}

	public class BluetoothConnectReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (headset_controlled && intent != null) {
				BluetoothDevice btDevice = intent.getExtras().getParcelable(BluetoothDevice.EXTRA_DEVICE);
				int btDevClass = btDevice.getBluetoothClass().getDeviceClass();
				String s = "";
				if ((btDevClass & remoteAudioClass) != 0) {
					s += " Bluetooth audio headset connected.";
					bluetoothConnected = true;
				} else {
					s += " Bluetooth device without audio connected.";
				}
				Log.i(getString(R.string.app_name), "Received event: ACTION_ACL_CONNECTED." + s);
				doStopService(true);
				doStartService(true);
			}
		}
		
	}
	
	public class BluetoothDisconnectReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (headset_controlled && intent != null) {
				BluetoothDevice btDevice = intent.getExtras().getParcelable(BluetoothDevice.EXTRA_DEVICE);
				int btDevClass = btDevice.getBluetoothClass().getDeviceClass();
				String s = "";
				if ((btDevClass & remoteAudioClass) != 0) {
					s += " Bluetooth audio headset disconnected.";
					bluetoothConnected = false;
				} else
					s += " Bluetooth device without audio disconnected.";
				Log.i(getString(R.string.app_name), "Received event: ACTION_ACL_DISCONNECTED." + s);
				doStopService(true);
				doStartService(true);
			}
		}
		
	}
	
}
