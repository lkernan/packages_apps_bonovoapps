package com.bonovo.bluetooth;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.widget.Toast;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.content.ContentProviderOperation;
import android.content.OperationApplicationException;
import android.content.ContentUris;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;
import java.util.ArrayList;

@SuppressWarnings("deprecation")
public class BonovoBlueToothService extends Service {
	private static final String TAG = "BonovoBlueToothService";
	private boolean DEB = true;
	private static Context mContext;

	private boolean myBtSwitchStatus = true; // 0:BT power off; 1:BT power on
	private boolean myBtHFPStatus = false;   // 0:Phone function disable; 1:Phone function enable
	private boolean myBtMusicStatus = false;
	private boolean mBtMusicIsEnable = false;
	private boolean mStartComplete = false;
    private boolean mReadName = false;
    private boolean mReadPinCode = false;
	private boolean mIsBindered = false;
    private boolean mMicMuted = false;
    private int mPhoneSignalLevel = 0;
    private int mPhoneBatteryLevel = 0;
    private String mPhoneOperatorName = "";
    private int mSetNameTime = 0;
    private int mSetPinCodeTime = 0;
    private final static int MAX_SET_TIME = 5; 
	private final static String DEF_BT_NAME = "BTHFD";
	private final static String DEF_BT_PIN = "1234";
	private String myBtName = DEF_BT_NAME; // bt name
	private String myBtPinCode = DEF_BT_PIN; // PIN code
	private PhoneState mBtPhoneState = PhoneState.OFFHOOK;
	
	/**
	 * The Phone state. One of the following:
	 * IDLE = no phone activity
	 * RINGING = a phone call is ringing or call waiting.
	 * ACTIVE = a phone is answered.
	 * OFFHOOK = HFP disconnect.
	 */
	public enum PhoneState{
		IDLE, RINGING, DIALING, ACTIVE, OFFHOOK;
	}
	
    class AudioLevel {
        public static final int CODEC_LEVEL_NO_ANALOG = 0;
        public static final int CODEC_LEVEL_BT_MUSIC = 1;
        public static final int CODEC_LEVEL_AV_IN = 2;
        public static final int CODEC_LEVEL_DVB = 3;
        public static final int CODEC_LEVEL_DVD = 4;
        public static final int CODEC_LEVEL_RADIO = 5;
        public static final int CODEC_LEVEL_BT_TEL = 6;
	}

	private static final int MSG_START_HANDFREE = 0;
	private static final int MSG_PHONE_STATE_CHANGE = 1;
	private static final int MSG_HFP_STATE_CHANGE = 2;
	private static final int MSG_AUDIO_STATE_CHANGE = 3;
	private static final int MSG_BT_NAME_INFO = 4;
	private static final int MSG_BT_PINCODE_INFO = 5;
	private static final int MSG_BT_SHUTDOWN = 6;
	private static final int MSG_BT_HFP_DISCONNECT = 7;
	private static final int MSG_BT_A2DP_DISCONNECT = 8;
	private static final int MSG_BT_CHECK_NAME = 9;
	private static final int MSG_BT_CHECK_PINCODE = 10;
    private static final int MSG_BT_SHOW_INFO = 11;
    private static final int MSG_SYNC_PHONE_BOOK_COMPLETE = 12;
    private static final int MSG_SYNC_CONTACTS_READ_COUNT = 13;
    private static final int MSG_SYNC_CONTACTS_WRITE_DATABASE = 14;
    private static final int MSG_SYNC_CONTACTS_TIMEOUT = 15;
    private static final int MSG_SYNC_CONTACTS_NOTSUPPORT = 16;
    private static final int MSG_ACTIVE_AUDIO = 20;
    private static final int MSG_RECOVERY_AUDIO = 21;
    private static final int MSG_STOP_MUSIC = 22;
    private static final int MSG_BT_MIC_STATE_CHANGE = 23;
    private static final int MSG_SEND_COMMANDER_ERROR = 30;
    private static final int MSG_PHONE_NETWORKNAME = 32;
    private static final int MSG_PHONE_BATTERYLEVEL = 33;
    private static final int MSG_PHONE_SIGNALLEVEL = 34;
    private static final int MSG_PHONE_NEW_CALL_WAITING = 35;
    private static final int MSG_PHONE_HELD_ACTIVE_SWITCHED_TO_CALL_WAITING = 36;
    private static final int MSG_PHONE_CONFERENCE_CALL = 37;
    private static final int MSG_PHONE_HUNG_UP_INACTIVE = 38;
    private static final int MSG_PHONE_HUNG_UP_ACTIVE_SWITCHED_TO_CALL_WAITING = 39;
	private static final int DELAY_TIME_CHECKPINCODE = 2000;
	private static final int DELAY_TIME_DISCONNECT = 1000;
	private static final int DELAY_TIME_SHUTDOWN = 5000;
    private static final int DELAY_TIME_STOP_MUSIC = 5 * 1000;
    private static final int DELAY_TIMEOUT_SYNC_CONTACTS = 60 * 1000;
    private static final int DELAY_TIMEOUT_WAIT_USER = 30 * 1000;
    private static final int MAX_COUNT_CONTACTS_PRE_SYNC = 500;
    private static final int MAX_PAUSE_MUSIC_TIMES = 5;
    private int mMusicStopTimes = 0;
	private String mCurrNumber = "";
	private long mAnswerTimer = -1;
	private boolean mIsBtWillShutDown = false;
	private final static String ACTION_BT_POWERON = "android.intent.action.BONOVO_BT_POWERON";
	private final static String ACTION_BT_POWEROFF = "android.intent.action.BONOVO_BT_POWEROFF";
	
	private final static String ACTION_CALL_DIAL = "android.intent.action.BONOVO_CALL_DIAL";
	private final static String ACTION_CALL_ANSWER = "android.intent.action.BONOVO_CALL_ANSWER";
	private final static String ACTION_CALL_HANGUP = "android.intent.action.BONOVO_CALL_HANGUP";
	private final static String ACTION_CALL_MUTE = "android.intent.action.BONOVO_CALL_MUTE";
	private final static String ACTION_CALL_SWITCHAUDIO = "android.intent.action.BONOVO_CALL_SWITCHAUDIO";
	private final static String ACTION_CALL_VOLUMEUP = "android.intent.action.BONOVO_CALL_VOLUMEUP";
	private final static String ACTION_CALL_VOLUMEDOWN = "android.intent.action.BONOVO_CALL_VOLUMEDOWN";
	private final static String ACTION_CALL_REJECTCALLWAITING = "android.intent.action.BONOVO_CALL_REJECTCALLWAITING";
	private final static String ACTION_CALL_ENDANDACCEPTCALLWAITING = "android.intent.action.BONOVO_CALL_ENDANDACCEPTCALLWAITING";
	private final static String ACTION_CALL_HOLDANDACCEPTCALLWAITING = "android.intent.action.BONOVO_CALL_HOLDANDACCEPTCALLWAITING";
	private final static String ACTION_CALL_MAKECONFERENCECALL = "android.intent.action.BONOVO_CALL_MAKECONFERENCECALL";
	private final static String ACTION_CALL_VOICEDIAL = "android.intent.action.BONOVO_CALL_VOICEDIAL";
	private final static String ACTION_CALL_VOICEDIAL_CANCEL = "android.intent.action.BONOVO_CALL_VOICEDIAL_CANCEL";
	
	private final static String ACTION_MUSIC_STOP = "android.intent.action.BONOVO_MUSIC_STOP";
	private final static String ACTION_MUSIC_PLAY = "android.intent.action.BONOVO_MUSIC_PLAY";
	private final static String ACTION_MUSIC_NEXT_TRACK = "android.intent.action.BONOVO_MUSIC_NEXT_TRACK";
	private final static String ACTION_MUSIC_PREV_TRACK = "android.intent.action.BONOVO_MUSIC_PREV_TRACK";
	
    // onKeyEvent
    private final static String ACTION_KEY_BT = "android.intent.action.BONOVO_BT";
    private final static String ACTION_KEY_BT_ANSWER = "android.intent.action.BONOVO_BT_ANSWER";
    private final static String ACTION_KEY_BT_HANG_UP = "android.intent.action.BONOVO_BT_HANG_UP";
    private final static String ACTION_KEY_BT_ANSWER_HANG = "android.intent.action.BONOVO_BT_ANSWER_HANG";
    private final static String ACTION_KEY_BT_SWITCH_AUDIO = "android.intent.action.BONOVO_BT_SWITCH_AUDIO";

    public PairedDevice[] pairedDevices = new PairedDevice[7];  // HFP device can be paired with up to 8 devices 
    
	private native void BonovoBlueToothInit();

	private native void BonovoBlueToothDestroy();

	private native void BonovoBlueToothSet(int cmd);

	private native int BonovoBlueToothPower(int status);
	private native int BonovoBlueToothActiveAudio(int level);
	private native int BonovoBlueToothRecoveryAudio(int level);

	private native void BonovoBlueToothSetWithParam(int cmd, String param);
	
	private ServiceBinder serviceBinder = new ServiceBinder();

	public class ServiceBinder extends Binder {

		public BonovoBlueToothService getService() {
			mIsBindered = true;
			return BonovoBlueToothService.this;
		}
	}

	static {
		System.loadLibrary("bonovobluetooth");
	}
	
	@Override
	public void onRebind(Intent intent){
		mIsBindered = true;
		super.onRebind(intent);
	}
	
	@Override
	public boolean onUnbind(Intent intent){
		mIsBindered = false;
		return super.onUnbind(intent);
	}
	
	private IntentFilter getIntentFilter() {
		IntentFilter myIntentFilter = new IntentFilter();
        myIntentFilter.addAction("android.intent.action.ACTION_SHUTDOWN");
		myIntentFilter.addAction("android.intent.action.BONOVO_SLEEP_KEY");
		myIntentFilter.addAction("android.intent.action.BONOVO_WAKEUP_KEY");
		myIntentFilter.addAction(ACTION_CALL_DIAL);
		myIntentFilter.addAction(ACTION_CALL_ANSWER);
		myIntentFilter.addAction(ACTION_CALL_HANGUP);
		myIntentFilter.addAction(ACTION_CALL_MUTE);
		myIntentFilter.addAction(ACTION_CALL_SWITCHAUDIO);
		myIntentFilter.addAction(ACTION_CALL_VOLUMEUP);
		myIntentFilter.addAction(ACTION_CALL_VOLUMEDOWN);
		myIntentFilter.addAction(ACTION_KEY_BT);
		myIntentFilter.addAction(ACTION_KEY_BT_ANSWER);
		myIntentFilter.addAction(ACTION_KEY_BT_HANG_UP);
		myIntentFilter.addAction(ACTION_KEY_BT_ANSWER_HANG);
		myIntentFilter.addAction(ACTION_KEY_BT_SWITCH_AUDIO);
		myIntentFilter.addAction(ACTION_CALL_REJECTCALLWAITING);
		myIntentFilter.addAction(ACTION_CALL_ENDANDACCEPTCALLWAITING);
		myIntentFilter.addAction(ACTION_CALL_HOLDANDACCEPTCALLWAITING);
		myIntentFilter.addAction(ACTION_CALL_MAKECONFERENCECALL);
		myIntentFilter.addAction(ACTION_CALL_VOICEDIAL);
		myIntentFilter.addAction(ACTION_CALL_VOICEDIAL_CANCEL);
		myIntentFilter.addAction(ACTION_MUSIC_NEXT_TRACK);
		myIntentFilter.addAction(ACTION_MUSIC_PREV_TRACK);
		myIntentFilter.addAction(ACTION_MUSIC_PLAY);
		myIntentFilter.addAction(ACTION_MUSIC_STOP);
		return myIntentFilter;
	};
	
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if(DEB) Log.d(TAG, "====== action:" + action 
               + " BroadcastReceiver myBtSwitchStatus = " + myBtSwitchStatus);
			if(action.equals("android.intent.action.BONOVO_SLEEP_KEY")
               || action.equals("android.intent.action.ACTION_SHUTDOWN")){
				if(myBtSwitchStatus){
					myBtSwitchStatus = false;
					mIsBtWillShutDown = true;
	                mIsBindered = false;
                    mStartComplete = false;
					mHandler.sendEmptyMessage(MSG_BT_HFP_DISCONNECT);
				}
			}else if(action.equals("android.intent.action.BONOVO_WAKEUP_KEY")){
				mHandler.removeMessages(MSG_BT_SHUTDOWN);
				mHandler.removeMessages(MSG_BT_A2DP_DISCONNECT);
				mHandler.removeMessages(MSG_BT_HFP_DISCONNECT);
				mIsBtWillShutDown = false;
				SharedPreferences settings = getSharedPreferences("com.bonovo.bluetooth", MODE_PRIVATE);
				myBtSwitchStatus = settings.getBoolean("myBtSwitchStatus", false);
				if (!myBtSwitchStatus) {
					setBtHFPStatus(false);
					myBtMusicStatus = false;
				}else{
				    setBtSwitchStatus(myBtSwitchStatus);
				    //BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_CC);
				}
			}else if(action.equals(ACTION_CALL_DIAL)){
			    if(getBtHFPStatus()){
				    String number = intent.getStringExtra(BonovoBlueToothData.PHONE_NUMBER);
				    BlueToothPhoneDial(number);
			    }else{
			        Log.e(TAG, "HFP Not Connect!  intent:" + action);
                    showToast(getString(R.string.description_phone_disable));
			    }
			}else if(action.equals(ACTION_CALL_ANSWER)){
			    if(getPhoneState() == PhoneState.RINGING){
				    BlueToothPhoneAnswer();
			    }
			}else if(action.equals(ACTION_CALL_HANGUP)){
			    PhoneState status = getPhoneState();
				if(status == PhoneState.RINGING){
					BlueToothPhoneRejectCall();
				}else if((status == PhoneState.DIALING) || (status == PhoneState.ACTIVE)){
					BlueToothPhoneHangup();
				}
			}else if(action.equals(ACTION_CALL_MUTE)){
			    if(getBtHFPStatus()){
				    BlueToothPhoneMute();
			    }else{
			        Log.e(TAG, "HFP Not Connect!  intent:" + action);
                    showToast(getString(R.string.description_phone_disable));
			    }
			}else if(action.equals(ACTION_CALL_SWITCHAUDIO)
			    || action.equals(ACTION_KEY_BT_SWITCH_AUDIO)){
			    if(getBtHFPStatus()){
				    BlueToothSwitchAudio();
			    }else{
			        Log.e(TAG, "HFP Not Connect!  intent:" + action);
                    showToast(getString(R.string.description_phone_disable));
			    }
			}else if(action.equals(ACTION_CALL_VOLUMEUP)){
			    if(getBtHFPStatus()){
				    BlueToothPhoneVolumeUp();
			    }else{
			        Log.e(TAG, "HFP Not Connect!  intent:" + action);
                    showToast(getString(R.string.description_phone_disable));
			    }
			}else if(action.equals(ACTION_CALL_VOLUMEDOWN)){
                if(getBtHFPStatus()){
                    BlueToothPhoneVolumeDown();
                }else{
                    Log.e(TAG, "HFP Not Connect!  intent:" + action);
                    showToast(getString(R.string.description_phone_disable));
                }
			}else if(action.equals(ACTION_BT_POWERON)){
				setBtSwitchStatus(true);
			}else if(action.equals(ACTION_BT_POWEROFF)){
				setBtSwitchStatus(false);
			}else if(action.equals(ACTION_KEY_BT)){
			    if(getBtHFPStatus()){
                    Message msg = mHandler.obtainMessage(MSG_START_HANDFREE);
                    mHandler.sendMessage(msg);
			    }else{
			        Log.e(TAG, "HFP Not Connect!  intent:" + action);
			        showToast(getString(R.string.description_phone_disable));
			    }
			}else if(action.equals(ACTION_KEY_BT_ANSWER)){
			    if(getPhoneState() == PhoneState.RINGING){
                    BlueToothPhoneAnswer();
			    }
			}else if(action.equals(ACTION_KEY_BT_HANG_UP)){
			    if(getPhoneState() == PhoneState.RINGING){
                    BlueToothPhoneRejectCall();
			    }else if((getPhoneState() == PhoneState.DIALING)
                    || (getPhoneState() == PhoneState.ACTIVE)){
                    BlueToothPhoneHangup();
			    }
			}else if(action.equals(ACTION_KEY_BT_ANSWER_HANG)){
			    if(getPhoneState() == PhoneState.RINGING){
                    BlueToothPhoneAnswer();
			    }else if((getPhoneState() == PhoneState.DIALING)
                    || (getPhoneState() == PhoneState.ACTIVE)){
                    BlueToothPhoneHangup();
			    }
			}else if(action.equals(ACTION_CALL_MAKECONFERENCECALL)){
			    if(getPhoneState() == PhoneState.ACTIVE){
				    BlueToothPhoneConferenceCalls();
			    }
			}else if(action.equals(ACTION_CALL_HOLDANDACCEPTCALLWAITING)){
			    if(getPhoneState() == PhoneState.ACTIVE){
				    BlueToothPhoneHoldAndSwitchToWaitingCall();
			    }
			}else if(action.equals(ACTION_CALL_ENDANDACCEPTCALLWAITING)){
			    if(getPhoneState() == PhoneState.ACTIVE){
				    BlueToothPhoneEndAndSwitchToWaitingCall();
			    }
			}else if(action.equals(ACTION_CALL_REJECTCALLWAITING)){
			    if(getPhoneState() == PhoneState.ACTIVE){
				    BlueToothPhoneRejectWaitingCall();
			    }
			}else if(action.equals(ACTION_CALL_VOICEDIAL)){
				BlueToothPhoneVoiceDial();
			}else if(action.equals(ACTION_CALL_VOICEDIAL_CANCEL)){
				BlueToothPhoneVoiceDialCancel();
			}else if(action.equals(ACTION_MUSIC_PLAY)){
				BlueToothMusicPlay();
			}else if(action.equals(ACTION_MUSIC_STOP)){
				BlueToothMusicStop();
			}else if(action.equals(ACTION_MUSIC_NEXT_TRACK)){
				BlueToothMusicNext();
			}else if(action.equals(ACTION_MUSIC_PREV_TRACK)){
				BlueToothMusicPre();		
			}

        }
		
	};
	
	private void wakeUpAndUnlockIfNeed(){
		PowerManager mPm=(PowerManager) getSystemService(Context.POWER_SERVICE);
		if(!mPm.isScreenOn()){
			PowerManager.WakeLock mWl = mPm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "BonovoBt");
			mWl.acquire();
			mWl.release();
		}
		
		KeyguardManager mKm = (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
		if(mKm.inKeyguardRestrictedInputMode()){
			KeyguardLock mKl = mKm.newKeyguardLock("unLock");
			mKl.disableKeyguard();
		}
	}

    private void showToast(final String info){
        Message msg = mHandler.obtainMessage(MSG_BT_SHOW_INFO, info);
        mHandler.sendMessage(msg);
    }
	
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			int what = msg.what;
			switch (what) {
			case MSG_START_HANDFREE:{
				wakeUpAndUnlockIfNeed();
				//Intent intent = new Intent(BonovoBlueToothService.this, BonovoBluetoothHandfree.class);
				//intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				Intent intent = new Intent(Intent.ACTION_MAIN, null);
				intent.addCategory("android.intent.category.APP_BT_PHONE");
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
				intent.putExtra(BonovoBlueToothData.PHONE_NUMBER, (String)msg.obj);
				startActivity(intent);

				Intent i2 = new Intent(BonovoBlueToothData.ACTION_PHONE_STATE_CHANGED);
				i2.putExtra(BonovoBlueToothData.PHONE_STATE, getPhoneState().toString());
				mContext.sendOrderedBroadcast(i2, null);
				break;
			}
			case MSG_PHONE_STATE_CHANGE:{
				Intent i = new Intent(BonovoBlueToothData.ACTION_PHONE_STATE_CHANGED);
				if(msg.obj != null){
//					mCurrNumber = (String)msg.obj;
					setCurrentNumber((String)msg.obj);
					i.putExtra(BonovoBlueToothData.PHONE_NUMBER, (String)msg.obj);
				}
				i.putExtra(BonovoBlueToothData.PHONE_STATE, getPhoneState().toString());
				
				mContext.sendOrderedBroadcast(i, null);
			}
			break;
			case MSG_HFP_STATE_CHANGE:{
//				Bundle bundle = new Bundle();
				Intent intent1 = new Intent(BonovoBlueToothData.ACTION_DATA_IAIB_CHANGED);
				intent1.putExtra(BonovoBlueToothData.HFP_STATUS, getBtHFPStatus());
//				intent1.putExtras(bundle);
				mContext.sendBroadcast(intent1);
			}
			break;
			case MSG_AUDIO_STATE_CHANGE:{
//				Bundle bundle = new Bundle();
				Intent intent3 = new Intent(BonovoBlueToothData.ACTION_DATA_MAMB_CHANGED);
				intent3.putExtra(BonovoBlueToothData.A2DP_STATUS, getMusicStatus());
//				intent3.putExtras(bundle);
				mContext.sendBroadcast(intent3);
			}
			break;
			case MSG_BT_SHUTDOWN:{
				if(DEB) Log.d(TAG, "====== Handler  shutdown 111");
				PhoneState tempPhoneStatus = getPhoneState();
				if(tempPhoneStatus != PhoneState.OFFHOOK && tempPhoneStatus != PhoneState.IDLE){
					setBtHFPStatus(false);
				}
                if(getMusicStatus()){
                    setMusicStatus(false);
                }
	            mStartComplete = false;
				BonovoBlueToothPower(0);
			}
				break;
			case MSG_BT_HFP_DISCONNECT:{
				BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_CD);
				this.sendEmptyMessageDelayed(MSG_BT_A2DP_DISCONNECT, DELAY_TIME_DISCONNECT);
			}
				break;
			case MSG_BT_A2DP_DISCONNECT:{
				BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_MJ);
				this.sendEmptyMessageDelayed(MSG_BT_SHUTDOWN, DELAY_TIME_SHUTDOWN);
			}
				break;
            case MSG_BT_SHOW_INFO:
                String info = (String)msg.obj;
                if(info != null)
                    Toast.makeText(mContext, info, Toast.LENGTH_SHORT).show();
                break;
            case MSG_STOP_MUSIC:{
                if((mMusicStopTimes < MAX_PAUSE_MUSIC_TIMES) 
                    && !getMusicServiceEnable() && getMusicStatus()){
                    //BlueToothMusicPause();
                    BlueToothMusicStop();
                    mMusicStopTimes++;
                    this.sendEmptyMessageDelayed(MSG_STOP_MUSIC, DELAY_TIME_STOP_MUSIC);
                }
                break;
            }
            case MSG_ACTIVE_AUDIO:{
                int level = msg.arg1;
                if((level == AudioLevel.CODEC_LEVEL_BT_MUSIC)
        		    && !getMusicServiceEnable()){
        			break;
        		}else{
        			BonovoBlueToothActiveAudio(level);
        		}
                break;
            }
            case MSG_RECOVERY_AUDIO:{
                int level = msg.arg1;
                BonovoBlueToothRecoveryAudio(level);
        		break;
            }
            case MSG_SEND_COMMANDER_ERROR:{
                Intent intent = new Intent(BonovoBlueToothData.ACTION_SEND_COMMANDER_ERROR);
                sendBroadcast(intent);
                break;
            }
			default:
				break;
			}
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();
		mContext = getApplicationContext();
        
		getBtName();
		getBtPinCode();
		BonovoBlueToothInit();
//		BonovoBlueToothPower(0);
		SharedPreferences settings = getSharedPreferences("com.bonovo.bluetooth", MODE_PRIVATE);
		myBtSwitchStatus = settings.getBoolean("myBtSwitchStatus", false);
		if(DEB) Log.d(TAG, "++++++++ onCreate myBtSwitchStatus = " + myBtSwitchStatus);
		mBtMusicIsEnable = settings.getBoolean("mBtMusicIsEnable", false);
		if (!myBtSwitchStatus) {
			setBtHFPStatus(false);
			myBtMusicStatus = false;
		}
		setBtSwitchStatus(myBtSwitchStatus);
		registerReceiver(mBroadcastReceiver, getIntentFilter());

		// Prepare the list of paired devices
		for(int i=0; i<pairedDevices.length; i++){
            pairedDevices[i] = new PairedDevice(i, "", "");
        }
		
		Log.v(TAG, "About to get paired device list");
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return serviceBinder;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		BonovoBlueToothDestroy();
		unregisterReceiver(mBroadcastReceiver);
	}

	public int recoveryAudio(int level){
        Message msg = mHandler.obtainMessage(MSG_RECOVERY_AUDIO, level, 0);
        mHandler.sendMessage(msg);
        return 0;
	}
	
	public int activeAudio(int level){
		Message msg = mHandler.obtainMessage(MSG_ACTIVE_AUDIO, level, 0);
        mHandler.sendMessage(msg);
		return 0;
	}
	
	//
	public void BlueToothSetCmd(int cmd) {
		BonovoBlueToothSet(cmd);
	}
	
	// build pbap link
	public void BlueToothBuildPbapContacts() {
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_QA);
	}
	
	// down load phone numbers through pbap
	public void BlueToothPbapContacts() {
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_QB);
	}
		
	// read all records
	public void BlueToothDownloadContacts() {
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_PF);
	}
	
	// reset bt module
	public void BlueToothReset() {
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_CZ);
	}
	
	// read pairing list
	public void BlueToothGetPairedList() {
		Log.v(TAG, "Requesting paired device list");
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_MX);
	}
	
	// Returns the details of the device listed at specified position in pairing list
	public PairedDevice BlueToothGetPairedDeviceDetails(Integer position) {
		return pairedDevices[position];
	}
		
	// clear pairing records 
	public void BlueToothClear() {
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_CV);
	}
	
	// Music play
	public void BlueToothMusicPlay() {
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_MA);
	}

	// Music pause
	public void BlueToothMusicPause() {
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_MA);
	}

	// music stop
	public void BlueToothMusicStop() {
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_MC);
	}

	// music previous
	public void BlueToothMusicPre() {
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_ME);
	}

	// music next
	public void BlueToothMusicNext() {
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_MD);
	}
	
	/**
	 * switch audio channel
	 */
	public void BlueToothSwitchAudio(){
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_CO);
	}

	/**
	 * add by bonovo zbiao for bluetooth phone
	 * @param number telephone no.
	 */
	public void BlueToothPhoneDial(String number) {
		BonovoBlueToothSetWithParam(BonovoBlueToothRequestCmd.CMD_SOLICATED_CW, number);
	}
	
	/**
	 * answer the phone
	 */
	public void BlueToothPhoneAnswer(){
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_CE);
	}
	
	/**
	 * rejecting a call
	 */
	public void BlueToothPhoneRejectCall(){
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_CF);
	}
	
	/**
	 * Hangup the phone
	 */
	public void BlueToothPhoneHangup(){
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_CG);
	}
	
	/**
	 * HFP Volume up
	 */
	public void BlueToothPhoneVolumeUp(){
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_CK);
	}
	
	/**
	 * HFP Volume down
	 */
	public void BlueToothPhoneVolumeDown(){
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_CL);
	}
	
	/**
	 * HFP Mute
	 */
	public void BlueToothPhoneMute(){
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_CM);
	}
	
	// Returns the current mute state of the BT Mic
	public boolean BlueToothMicrophoneState() {
		return mMicMuted;
	}
	
	/**
	 * DTMF Dial
	 */
	public void BlueToothPhoneDTMF(String number){
		BonovoBlueToothSetWithParam(BonovoBlueToothRequestCmd.CMD_SOLICATED_CX, number);
	}
	
	/**
	 * HFP Reject call waiting
	 */
	public void BlueToothPhoneRejectWaitingCall() {
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_CQ);
	}

	/**
	 * HFP End current call and switch to call waiting
	 */
	public void BlueToothPhoneEndAndSwitchToWaitingCall() {
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_CR);
	}
	
	/**
	 * HFP Hold current call and switch to call waiting
	 */
	public void BlueToothPhoneHoldAndSwitchToWaitingCall() {
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_CS);
	}
	
	/**
	 * HFP Merge active and waiting calls into conference
	 */
	public void BlueToothPhoneConferenceCalls() {
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_CT);
	}
	
	/**
	 * Start voice dial, activates voice commands on the connected phone (siri / ok google)
	 */
	public void BlueToothPhoneVoiceDial() {
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_CI);
	}
	
	/**
	 * Cancel the voice dial command
	 */
	public void BlueToothPhoneVoiceDialCancel() {
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_CJ);
	}

	/**
	 * Enter pairing mode
	 */
	public void BlueToothEnterPairingMode() {
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_CA);
	}

	/**
	 * Cancel pairing mode
	 */
	public void BlueToothCancelPairingMode() {
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_CB);
	}

	/**
	 * Accept pairing request
	 */
	public void BlueToothAcceptPairingRequest() {
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_QJ);
	}
	
	/**
	 * Reject pairing request
	 */
	public void BlueToothRejectPairingRequest() {
		BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_QK);
	}
	
	/**
	 * Set or check bt's name
	 * @param name  if name is null,check bt's name. Otherwise set name.
	 */
	public void BlueToothSetOrCheckName(String name){
		if(name != null){
			BonovoBlueToothSetWithParam(BonovoBlueToothRequestCmd.CMD_SOLICATED_MM, name);
		}else{
		    mReadName = true;
			BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_MM);
		}
	}
	
	/**
	 * Set or check bt's paring code
	 * @param pin  paring code. if pin is null, check bt's pin.Otherwise set pin.
	 */
	public void BlueToothSetOrCheckPin(String pin){
		if(pin != null){
			BonovoBlueToothSetWithParam(BonovoBlueToothRequestCmd.CMD_SOLICATED_MN, pin);
		}else{
		    mReadPinCode = true;
			BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_MN);
		}
	}

	/*
	 * set mBtMusicIsEnable
	 */
	public void setMusicServiceEnable(boolean offOn){
		mBtMusicIsEnable = offOn;
		SharedPreferences settings = getSharedPreferences("com.bonovo.bluetooth", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("mBtMusicIsEnable", mBtMusicIsEnable);
		editor.commit();
        if(getMusicStatus()){
            if(mBtMusicIsEnable){
                activeAudio(AudioLevel.CODEC_LEVEL_BT_MUSIC);
            }else{
                recoveryAudio(AudioLevel.CODEC_LEVEL_BT_MUSIC);
                mMusicStopTimes = 0;
                mHandler.sendEmptyMessage(MSG_STOP_MUSIC);
            }
        }
	}

	/*
	 * get mBtMusicIsEnable
	 */
	public boolean getMusicServiceEnable(){
		return mBtMusicIsEnable;
	}

	/*
	 * get bluetooth name
	 */
	public String getBtName(){
		return myBtName;
	}
	
	/*
	 * set bluetooth name
	 */
	public void setBtName(String name){
		myBtName = name;
	}
	
	/*
	 * get bluetooth pin code
	 */
	public String getBtPinCode(){
		return myBtPinCode;
	}
	
	/*
	 * set bluetooth pin code
	 */
	public void setBtPinCod(String pin){
		myBtPinCode = pin;
	}

	/**
	 *  BT Power On/Off
	 */
	public void setBtSwitchStatus(boolean enable) {
		myBtSwitchStatus = enable;
		SharedPreferences settings = getSharedPreferences("com.bonovo.bluetooth", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("myBtSwitchStatus", myBtSwitchStatus);
		editor.commit();
        mStartComplete = false;
		if(!enable){  // do someting before poweroff.
			setBtHFPStatus(enable);
			mHandler.sendEmptyMessage(MSG_BT_HFP_DISCONNECT);
			//BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_MC);
			//BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_CD);
			//BonovoBlueToothSet(BonovoBlueToothRequestCmd.CMD_SOLICATED_CA);
		}else{
		    mHandler.removeMessages(MSG_BT_SHUTDOWN);
            mHandler.removeMessages(MSG_BT_A2DP_DISCONNECT);
            mHandler.removeMessages(MSG_BT_HFP_DISCONNECT);
			BonovoBlueToothPower(1);
		}
		//if(enable){
		//	BlueToothSetOrCheckName(myBtName);
		//	BlueToothSetOrCheckPin(myBtPinCode);
		//}
	}

	public boolean getBtSwitchStatus() {
		return myBtSwitchStatus;
	}
	
	/**
	 * get Bt phone state
	 * @return
	 */
	public PhoneState getPhoneState(){
		return mBtPhoneState;
	}
	
	/**
	 * set Bt phone state
	 * @param state
	 */
	public void setPhoneState(PhoneState state){
		mBtPhoneState = state;
		if(mBtPhoneState == PhoneState.ACTIVE){
			setAnswerTime(SystemClock.elapsedRealtime());
		}else if(mBtPhoneState == PhoneState.IDLE || mBtPhoneState == PhoneState.OFFHOOK){
			clearAnswerTime();
		}
	}

	// set bt HFP status
	public void setBtHFPStatus(boolean enable) {
		myBtHFPStatus = enable;
		if(enable){
			setPhoneState(PhoneState.IDLE);
		}else{
			setPhoneState(PhoneState.OFFHOOK);
		}
	}

	public boolean getBtHFPStatus() {
		return myBtHFPStatus;
	}

	// set bt music status
	public void setMusicStatus(boolean enable) {
	    myBtMusicStatus = enable;
	}

	public boolean getMusicStatus() {
		return myBtMusicStatus;
	}
	
	public String getCurrentNumber() {
		return mCurrNumber;
	}
	
	private String cleanInfo(String info){
		byte[] temp = info.getBytes();
		int i = 0;
		for(i=0; i<info.length(); i++){
			if((temp[i] == '\0') || (temp[i] == '\r') || (temp[i] == '\n')){
				break;
			}
		}
		String subInfo = info.substring(0, i);
		return subInfo;
	}
	
	public void setCurrentNumber(String number) {
		mCurrNumber = cleanInfo(number);
	}
	
	public long getAnswerTime(){
		return mAnswerTimer;
	}
	
	public void clearAnswerTime(){
		mAnswerTimer = -1;
	}
	
	private void setAnswerTime(long time){
		mAnswerTimer = time;
	}

	public void BlueToothCallback(String param) {
		if(DEB) Log.d(TAG, "BlueToothCallback value=" + param);

		if(param.startsWith("IS")){
			// Device initialization is completed.  We are ready to go
			mStartComplete = true;
			
			//   Call a bunch of functions to get info from the HFP device, we'll get it's responses in various callbacks below
			BlueToothSetOrCheckName("");		// Read the device Name
			BlueToothSetOrCheckPin("");			// Read the device Pin Code
			
			BlueToothGetPairedList();			// Read the list of paired devices
			
		}else if(param.startsWith("IA")){
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_IA   myBtHFPStatus:" + myBtHFPStatus);
			
            if(getPhoneState() != PhoneState.IDLE){
				setPhoneState(PhoneState.IDLE);
				setCurrentNumber("");
				Message msg = mHandler.obtainMessage(MSG_PHONE_STATE_CHANGE);
				mHandler.sendMessage(msg);
            }
			setBtHFPStatus(false);
			recoveryAudio(AudioLevel.CODEC_LEVEL_BT_TEL);
			Message msg = mHandler.obtainMessage(MSG_HFP_STATE_CHANGE);
			mHandler.sendMessage(msg);
            
			if(mIsBtWillShutDown){
				mHandler.removeMessages(MSG_BT_SHUTDOWN);
				mHandler.sendEmptyMessage(MSG_BT_SHUTDOWN);
				mIsBtWillShutDown = false;
			}
			
		}else if(param.startsWith("IB")){
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_IB   myBtHFPStatus:" + myBtHFPStatus);
			setBtHFPStatus(true);
			Message msg = mHandler.obtainMessage(MSG_HFP_STATE_CHANGE);
			mHandler.sendMessage(msg);
			
		} else if(param.startsWith("MA")){
            if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_MA");
			setMusicStatus(false);
            mHandler.removeMessages(MSG_STOP_MUSIC);
			recoveryAudio(AudioLevel.CODEC_LEVEL_BT_MUSIC);
			Message msg = mHandler.obtainMessage(MSG_AUDIO_STATE_CHANGE);
			mHandler.sendMessage(msg);
			
		}else if(param.startsWith("MB")){
            if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_MB");
            activeAudio(AudioLevel.CODEC_LEVEL_BT_MUSIC);
			setMusicStatus(true);
			Message msg = mHandler.obtainMessage(MSG_AUDIO_STATE_CHANGE);
			mHandler.sendMessage(msg);
			
		}else if(param.startsWith("PA")){
            Log.d(TAG, "Callback -->CMD_UNSOLICATED_PA  param:" + param);
            if(param.startsWith("0\r\n")){
                Message msg = mHandler.obtainMessage(MSG_SYNC_CONTACTS_NOTSUPPORT);
    			mHandler.sendMessage(msg);
            }else{
                mHandler.removeMessages(MSG_SYNC_CONTACTS_TIMEOUT);
                mHandler.sendEmptyMessageDelayed(MSG_SYNC_CONTACTS_TIMEOUT, DELAY_TIMEOUT_SYNC_CONTACTS);
            }
            
		}else if(param.startsWith("PC")){
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_PC");
            Message msg = mHandler.obtainMessage(MSG_SYNC_CONTACTS_WRITE_DATABASE);
			mHandler.sendMessage(msg);
			
		}else if(param.startsWith("QA")){
			Log.v(TAG, "(QA) Device pairing request. " + param);
			String MACAddr = param.substring(2,13);
			String Pin = param.substring(13);
			Log.v(TAG, "Device pairing request. PIN: " + Pin);
			
		}else if(param.startsWith("QB")){
			Log.v(TAG, "Device pairing success. " + param);	
			
		}else if(param.startsWith("QC")){
			Log.v(TAG, "Device pairing failed. " + param);
			
		}else if(param.startsWith("MX")){
			Log.v(TAG, "Callback -->MX: " + param);
			
			Integer listPosition = Integer.parseInt(param.substring(2,3));
			String MACAddr = param.substring(3,15);
			String DeviceName = param.substring(15, param.length() - 2);
			
			// BT device positions are 1 - 8, pairedDevices array starts at 0
			pairedDevices[listPosition - 1] = new PairedDevice(listPosition, MACAddr, DeviceName);
	
		}else if(param.startsWith("CV")){
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_CV");	
			
		}else if(param.startsWith("IC")){
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_IC  getPhoneState():" + getPhoneState());
            activeAudio(AudioLevel.CODEC_LEVEL_BT_TEL);
            
            // TODO: cancel any pending hangup messages here
            
			if(getPhoneState() == PhoneState.IDLE){
				setPhoneState(PhoneState.DIALING);
				Message msg = mHandler.obtainMessage(MSG_START_HANDFREE);
				mHandler.sendMessage(msg);
			}
			
		}else if(param.startsWith("ID")){
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_ID param:" + param + "  mBtPhoneState: " + mBtPhoneState);
            activeAudio(AudioLevel.CODEC_LEVEL_BT_TEL);
            
			if(getPhoneState() == PhoneState.IDLE){
				setPhoneState(PhoneState.RINGING);
				Message msg = mHandler.obtainMessage(MSG_START_HANDFREE);
				msg.obj = param.substring(2);
				mHandler.sendMessage(msg);
			}else {
				Message msg = mHandler.obtainMessage(MSG_PHONE_STATE_CHANGE);
				msg.obj = param.substring(2);
				mHandler.sendMessage(msg);
			}
			
		}else if(param.startsWith("IF")){
                recoveryAudio(AudioLevel.CODEC_LEVEL_BT_TEL);
				if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_IF   getPhoneState():" + getPhoneState());
				setPhoneState(PhoneState.IDLE);
				setCurrentNumber("");
				Message msg = mHandler.obtainMessage(MSG_PHONE_STATE_CHANGE);
				mHandler.sendMessage(msg);
				
		}else if(param.startsWith("IG")){
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_IG   getPhoneState():" + getPhoneState());
            activeAudio(AudioLevel.CODEC_LEVEL_BT_TEL);
            
			if(getPhoneState() == PhoneState.IDLE){
				setPhoneState(PhoneState.ACTIVE);
				Message msg = mHandler.obtainMessage(MSG_START_HANDFREE, " ");
				mHandler.sendMessage(msg);
			}else if(getPhoneState() == PhoneState.DIALING || getPhoneState() == PhoneState.RINGING){
				setPhoneState(PhoneState.ACTIVE);
				Message msg = mHandler.obtainMessage(MSG_PHONE_STATE_CHANGE);
				mHandler.sendMessage(msg);
			}

		}else if(param.startsWith("II")){
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_II  mStartComplete : " + mStartComplete);	
			
		}else if(param.startsWith("IJ")){
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_IJ");
			
		}else if(param.startsWith("IR")){
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_IR param:" + param.substring(2));
			activeAudio(AudioLevel.CODEC_LEVEL_BT_TEL);
			if((getPhoneState() != PhoneState.ACTIVE)&&(getPhoneState() != PhoneState.DIALING)){
				setPhoneState(PhoneState.DIALING);
			}else if(getPhoneState() == PhoneState.IDLE){
				setPhoneState(PhoneState.ACTIVE);
				Message msg = mHandler.obtainMessage(MSG_START_HANDFREE, param.substring(2));
				mHandler.sendMessage(msg);
			}else {
				Message msg = mHandler.obtainMessage(MSG_PHONE_STATE_CHANGE, param.substring(2));
				mHandler.sendMessage(msg);
			}
		
		}else if(param.startsWith("IX")){
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_IX param:" + param.substring(2));
			if(param.length() > 0){	
				Integer newLevel = Integer.parseInt(param.substring(2,3));
				if(newLevel != mPhoneBatteryLevel) {
					mPhoneBatteryLevel = newLevel;
					Intent icw = new Intent(BonovoBlueToothData.ACTION_PHONE_BATTERY_LEVEL_CHANGED);
					icw.putExtra(BonovoBlueToothData.LEVEL, newLevel);
					mContext.sendBroadcast(icw);
				}
			}
			
		}else if(param.startsWith("IU")){
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_IU param:" + param.substring(2));
			if(param.length() > 0){
				Integer newLevel = Integer.parseInt(param.substring(2,3));
				if(newLevel != mPhoneSignalLevel) {
					mPhoneSignalLevel = newLevel;
					Intent icw = new Intent(BonovoBlueToothData.ACTION_PHONE_SIGNAL_LEVEL_CHANGED);
					icw.putExtra(BonovoBlueToothData.LEVEL, newLevel);
					mContext.sendBroadcast(icw);
				}
			}
			
		}else if(param.startsWith("MM")){
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_MM param:" + param.substring(2));
			myBtName = param;
			
		}else if(param.startsWith("MN")){
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_MN param:" + param.substring(2));
			myBtPinCode = param;

		}else if(param.startsWith("MO")){
			// Speaker Anti-Pop function
			if(DEB) Log.d(TAG, "Callback -->Speaker Anti-Pop (MO) param:" + param.substring(2));
			if (param.substring(2) == "0"){
				// disconnect speaker
				if(mBtMusicIsEnable){
					recoveryAudio(AudioLevel.CODEC_LEVEL_BT_MUSIC);
				}
				recoveryAudio(AudioLevel.CODEC_LEVEL_BT_TEL);
				if(DEB) Log.d(TAG, "Bluetooth device disconnected from speaker.");
			}else{
				// reconnect speaker
				if(mBtMusicIsEnable){
					activeAudio(AudioLevel.CODEC_LEVEL_BT_MUSIC);
				}
				activeAudio(AudioLevel.CODEC_LEVEL_BT_TEL);
				if(DEB) Log.d(TAG, "Bluetooth device reconnected to speaker.");
			}
					
		}else if(param.startsWith("MY")){	
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_MY");
            recoveryAudio(AudioLevel.CODEC_LEVEL_BT_TEL);
            
		}else if(param.startsWith("ERROR")){
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_ERROR");
            Message msg = mHandler.obtainMessage(MSG_SEND_COMMANDER_ERROR);
			mHandler.sendMessage(msg);

		}else if(param.startsWith("IO0")){
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_IO0");
			mMicMuted = false;
			Intent icw = new Intent(BonovoBlueToothData.ACTION_PHONE_MUTE_STATE_CHANGED);
			icw.putExtra("state", mMicMuted);
			mContext.sendBroadcast(icw);
			
		}else if(param.startsWith("IO1")){
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_IO1");
			mMicMuted = true;		
			Intent icw = new Intent(BonovoBlueToothData.ACTION_PHONE_MUTE_STATE_CHANGED);
			icw.putExtra("state", mMicMuted);
			mContext.sendBroadcast(icw);
			
		}else if(param.startsWith("IK")){
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_IK param:" + param.substring(2));
			// call waiting - number attached
			Intent icw = new Intent(BonovoBlueToothData.ACTION_PHONE_NEW_CALL_WAITING);
			icw.putExtra(BonovoBlueToothData.PHONE_NUMBER, param.substring(2));
			mContext.sendBroadcast(icw);
			
		}else if(param.startsWith("IL")){
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_IL");
			// Held active call and switched to call waiting
			Intent icw = new Intent(BonovoBlueToothData.ACTION_PHONE_HELD_ACTIVE_SWITCHED_TO_CALL_WAITING);
			mContext.sendBroadcast(icw);
			
		}else if(param.startsWith("IM")){
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_IM");
			// Conference call created
			Intent icw = new Intent(BonovoBlueToothData.ACTION_PHONE_CONFERENCE_CALL);
			mContext.sendBroadcast(icw);
			
		}else if(param.startsWith("IN")){
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_IN");
			// Release held or reject waiting call (hang up the inactive call)
			Intent icw = new Intent(BonovoBlueToothData.ACTION_PHONE_HUNG_UP_INACTIVE);
			mContext.sendBroadcast(icw);
			
		}else if(param.startsWith("IT")){
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_IT");
			// Release active and switch to call waiting
			Intent icw = new Intent(BonovoBlueToothData.ACTION_PHONE_HUNG_UP_ACTIVE_SWITCHED_TO_CALL_WAITING);
			mContext.sendBroadcast(icw);
			
		}else if(param.startsWith("IQ")){
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_IQ param:" + param);
			// Incoming call with name indication
			
		}else if(param.startsWith("PT")){
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_PT");
			// current call holding
			
		}else if(param.startsWith("PV")){
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_PV param:" + param);
			// Current phone network operator name
			if(param != mPhoneOperatorName) {
				mPhoneOperatorName = param.substring(2);
				Intent icw = new Intent(BonovoBlueToothData.ACTION_PHONE_NETWORK_NAME_CHANGED);
				icw.putExtra(BonovoBlueToothData.NAME, mPhoneOperatorName);
				mContext.sendBroadcast(icw);
			}
			
		}else if(param.startsWith("PZ")){
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_PZ");
			// Last number redial failed
			
		}else if(param.startsWith("MQ")){
			if(DEB) Log.d(TAG, "Callback -->CMD_UNSOLICATED_MQ (pairing):  " + param.substring(2));
			// Remote device wants to pair with us.  
			//	Param:  first 12 bytes, MAC address
			//			Remaining is device name
			
			//TODO: check pin code matches before accpeting.  This is for test only...
			BlueToothAcceptPairingRequest();	// Accept pairing request automatically
			
		}else if(param.startsWith("MH")){
			// A2DP current track information
			Log.v(TAG, "Callback -->CMD_UNSOLICATED_MH: " + param);
			Integer responseType = Integer.parseInt(param.substring(2, 3));
			Integer responseLength;
			String responseValue = param.substring(3);
			switch(responseType) {
			case 0:
				// Error
			case 1:
				// Track Title
			case 2:
				// Artist
			case 3:
				// Album name
			case 4:
				// Track number
			case 5:
				// Total number of tracks
			case 7:
				// playing time in milliseconds
			}
			
		}else if(param.startsWith("MJ")){
			// A2DP current status
			Log.v(TAG, "Callback -->CMD_UNSOLICATED_MJ: " + param);
			Integer responseType = Integer.parseInt(param.substring(2, 3));
			Integer playingPosition;
			Integer playingLength;
			switch(responseType) {
			case 0:
				// Stopped
			case 1:
				// Playing
			case 2:
				// Paused
			case 3:
				// Forward seek
			case 4:
				// Reverse seek
			}	
			
		} else {
			// Fallback to writing to the log
			if(DEB) Log.d(TAG, "Callback -->UNHANDLED COMMAND: " + param);
		}
	}

	public class PairedDevice {
		private Integer mPosition;
		private String mMACAddress;
		private String mName;
		private Boolean mHasHFP;
		private Boolean mHasA2DP;
		private Boolean mIsConnected;
		
		PairedDevice(Integer Position, String MACAddress, String Name) {
			this.mPosition = Position;
			this.mMACAddress = MACAddress;
			this.mName = Name;
		}

		public void set_isConnected(Boolean connectedState){
			mIsConnected = connectedState;
		}
		
		public Boolean get_isConnected(){
			return mIsConnected;
		}
		
		public void set_hasHFP(Boolean hasHFP){
			mHasHFP = hasHFP;
		}
		
		public Boolean get_hasHFP(){
			return mHasHFP;
		}

		public void set_hasA2DP(Boolean hasA2DP){
			mHasA2DP = hasA2DP;
		}
		
		public Boolean get_hasA2DP(){
			return mHasA2DP;
		}
		
		@Override
	    public String toString() {
	        return this.mName;
	    }
	}
	
	class BonovoBlueToothRequestCmd {
		public static final int CMD_SOLICATED_CA = 0;	// Enter pairing mode
		public static final int CMD_SOLICATED_CB = 1;	// Cancel pairing mode
		public static final int CMD_SOLICATED_CC = 2;	// Connect to handset
		public static final int CMD_SOLICATED_CD = 3;	// Disconnect from handset
		public static final int CMD_SOLICATED_CE = 4;	// Answer call
		public static final int CMD_SOLICATED_CF = 5;	// Reject call
		public static final int CMD_SOLICATED_CG = 6;	// End call
		public static final int CMD_SOLICATED_CH = 7;	// Redial
		public static final int CMD_SOLICATED_CI = 8;	// Voice dial (Siri / Google Now activation)
		public static final int CMD_SOLICATED_CJ = 9;	// Cancel voice dial
		public static final int CMD_SOLICATED_CK = 10;	// Volume up
		public static final int CMD_SOLICATED_CL = 11;	// Volume down
		public static final int CMD_SOLICATED_CM = 12;	// Mute / Unmute mic
		public static final int CMD_SOLICATED_CO = 13;	// Transfer audio to/from handset
		public static final int CMD_SOLICATED_CW = 14;	// Dial one call
		public static final int CMD_SOLICATED_CX = 15;	// Send DTMF 
		public static final int CMD_SOLICATED_CY = 16;	// Query HFP status
		public static final int CMD_SOLICATED_CN = 17;	// Start inquiry AG
		public static final int CMD_SOLICATED_CP = 18;	// Stop inquiry AG

		public static final int CMD_SOLICATED_WI = 19;	// ?
		public static final int CMD_SOLICATED_MA = 20;	// Play / pause music
		public static final int CMD_SOLICATED_MC = 21;	// Stop music
		public static final int CMD_SOLICATED_MD = 22;	// Forward
		public static final int CMD_SOLICATED_ME = 23;	// Backward
		public static final int CMD_SOLICATED_MV = 24;	// Query A2DP status
		public static final int CMD_SOLICATED_MO = 25;	// Query AVRCP status

		public static final int CMD_SOLICATED_PA = 26;	// Select SIM phonebook storage
		public static final int CMD_SOLICATED_PB = 27;	// Select phone memory storage
		public static final int CMD_SOLICATED_PH = 28;	// Select dialed call list storage
		public static final int CMD_SOLICATED_PI = 29;	// Select received call list storage 
		public static final int CMD_SOLICATED_PJ = 30;	// Select missed call list storage
		public static final int CMD_SOLICATED_PF = 31;	// Download all phonebook / call list from selected storage
		public static final int CMD_SOLICATED_PE = 32;	// Accept OPP connection
		public static final int CMD_SOLICATED_PG = 33;	// Reject or abort OPP connection
		public static final int CMD_SOLICATED_QA = 34;	// Start PBAP connection
		public static final int CMD_SOLICATED_QB = 35;	// Download phonebook item (via PBAP)
		public static final int CMD_SOLICATED_QC = 36;	// Close PBAP connection

		public static final int CMD_SOLICATED_CZ = 37;	// Reset Bluetooth moduke
		public static final int CMD_SOLICATED_CV = 38;	// Delete Paired Information and Enter Pairing Mode
		public static final int CMD_SOLICATED_MY = 39;	// Query HFP module software version
		public static final int CMD_SOLICATED_MG = 40;	// Enable auto connection
		public static final int CMD_SOLICATED_MH = 41;	// Disable auto connection
		public static final int CMD_SOLICATED_MP = 42;	// Enable auto answer
		public static final int CMD_SOLICATED_MQ = 43;  // Disable auto answer
		public static final int CMD_SOLICATED_MF = 44;	// Query Auto Answer and Power On Auto Connection Configuration
		public static final int CMD_SOLICATED_MM = 45;	// Change local device name
		public static final int CMD_SOLICATED_MN = 46;	// Change local device PIN
		public static final int CMD_SOLICATED_MX = 47;	// Query paired list
		public static final int CMD_SOLICATED_DA = 48;	// ?
		
		public static final int CMD_SOLICATED_CQ = 49;	// Release held or reject waiting call
		public static final int CMD_SOLICATED_CR = 50;  // Release active, accept waiting call
		public static final int CMD_SOLICATED_CS = 51;	// Hold active, accept waiting call
		public static final int CMD_SOLICATED_CT = 52;	// Conference all calls
		
		public static final int CMD_SOLICATED_MZ = 53;	// Query HFP module local address
		public static final int CMD_SOLICATED_QD = 54;	// Get play status (a2dp / avrcp)
		public static final int CMD_SOLICATED_QE = 55;	// Get element attributes (a2dp / avrcp)
		
		public static final int CMD_SOLICATED_PP = 56;	// Send data via SPP
		public static final int CMD_SOLICATED_MJ = 57;	// Disconnect from A2DP source
		public static final int CMD_SOLICATED_QJ = 58;	// Accept pairing request
		public static final int CMD_SOLICATED_QK = 59;	// Reject pairing request
		
		public static final int CMD_SOLICATED_MAX = 60;
	}
	
	class BonovoBlueToothData {
		public final static String ACTION_DATA_IAIB_CHANGED = "android.intent.action.DATA_IAIB_CHANGED";
		public final static String ACTION_DATA_MAMB_CHANGED = "android.intent.action.DATA_MAMB_CHANGED";
		public final static String ACTION_PHONE_STATE_CHANGED = "android.intent.action.PHONE_STATE_CHANGED";
        public final static String ACTION_SYNC_CONTACTS_READ_COUNT = "android.intent.action.SYNC_CONTACTS_READ_COUNT";
        public final static String ACTION_SYNC_CONTACTS_WRITE_DATABASE = "android.intent.action.SYNC_CONTACTS_START_WRITE_DATABASE";
		public final static String ACTION_SYNC_CONTACTS_COMPLETE = "android.intent.action.SYNC_CONTACTS_COMPLETE";
        public final static String ACTION_SYNC_CONTACTS_TIMEOUT = "android.intent.action.SYNC_CONTACTS_TIMEOUT";
        public final static String ACTION_SYNC_CONTACTS_NOT_SUPPORT = "android.intent.action.SYNC_CONTACTS_NOT_SUPPORT";
        public final static String ACTION_SEND_COMMANDER_ERROR = "android.intent.action.SEND_COMMANDER_ERROR";
        public final static String ACTION_PHONE_NEW_CALL_WAITING = "android.intent.action.PHONE_NEW_CALL_WAITING";
        public final static String ACTION_PHONE_HELD_ACTIVE_SWITCHED_TO_CALL_WAITING = "android.intent.action.PHONE_HELD_ACTIVE_SWITCHED_TO_CALL_WAITING";
        public final static String ACTION_PHONE_CONFERENCE_CALL = "android.intent.action.PHONE_CONFERENCE_CALL";
        public final static String ACTION_PHONE_HUNG_UP_INACTIVE = "android.intent.action.PHONE_HUNG_UP_INACTIVE";
        public final static String ACTION_PHONE_HUNG_UP_ACTIVE_SWITCHED_TO_CALL_WAITING = "android.intent.action.PHONE_HUNG_UP_ACTIVE_SWITCHED_TO_CALL_WAITING";
        public final static String ACTION_PHONE_NETWORK_NAME_CHANGED = "android.intent.action.PHONE_NETWORK_NAME_CHANGED";
        public final static String ACTION_PHONE_SIGNAL_LEVEL_CHANGED = "android.intent.action.PHONE_SIGNAL_LEVEL_CHANGED";
        public final static String ACTION_PHONE_BATTERY_LEVEL_CHANGED = "android.intent.action.PHONE_BATTERY_LEVEL_CHANGED";
        public final static String ACTION_PHONE_MUTE_STATE_CHANGED = "android.intent.action.PHONE_MUTE_STATE_CHANGED";
        public final static String ACTION_MUSIC_STATE_CHANGED = "android.intent.action.MUSIC_STATE_CHANGED";
        
		public final static String NAME = "name";
		public final static String VAL = "value";
		public final static String LEVEL = "level";
		public final static String PHONE_NUMBER = "phone_number";
		public final static String PHONE_STATE = "phone_status";
		public final static String HFP_STATUS = "hfp_status";
		public final static String A2DP_STATUS = "a2dp_status";
        public final static String KEY_SYNC_CONTACTS_COUNT = "contacts_count";
	}
}
