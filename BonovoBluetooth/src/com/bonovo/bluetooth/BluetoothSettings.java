package com.bonovo.bluetooth;

import com.bonovo.bluetooth.BonovoBlueToothService.BonovoBlueToothData;
import com.bonovo.bluetooth.BonovoBlueToothService.AudioLevel;
import com.bonovo.bluetooth.BonovoBlueToothReceiver;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewStub;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.ToggleButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class BluetoothSettings extends Activity implements View.OnClickListener, View.OnLongClickListener{

	private final static String TAG = "BluetoothSettings";
	private final static boolean DEBUG = false;
	private ViewStub mStubBtSettings = null;
	private ViewStub mStubBtMusic = null;
	private Button mBtnPhone = null;
	private ImageButton mIgeBtnVolUp = null;
	private ImageButton mIgeBtnVolDown = null;
	
	private ListView mPairedList = null;
	
	//private Button mBtnBtPower = null; 	//0718 ɾ��
	private TextView mTvBtNameInfo = null;
	private TextView mTvBtName = null;
	private TextView mTvBtPinInfo = null;
	private TextView mTvBtPin = null;
	private TextView mTvBtStatus = null;
	private TextView mTvBtHFPStatus = null;
	private TextView mBtnName = null;       //0825
	private TextView mBtnMusicName = null;  //0825
	
	private ToggleButton mSwBtPower = null;      //0718 �¼�
	private ToggleButton mSwMusic = null;	 //0820 �޸�
	private Context mContext = null;
	
	private static BonovoBlueToothService mBtService = null;
	
	private static final int MSG_SYNC_CONTACTS_COMPLETE = 1;
	private static final int MSG_UPDATA_BT_NAME = 2; // update name message id
	private static final int MSG_READ_BT_NAME = 3; // update name message id
	private static final int MSG_UPDATA_BT_PIN = 4;  // update pin message id
	private static final int MSG_READ_BT_PIN = 5;  // update pin message id
	private int mReadBtNameTime = 0;
	private int mReadBtPinTime = 0;
	private final static int MAX_READ_TIME = 5;
	private final static int DELAY_TIME_READ = 3 * 1000;
	private static boolean mDown = false;//keyEvent flag
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bluetooth);
		mContext = this;
			
		mStubBtSettings = (ViewStub)findViewById(R.id.stubSettings);
		mStubBtSettings.inflate();
		mStubBtSettings.setVisibility(View.VISIBLE);
		
		// bottom buttons
		mBtnPhone = (Button)findViewById(R.id.btnPhone);
		mIgeBtnVolUp = (ImageButton)findViewById(R.id.btnVolAdd);
		mIgeBtnVolDown = (ImageButton)findViewById(R.id.btnVolDown);
		
		// Paired devices listview
		mPairedList = (ListView)findViewById(R.id.PairedList);
		
		mBtnPhone.setOnClickListener(this);
		mIgeBtnVolUp.setOnClickListener(this);
		mIgeBtnVolDown.setOnClickListener(this);
		
		//  bt settings's module
		//	mBtnBtPower = (Button)findViewById(R.id.btnBluetoothStatus);
		//	mBtnBtPower.setOnClickListener(this);
		mTvBtName = (TextView)findViewById(R.id.textView1);
		mTvBtNameInfo = (TextView)findViewById(R.id.textView2);
		mTvBtPin = (TextView)findViewById(R.id.textView3);
		mTvBtPinInfo = (TextView)findViewById(R.id.textView4);
		mTvBtStatus = (TextView)findViewById(R.id.textViewBlueToothStatus);
		mTvBtHFPStatus = (TextView)findViewById(R.id.textViewPhoneLinkStatus);
		mBtnName = (TextView)findViewById(R.id.textView5);      //0825
		mBtnMusicName = (TextView)findViewById(R.id.textView6);	//0825	
		
		mTvBtName.setOnLongClickListener(this);
		mTvBtNameInfo.setOnLongClickListener(this);
		mTvBtPin.setOnLongClickListener(this);
		mTvBtPinInfo.setOnLongClickListener(this);
				
		mSwBtPower = (ToggleButton)findViewById(R.id.swBtStatus);
		try {
			mSwBtPower.setChecked(mBtService.getBtSwitchStatus());
		} catch(Exception ex) {
			mSwBtPower.setChecked(false);
		}
		mSwBtPower.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
				if(mBtService == null)
					return;

                if(mBtService.getBtSwitchStatus() != isChecked){
                    mBtService.setBtSwitchStatus(isChecked);
                }
				boolean powerStatus = mBtService.getBtSwitchStatus();
				boolean hfpStatus = mBtService.getBtHFPStatus();
				//	mSwBtPower.setText(powerStatus ? R.string.setting_button_bluetooth_status_closed : R.string.setting_button_bluetooth_status_open);						
				mTvBtStatus.setText(powerStatus ? R.string.bluetooth_status_opened : R.string.bluetooth_status_closed);
				mTvBtHFPStatus.setText(hfpStatus ? R.string.phone_link_status_opened : R.string.phone_link_status_closed);
				if(isChecked){
					//ѡ��ʱ�����Ĳ���
					mTvBtName.setVisibility(View.VISIBLE);
					mTvBtPin.setVisibility(View.VISIBLE);
					mTvBtNameInfo.setText(mBtService.getBtName());
					mTvBtPinInfo.setText(mBtService.getBtPinCode());
					mTvBtNameInfo.setVisibility(View.VISIBLE);
					mTvBtPinInfo.setVisibility(View.VISIBLE);
					mSwMusic.setVisibility(View.VISIBLE);					
					mTvBtStatus.setVisibility(View.GONE);		//0722
					mTvBtHFPStatus.setVisibility(View.VISIBLE);//0722
					mBtnMusicName.setVisibility(View.VISIBLE);//0825
					
				}else{
					mTvBtName.setVisibility(View.GONE);
					mTvBtPin.setVisibility(View.GONE);
					mTvBtNameInfo.setVisibility(View.GONE);
					mTvBtPinInfo.setVisibility(View.GONE);
					mSwMusic.setVisibility(View.GONE);
					mTvBtStatus.setVisibility(View.VISIBLE);//0722
					mTvBtHFPStatus.setVisibility(View.GONE);//0722
					mBtnMusicName.setVisibility(View.GONE);//0825
				}
			}
		});
		
		
		mSwMusic = (ToggleButton)findViewById(R.id.swBtMusic);
		try {
			mSwMusic.setChecked(mBtService.getMusicServiceEnable());
		} catch(Exception ex) {
			mSwMusic.setChecked(false);
		}
        mSwMusic.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override			
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
				if(isChecked){			
					if(mBtService != null){
						mBtService.setMusicServiceEnable(true);
					}
				}else{
					if(mBtService != null){
						mBtService.setMusicServiceEnable(false);
					}
				}
			}
		});
		
		registerReceiver(mReceiver, getIntentFilter());

		Intent intent = new Intent();
		intent.setClassName("com.bonovo.bluetooth", "com.bonovo.bluetooth.BonovoBlueToothService");
		bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
	}
	
	
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub

		// Cancel pairing mode
		mBtService.BlueToothCancelPairingMode();
		
		super.onDestroy();
		Intent intent = new Intent();
		intent.setClassName("com.bonovo.bluetooth", "com.bonovo.bluetooth.BonovoBlueToothService");
		unbindService(mServiceConnection);
		unregisterReceiver(mReceiver);
	}
	
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}
	
	private ServiceConnection mServiceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			mBtService = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			mBtService = ((BonovoBlueToothService.ServiceBinder)service).getService();
			mSwMusic.setChecked(mBtService.getMusicServiceEnable());
			boolean powerStatus = mBtService.getBtSwitchStatus();
			boolean hfpStatus = mBtService.getBtHFPStatus();
            mSwBtPower.setChecked(powerStatus);
			//	mBtnBtPower.setText(powerStatus ? R.string.setting_button_bluetooth_status_closed : R.string.setting_button_bluetooth_status_open);
			mTvBtStatus.setText(powerStatus ? R.string.bluetooth_status_opened : R.string.bluetooth_status_closed);
			mTvBtHFPStatus.setText(hfpStatus ? R.string.phone_link_status_opened : R.string.phone_link_status_closed);
			mTvBtNameInfo.setText(mBtService.getBtName());
			mTvBtPinInfo.setText(mBtService.getBtPinCode());
			
			// Fill the list of paired devices
			ArrayAdapter<BonovoBlueToothService.PairedDevice> adapter = new ArrayAdapter<BonovoBlueToothService.PairedDevice>(mContext,
				android.R.layout.simple_list_item_1, mBtService.pairedDevices);
        
			mPairedList.setAdapter(adapter);
			
			// Enter pairing mode
			mBtService.BlueToothEnterPairingMode();
		}
	};

	private IntentFilter getIntentFilter(){
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BonovoBlueToothData.ACTION_DATA_IAIB_CHANGED);
		intentFilter.addAction(BonovoBlueToothData.ACTION_DATA_MAMB_CHANGED);
		intentFilter.addAction(BonovoBlueToothData.ACTION_SYNC_CONTACTS_COMPLETE);
        intentFilter.addAction(BonovoBlueToothData.ACTION_SYNC_CONTACTS_READ_COUNT);
        intentFilter.addAction(BonovoBlueToothData.ACTION_SYNC_CONTACTS_WRITE_DATABASE);
        intentFilter.addAction(BonovoBlueToothData.ACTION_SYNC_CONTACTS_TIMEOUT);
        intentFilter.addAction(BonovoBlueToothData.ACTION_SYNC_CONTACTS_NOT_SUPPORT);
        intentFilter.addAction(BonovoBlueToothData.ACTION_SEND_COMMANDER_ERROR);
        intentFilter.addAction("android.intent.action.ACTION_SHUTDOWN");
		intentFilter.addAction("android.intent.action.BONOVO_SLEEP_KEY");
		intentFilter.addAction("android.intent.action.BONOVO_WAKEUP_KEY");
		return intentFilter;
	}
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if(BonovoBlueToothData.ACTION_DATA_IAIB_CHANGED.equals(action)){
				if(mBtService != null){
					boolean hfpStatus = mBtService.getBtHFPStatus();
					mTvBtHFPStatus.setText(hfpStatus ? R.string.phone_link_status_opened : R.string.phone_link_status_closed);
				}
			}else if(BonovoBlueToothData.ACTION_SEND_COMMANDER_ERROR.equals(action)){
				if(DEBUG) Log.e(TAG, "send bluetooth commander error!!!");
			}else if(action.equals("android.intent.action.BONOVO_SLEEP_KEY")
               || action.equals("android.intent.action.ACTION_SHUTDOWN")){
				mTvBtStatus.setText(R.string.bluetooth_status_closed);
    			mTvBtHFPStatus.setText(R.string.phone_link_status_closed);
			}else if(action.equals("android.intent.action.BONOVO_WAKEUP_KEY")){

			}
		}
		
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.bluetooth_settings, menu);
		return true;
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId()){
		// the button of phone
		case R.id.btnPhone:{
			if((mBtService == null) || (!mBtService.getBtSwitchStatus())){
				Toast.makeText(mContext, R.string.description_phone_disable, Toast.LENGTH_SHORT).show();
				break;
			}
			
			if(DEBUG) Log.d(TAG, "btnPhone is been pressed.");
			Intent intent = new Intent(this, BonovoBluetoothHandfree.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			break;
		}
							
		// the button of volume up
		case R.id.btnVolAdd:{
			if((mBtService == null) || (!mBtService.getBtSwitchStatus())){
				Toast.makeText(mContext, R.string.description_volume_disable, Toast.LENGTH_SHORT).show();
				break;
			}
			if((mBtService != null) && mBtService.getBtSwitchStatus()){
				mBtService.BlueToothPhoneVolumeUp();
			}
			break;
		}
		
		// the button of volume down
		case R.id.btnVolDown:{
			if((mBtService == null) || (!mBtService.getBtSwitchStatus())){
				Toast.makeText(mContext, R.string.description_volume_disable, Toast.LENGTH_SHORT).show();
				break;
			}
			if((mBtService != null) && mBtService.getBtSwitchStatus()){
				mBtService.BlueToothPhoneVolumeDown();
			}
			break;
		}
		default:
			break;
		}
	}


	@Override
	public boolean onLongClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId()){
		case R.id.textView1:
		case R.id.textView2:
		case R.id.textView3:
		case R.id.textView4:
			LayoutInflater inflater = getLayoutInflater();
			final View view = inflater.inflate(R.layout.dialog_bt_info_change, null);
			final EditText etBtName = (EditText)view.findViewById(R.id.etDialogBtName);
			final EditText etBtPin = (EditText)view.findViewById(R.id.etDialogBtPinCode);
			new AlertDialog.Builder(this)
				.setIcon(R.drawable.ic_launcher)
				.setTitle(R.string.description_title_setting)
				.setView(view)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						
						Message msgUpdateName = mHandler.obtainMessage(MSG_UPDATA_BT_NAME, etBtName.getText().toString());
						mHandler.sendMessage(msgUpdateName);
						
						Message msgUpdatePin = mHandler.obtainMessage(MSG_UPDATA_BT_PIN, etBtPin.getText().toString());
						mHandler.sendMessage(msgUpdatePin);
					}
				})
				.setNegativeButton(android.R.string.cancel, null)
				.show();
			etBtName.setText(mTvBtNameInfo.getText());
			etBtName.setSelection(etBtName.getText().length());
			etBtPin.setText(mTvBtPinInfo.getText());
			etBtPin.setSelection(etBtPin.getText().length());
			break;
		default:
			break;
		}
		return false;
	}
	
	private Handler mHandler = new Handler() {
		@SuppressWarnings("deprecation")
		public void handleMessage(Message msg) {
			int what = msg.what;
			switch (what) {
			case MSG_UPDATA_BT_NAME:
				String newName = (String)msg.obj;
				if((newName != null) && (!newName.equals("")) && (mBtService != null)){
					mBtService.setBtName(newName);
                    if(mBtService.getBtSwitchStatus()){
                        mBtService.BlueToothSetOrCheckName(newName);
                        mHandler.sendEmptyMessageDelayed(MSG_READ_BT_NAME, DELAY_TIME_READ);
                    }else{
                        mTvBtNameInfo.setText(mBtService.getBtName());
                    }
				}
				break;
			case MSG_READ_BT_NAME:
				if(mBtService != null){
					mBtService.BlueToothSetOrCheckName(null);
				}
				break;
			case MSG_UPDATA_BT_PIN:
				String newPin = (String)msg.obj;
				if((newPin != null) && (!newPin.equals("")) && (mBtService != null)){
					mBtService.setBtPinCod(newPin);
                    if(mBtService.getBtSwitchStatus()){
                        mBtService.BlueToothSetOrCheckPin(newPin);
                        mHandler.sendEmptyMessageDelayed(MSG_READ_BT_PIN, DELAY_TIME_READ);
                    }else{
                        mTvBtPinInfo.setText(mBtService.getBtPinCode());
                    }
				}
				break;
			case MSG_READ_BT_PIN:
				if(mBtService != null){
					mBtService.BlueToothSetOrCheckPin(null);
				}
				break;
			default:
				break;
			}
		}
	};
}
