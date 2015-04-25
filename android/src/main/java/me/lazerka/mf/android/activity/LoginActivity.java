package me.lazerka.mf.android.activity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.auth.AndroidAuthenticator;
import me.lazerka.mf.android.auth.AndroidAuthenticator.AuthenticatorCallback;

import java.io.IOException;

/**
 * @author Dzmitry Lazerka
 */
public class LoginActivity extends Activity {
	private final String TAG = getClass().getName();

	private final int ACCOUNT_PICKER = 0;
	private final int USER_CONFIRMATION = 1;

	private final AndroidAuthenticator androidAuthenticator;

	public LoginActivity() {
		this.androidAuthenticator = new AndroidAuthenticator();
	}

	@Override
	protected void onResume() {
		Log.v(TAG, "onResume");
		super.onResume();

		Intent intent = androidAuthenticator.checkAccountValid();
		if (intent != null) {
			startActivityForResult(intent, ACCOUNT_PICKER);
			return;
		}
		// May start activity inside, asking user for permission.
		androidAuthenticator.checkUserPermission(this, new MyAuthenticatorCallback());
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.v(TAG, "onActivityResult: requestCode=" + requestCode + " resultCode=" + resultCode);

		switch (requestCode) {
			case ACCOUNT_PICKER:
				if (resultCode == RESULT_CANCELED) {
					Toast.makeText(this, "Please select an account", Toast.LENGTH_LONG).show();
					// Will reshow account picker.
					return;
				}
				if (resultCode != RESULT_OK) {
					Log.w(TAG, "Result not OK: " + resultCode);
					return;
				}
				String name = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
				String type = data.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);
				Account account = new Account(name, type);
				Application.preferences.setAccount(account);

				androidAuthenticator.checkUserPermission(this, new MyAuthenticatorCallback());
				break;
			case USER_CONFIRMATION:
				if (resultCode == RESULT_CANCELED) {
					Toast.makeText(this, "Unable to continue without permission.", Toast.LENGTH_LONG).show();
					return;
				}
				if (resultCode != RESULT_OK) {
					Log.w(TAG, "Result not OK: " + resultCode);
					return;
				}
				replaceWithActivity(MainActivity.class);
		}
	}

	private void replaceWithActivity(Class<? extends Activity> activityClass) {
		Intent intent = new Intent(this, activityClass);
		startActivity(intent);
		finish();
	}

	private class MyAuthenticatorCallback implements AuthenticatorCallback {
		@Override
		public void onSuccess(String authToken) {
			replaceWithActivity(MainActivity.class);
		}

		@Override
		public void onUserInputRequired(Intent intent) {
			Log.i(TAG, "onUserInputRequired, starting credentials prompt activity.");
			startActivityForResult(intent, USER_CONFIRMATION);
		}

		private void onException(Exception e, String userMessage) {
			Log.w(TAG, e.getClass().getSimpleName() + " getting authentication token", e);
			Toast.makeText(LoginActivity.this, userMessage, Toast.LENGTH_LONG).show();
		}

		@Override
		public void onIOException(IOException e) {
			onException(e, "Network problem");
		}

		@Override
		public void onAuthenticatorException(AuthenticatorException e) {
			onException(e, "Problem authenticating");
		}

		@Override
		public void onOperationCanceledException(OperationCanceledException e) {
			onException(e, "Need permissions to continue");
		}
	}
}

