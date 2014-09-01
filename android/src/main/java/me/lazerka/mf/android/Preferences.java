package me.lazerka.mf.android;

import android.accounts.Account;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import java.util.*;

/**
 * My API for SharedPreferences, type-safe.
 */
public class Preferences {
	private static final String TAG = "Preferences";

	private static final String ACCOUNT_NAME = "account.name";
	private static final String ACCOUNT_TYPE = "account.type";
	private static final String FRIENDS = "friends";

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

	public List<Uri> getFriends() {
		Set<String> set = preferences.getStringSet(FRIENDS, Collections.<String>emptySet());
		List<Uri> result = new ArrayList<>(set.size());
		for(String uriString : set) {
			Uri parsed = Uri.parse(uriString);
			result.add(parsed);
		}
		Log.i(TAG, "getFriends " + result.size());
		return result;
	}

	public boolean addFriend(Uri contactUri) {
		Log.i(TAG, "addFriend " + contactUri);
		synchronized (preferences) {
			// Clone, otherwise value won't be set.
			Set<String> friends = new LinkedHashSet<>(preferences.getStringSet(FRIENDS, new HashSet<String>(1)));
			boolean changed = friends.add(contactUri.toString());
			if (!changed) {
				return false;
			}
			preferences.edit()
					.putStringSet(FRIENDS, friends)
					.apply();
			return true;
		}
	}

	public boolean removeFriend(Uri contactUri) {
		Log.i(TAG, "removeFriend " + contactUri);
		synchronized (preferences) {
			// Clone, otherwise value won't be set.
			Set<String> friends = new LinkedHashSet<>(preferences.getStringSet(FRIENDS, new HashSet<String>(0)));
			boolean changed = friends.remove(contactUri.toString());
			if (!changed) {
				return false;
			}
			preferences.edit()
					.putStringSet(FRIENDS, friends)
					.apply();
			return true;
		}
	}
}
