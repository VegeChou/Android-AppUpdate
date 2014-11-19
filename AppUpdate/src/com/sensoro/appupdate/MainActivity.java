package com.sensoro.appupdate;


import org.apache.http.Header;

import com.google.gson.Gson;
import com.loopj.android.http.SyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;

public class MainActivity extends Activity {
	private static final String CHECK_VERSION_URL = "http://static.sensoro.com/downloads/apps/sensoro-beacon-utility/android/latest.json";

	private Context context;
	private String updateInfoResult;
	private VersionInfo versionInfo;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initAppUpdate();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private class CheckVersionTask extends AsyncTask<Void, Integer, Boolean> {

		@Override
		protected Boolean doInBackground(Void... arg0) {
			if (isNetworkAvailable(context)) {
				getUpdateInfo();
			}
			if (updateInfoResult == null) {
				return false;
			} else {
				return true;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result) {
				int currentVersionCode = getCurrentVersionCode();
				int newVersionCode = getNewVersionCode();
				if (currentVersionCode < newVersionCode) {
					showUpdateDialog();
				}
			}
			super.onPostExecute(result);
		}
	}

	private void initAppUpdate() {
		context = this;
		new CheckVersionTask().execute();
	}

	/**
	 * Get the update info of app from server.
	 * 
	 * @return
	 */
	private void getUpdateInfo() {
		SyncHttpClient syncHttpClient = new SyncHttpClient();
		syncHttpClient.get(context, CHECK_VERSION_URL, new TextHttpResponseHandler() {
			
			@Override
			public void onSuccess(int arg0, Header[] arg1, String arg2) {
				updateInfoResult = arg2;
			}
			
			@Override
			public void onFailure(int arg0, Header[] arg1,String arg2, Throwable arg3) {
				
			}
		});
	}

	/**
	 * Parse the new version code from update info.
	 * 
	 * @param result
	 * @return
	 */
	private int getNewVersionCode() {
		Gson gson = new Gson();
		versionInfo = gson.fromJson(updateInfoResult, VersionInfo.class);
		return versionInfo.getVersionCode();
	}

	/**
	 * Get the current version code of app.
	 * 
	 * @return
	 */
	private int getCurrentVersionCode() {
		int versionCode = 0;
		PackageManager packageManager = this.getPackageManager();
		try {
			PackageInfo packageInfo = packageManager.getPackageInfo(this.getPackageName(), 0);
			versionCode = packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return versionCode;
	}

	/**
	 * Check whether the network is available.
	 * 
	 * @param context
	 * @return
	 */
	private boolean isNetworkAvailable(Context context) {
		ConnectivityManager manager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		if (manager == null) {
			return false;
		}
		NetworkInfo networkinfo = manager.getActiveNetworkInfo();
		if (networkinfo == null || !networkinfo.isAvailable()) {
			return false;
		}
		return true;
	}

	private void showUpdateDialog() {
		// 弹出版本更新提示框
		Dialog dialog = new AlertDialog.Builder(context).setIcon(R.drawable.ic_launcher).setTitle("更新提示").setMessage(updateMessage()).setPositiveButton("后台更新", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				// 下载应用
				new ApkDownLoad(getApplicationContext(), versionInfo.getURL(), getDownloadApkName(),"云子配置", "版本升级").execute();
			}
		}).setNegativeButton("取消", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				arg0.cancel();
			}
		}).create();
		dialog.show();
	}

	private String getDownloadApkName() {
		StringBuilder sb = new StringBuilder();
		sb.append("Yunzi_v");
		sb.append(versionInfo.getVersion());
		sb.append(".apk");
		return sb.toString();
	}
	
	private String updateMessage() {
		StringBuilder sb = new StringBuilder();
		sb.append(versionInfo.getReleaseNote());
		return sb.toString();
	}
}
