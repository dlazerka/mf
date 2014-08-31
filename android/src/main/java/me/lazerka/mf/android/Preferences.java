package me.lazerka.mf.android;

import android.accounts.Account;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * My API for SharedPreferences, type-safe.
 */
public class Preferences {
	private static final String ACCOUNT_NAME = "account.name";
	private static final String ACCOUNT_TYPE = "account.type";

	private final SharedPreferences preferences;

	public Preferences(Application app) {
		// Same file name as RoboGuice default.
		preferences = app.getSharedPreferences("default.xml", Context.MODE_PRIVATE);
	}

	//@Nullable
	public Account getAccount() {
		String name = preferences.getString(ACCOUNT_NAME, null);
		String type = preferences.getString(ACCOUNT_TYPE, null);
		if (name == null || type == null) {
			return null;
		}
		return new Account(name, type);
	}

	//public void setAccount(@Nonnull Account account) {
	//	Editor editor = preferences.edit();
	//	editor.putString(ACCOUNT_NAME, account.name);
	//	editor.putString(ACCOUNT_TYPE, account.type);
	//	editor.apply();
	//}

	public void clearAccount() {
		preferences.edit()
				.remove(ACCOUNT_NAME)
				.remove(ACCOUNT_TYPE)
				.apply();
	}
}
