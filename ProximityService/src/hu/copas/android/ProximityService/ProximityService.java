package hu.copas.android.ProximityService;

import android.app.Activity;
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
import android.os.SystemClock;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.SlidingDrawer;

public class ProximityService extends Service {

	final private static int PROXIMITY_SCREEN_OFF_WAKE_LOCK = 32;
	final private static int remoteAudioClass = BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE | BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES | BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO;

	private PowerManager powerManager;
	private PowerManager.WakeLock proximityWakeLock;
	private KeyguardManager keyGuardManager;
	// public Intent serviceIntent;

	//BroadcastReceiver phoneStateChangeReceiver = null;
	//private ScreenOnReceiver screenOnReceiver;
	private ScreenOffReceiver screenOffReceiver;
	private UserPresentReceiver userPresentReceiver;
	private HeadSetPlugReceiver headSetPlugReceiver;
	private BluetoothConnectReceiver bluetoothConnectReceiver;
	private BluetoothDisconnectReceiver bluetoothDisconnectReceiver;
	private boolean receiversRegistered = false;
	private boolean headsetConnected = false;
	private boolean bluetoothConnected = false;
	private boolean event_controlled;
	private boolean ongoing;
	private boolean headset_controlled;
	private boolean no_control;
	private String ec;
	private boolean sendingToSleep;
	
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
		powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		proximityWakeLock = powerManager.newWakeLock(PROXIMITY_SCREEN_OFF_WAKE_LOCK, "PTWLTAG");
		proximityWakeLock.setReferenceCounted(false);
		//screenOnReceiver = new ScreenOnReceiver();
		screenOffReceiver = new ScreenOffReceiver();
		userPresentReceiver = new UserPresentReceiver();
		keyGuardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
		headSetPlugReceiver = new HeadSetPlugReceiver();
		bluetoothConnectReceiver = new BluetoothConnectReceiver();
		bluetoothDisconnectReceiver = new BluetoothDisconnectReceiver();
		sendingToSleep = false;
	}

	@Override
	public void onDestroy() {
		toggleReceivers(false);
		toggleProximityWakeLock(false);
		if (headset_controlled) {
			unregisterReceiver(headSetPlugReceiver);
			unregisterReceiver(bluetoothConnectReceiver);
			unregisterReceiver(bluetoothDisconnectReceiver);
		}
		if (ongoing)
			ProximityServiceHelper.stopForeground(this);
		ProximityServiceHelper.showNotification(this, ProximityServiceHelper.proximityServiceIcon, getString(R.string.service_stopped), getString(R.string.service_running), "Proximity service stopped.", false);
		super.onDestroy();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startid) {
		ongoing = ProximityServiceHelper.settingsReader(this).getBoolean("show_icon", true);
		int control = ProximityServiceHelper.settingsReader(this).getInt("control", 0);
		ec = getResources().obtainTypedArray(R.array.controls).getString(control);
		event_controlled = (control == 0);
		headset_controlled = (control == 1);
		no_control = (control == 2);
		
		if (headset_controlled) {
			registerReceiver(headSetPlugReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
			registerReceiver(bluetoothConnectReceiver, new IntentFilter(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED));
			registerReceiver(bluetoothDisconnectReceiver, new IntentFilter(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED));
		}
		if (ongoing)
			ProximityServiceHelper.startForeground(this, ProximityServiceHelper.proximityServiceIcon, getString(R.string.service_started), getString(R.string.service_running), "Proximity sensing: " + ec);
		else
			ProximityServiceHelper.showNotification(this, ProximityServiceHelper.proximityServiceIcon, getString(R.string.service_started), getString(R.string.service_running), "Proximity sensing: " + ec, false);
		if (no_control)
			toggleProximityWakeLock(true);
		else
			if (event_controlled)
				toggleReceivers(true);
		return START_STICKY;
	}
	
	public void toggleReceivers(boolean start) {
		if (event_controlled)
			if (start) {
				if (!receiversRegistered) {
					//registerReceiver(phoneStateChangeReceiver, new IntentFilter(android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED));
					//registerReceiver(screenOnReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
					registerReceiver(screenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
					registerReceiver(userPresentReceiver, new IntentFilter(Intent.ACTION_USER_PRESENT));
					receiversRegistered = true;
					if (keyGuardManager.inKeyguardRestrictedInputMode())
						toggleProximityWakeLock(true);
				}
			} else {
				if (receiversRegistered) {
					//unregisterReceiver(screenOnReceiver);
					unregisterReceiver(screenOffReceiver);
					unregisterReceiver(userPresentReceiver);
					receiversRegistered = false;
				}
			}
	}
	
	public void toggleProximityWakeLock(boolean on) {
		if (proximityWakeLock != null)
			if (on) {
				if (!proximityWakeLock.isHeld()) {
					Log.i(getString(R.string.app_name), "trying to aquire proximityWakeLock.");
					try {
						proximityWakeLock.acquire();
						Log.i(getString(R.string.app_name), "proximityWakeLock aquired.");
						ProximityServiceHelper.showNotification(this, ProximityServiceHelper.proximityServiceIconOn, getString(R.string.service_started), getString(R.string.service_active), "Proximity sensing activated.", false);
					} catch (Exception e) {
					}
				}
				/*
				else { 
					try {
						sendingToSleep = true;
						try {
							proximityWakeLock.release();
							powerManager.goToSleep(SystemClock.uptimeMillis() + 1000);
							Log.i(getString(R.string.app_name), "Sent to sleep.");
							Thread.sleep(1000);
							proximityWakeLock.acquire();
						} finally {
							sendingToSleep = false;
						}
						if (proximityWakeLock.isHeld())
							Log.i(getString(R.string.app_name), "wakelock re-aquired.");
						else
							Log.i(getString(R.string.app_name), "failed to re-aquire wakelock.");
					} catch (Exception e) {}
				}
				*/
			} else {
				if (proximityWakeLock.isHeld()) {
					int i = 0;
					while (proximityWakeLock.isHeld() && i < 30) {
						try {
							proximityWakeLock.release();
							i++;
							Thread.sleep(100);
						} catch (Exception e) {
							Log.e(getString(R.string.app_name), e.getMessage(), e);
						}
					}
					try {
						if (proximityWakeLock.isHeld())
							Log.e(getString(R.string.app_name), "BUG: proximityWakeLock could not be released after " + String.valueOf(i) + " attempts." );
						else
							Log.i(getString(R.string.app_name), "proximityWakeLock released after " + String.valueOf(i) + " number of attempts." );
						ProximityServiceHelper.showNotification(this, ProximityServiceHelper.proximityServiceIcon, getString(R.string.service_started), getString(R.string.service_active), "Proximity sensing: " + ec, false);
					} catch (Exception e) {
						Log.e(getString(R.string.app_name), e.getMessage(), e);
					}
				}
			}
	}

	public class ScreenOnReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			if (event_controlled) {
				Log.i(getString(R.string.app_name), "Received event: ACTION_SCREEN_ON");
			}
		}

	}
	
	public class ScreenOffReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			if (!sendingToSleep && event_controlled) {
				Log.i(getString(R.string.app_name), "Received event: ACTION_SCREEN_OFF");
				int i = 30;
				try {
					while (i > 0 && !keyGuardManager.inKeyguardRestrictedInputMode()) {
						i--;
						Thread.sleep(1000);
					}
					Thread.sleep(1000);
					if (keyGuardManager.inKeyguardRestrictedInputMode()) {
						toggleProximityWakeLock(true);
						//powerManager.goToSleep(0);
						
					}
					else
						Log.i(getString(R.string.app_name), "Not activated sensing since phone is still not locked.");
				} catch (Exception e) {
					Log.e(getString(R.string.app_name), e.getMessage(), e);
				}
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
				toggleProximityWakeLock(headsetConnected || bluetoothConnected);
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
				toggleProximityWakeLock(headsetConnected || bluetoothConnected);
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
				toggleProximityWakeLock(headsetConnected || bluetoothConnected);
			}
		}
		
	}
	
}
