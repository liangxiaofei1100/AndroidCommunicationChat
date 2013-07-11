package com.dreamlink.androidcommunicationchat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.dreamlink.aidl.Communication;
import com.dreamlink.aidl.OnCommunicationListenerExternal;
import com.dreamlink.aidl.User;

public class ChatActivity extends Activity {
	private EditText messageEidt;
	private Button sendButton;
	private ListView messageHistory;
	private int appId;
	private ArrayAdapter<String> adapter;
	private String TAG = "ChatActivity";
	private Toast toast;
	private final int RECEIVE_MESSAGE = 1;
	private final int SEND_MESSAGE = 2;
	private final int USER_FIRST_UDATE = 3;
	private final int CHAT_INFO_UPDATE = 4;
	private final int USER_REMOVE_UPDATE = 5;
	private final int USER_ADD_UPDATE = 6;
	private final int CHANGE_TO_CHAT = 7;
	private boolean flag = false;
	private Communication communication;
	private User localUser;
	private User sendUser;
	private ListView listView;
	private List<User> userList;
	private SimpleAdapter simpleAdapter;
	private List<Map<String, String>> chatInfo;
	private Intent intent = new Intent("com.dreamlink.communication.ComService");
	private OnCommunicationListenerExternal callBackListener = new OnCommunicationListenerExternal.Stub() {

		@Override
		public void onUserDisconnected(User user) throws RemoteException {
			// TODO Auto-generated method stub
			showToast(user.getUserName() + "    OffLine");
			if (userList != null) {
				handler.obtainMessage(USER_REMOVE_UPDATE, user).sendToTarget();
			}
		}

		@Override
		public void onUserConnected(User user) throws RemoteException {
			// TODO Auto-generated method stub
			showToast(user.getUserName() + "    OnLine");
			if (userList != null) {
				handler.obtainMessage(USER_ADD_UPDATE, user).sendToTarget();
			}
		}

		@Override
		public void onReceiveMessage(byte[] msg, User sendUser)
				throws RemoteException {
			// TODO Auto-generated method stub
			if (sendButton != null) {
				handler.obtainMessage(RECEIVE_MESSAGE,
						sendUser.getUserName() + "  :  " + new String(msg))
						.sendToTarget();
			}
			handler.obtainMessage(CHAT_INFO_UPDATE, sendUser).sendToTarget();

		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// initView();
		createView();
		appId = getAppID();
		toast = new Toast(getApplicationContext());
		toast.setDuration(Toast.LENGTH_SHORT);
		registIPC();
	}

	private void initView() {
		setContentView(R.layout.activity_chat);
		messageEidt = (EditText) findViewById(R.id.edtMsg);
		sendButton = (Button) findViewById(R.id.btnSend);
		sendButton.setOnClickListener(new ButtonListenr());
		messageHistory = (ListView) findViewById(R.id.lstHistoric);
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1);
		messageHistory.setAdapter(adapter);
	}

	private void createView() {
		setContentView(R.layout.chat_info);
		listView = (ListView) findViewById(R.id.user_list);
		chatInfo = new ArrayList<Map<String, String>>();
		simpleAdapter = new SimpleAdapter(getApplicationContext(), chatInfo,
				R.layout.user_info, new String[] { "name", "number" },
				new int[] { R.id.text1, R.id.text2 });
		listView.setAdapter(simpleAdapter);
		userList = new ArrayList<User>();
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub
				if (arg2 > 0) {
					sendUser = userList.get(arg2 - 1);
					handler.obtainMessage(CHANGE_TO_CHAT,
							userList.get(arg2 - 1).getUserName())
							.sendToTarget();
				} else {
					handler.obtainMessage(CHANGE_TO_CHAT, "Talk All")
							.sendToTarget();
				}
			}
		});
	}

	private int getAppID() {
		try {
			ActivityInfo info = this.getPackageManager().getActivityInfo(
					getComponentName(), PackageManager.GET_META_DATA);
			return info.metaData.getInt("APPID");
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, "Get App ID error!");
		}
		return 0;
	}

	private void showToast(String s) {
		toast.cancel();
		toast.setText(s);
		toast.show();
	}

	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			super.handleMessage(msg);
			switch (msg.what) {
			case RECEIVE_MESSAGE:
				adapter.add((String) msg.obj);
				break;
			case SEND_MESSAGE:
				adapter.add(localUser.getUserName() + "  :  " + msg.obj);
				break;
			case USER_FIRST_UDATE:
				chatInfo.clear();
				chatInfo.addAll((List<? extends Map<String, String>>) msg.obj);
				break;
			case CHAT_INFO_UPDATE:
				User u = (User) msg.obj;
				int index = 0;
				for (User user : userList) {
					if (user.getUserID() == u.getUserID()) {
						index = userList.indexOf(user);
					}
				}
				Map<String, String> m = chatInfo.get(index + 1);
				m.put("number", "new");
				break;
			case USER_REMOVE_UPDATE:
				User u1 = (User) msg.obj;
				int pos = userList.indexOf(u1);
				chatInfo.remove(pos + 1);
				userList.remove(pos + 1);
				break;
			case USER_ADD_UPDATE:
				User u2 = (User) msg.obj;
				userList.add(u2);
				HashMap<String, String> map = new HashMap<String, String>();
				map.put("name", u2.getUserName());
				map.put("number", "");
				chatInfo.add(map);
				break;
			case CHANGE_TO_CHAT:
				initView();
				setTitle((CharSequence) msg.obj);
				flag = true;
				break;
			default:
				break;
			}
			if (listView == null) {
				adapter.notifyDataSetChanged();
			} else {
				simpleAdapter.notifyDataSetChanged();
			}
		}
	};

	private class ButtonListenr implements OnClickListener {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (v.getId() == R.id.btnSend) {
				String message = messageEidt.getText().toString();
				if (!message.equals("")) {
					try {
						communication.sendMessage(message.getBytes(), appId,
								sendUser);
						messageEidt.setText("");
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					handler.obtainMessage(SEND_MESSAGE, message).sendToTarget();
				}
			}
		}
	}

	private void registIPC() {
		bindService(intent, connection, Context.BIND_AUTO_CREATE);
	}

	private ServiceConnection connection = new ServiceConnection() {

		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			try {
				communication.unRegistListenr(callBackListener);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		public void onServiceConnected(ComponentName name, IBinder service) {
			communication = Communication.Stub.asInterface(service);
			try {
				localUser = communication.getLocalUser();
				communication.registListenr(callBackListener, appId);
				getAllUser();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (flag) {
				createView();
				getAllUser();
				sendUser = null;
				flag = false;
			} else {
				finish();
			}
		}
		return false;
	}

	private void getAllUser() {
		if (communication != null) {
			ArrayList<HashMap<String, String>> temp = new ArrayList<HashMap<String, String>>();
			HashMap<String, String> map1 = new HashMap<String, String>();
			map1.put("name", "Send All");
			map1.put("number", "");
			temp.add(map1);
			try {
				for (User u : communication.getAllUser()) {
					HashMap<String, String> map = new HashMap<String, String>();
					map.put("name", u.getUserName());
					map.put("number", "");
					temp.add(map);
					userList.add(u);
				}
				handler.obtainMessage(USER_FIRST_UDATE, temp).sendToTarget();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
}
