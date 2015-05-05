package me.lazerka.mf.android.auth;

import android.accounts.*;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.gms.common.AccountPicker;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.activity.LoginActivity;
import me.lazerka.mf.api.ApiConstants;

import java.io.IOException;

/**
 * Handles on-device authentication, i.e.obtains Android token.
 *
 * The Android-token will then be used only to exchange it for GAE-token, see {@link GaeAuthenticator}.
 *
 * Must be called from main thread, because user will be prompted for permissions to access GAE.
 *
 * @see me.lazerka.mf.android.auth.GaeAuthenticator
 * @author Dzmitry Lazerka
 */
public class AndroidAuthenticator {
	private static final String TAG = AndroidAuthenticator.class.getName();
	static final String ACCOUNT_TYPE = "com.google";

	/**
	 * @return null if valid, intent to show otherwise.
	 */
	public Intent checkAccountValid() {
		Account account = Application.preferences.getAccount();
		if (account == null || !isAccountAvailable(account)) {
			return AccountPicker.newChooseAccountIntent(
					account, // selectedAccount
					null, // allowable accounts
					new String[]{ACCOUNT_TYPE}, // allowable account types
					false, // alwaysPromptForAccount
					null, // descriptionOverrideText
					null, // addAccountAuthTokenType
					null, // addAccountRequiredFeatures
					null // addAccountOptions
			);
		}
		return null;
	}

	private boolean isAccountAvailable(Account account) {
		AccountManager accountManager = getAccountManager();
		Account[] accounts = accountManager.getAccountsByType(ACCOUNT_TYPE);
		for (Account availableAccount : accounts) {
			if (account.equals(availableAccount)) {
				return true;
			}
		}
		return false;
	}

	public interface AuthenticatorCallback {
		void onSuccess(String authToken);
		void onUserInputRequired(Intent intent);
		void onIOException(IOException e);
		void onAuthenticatorException(AuthenticatorException e);
		void onOperationCanceledException(OperationCanceledException e);
	}

	/**
	 * For invoking from foreground.
	 * Launches credentials prompt user hasn't approved service usage, and token cannot be issued.
	 * If user approved, do nothing with the token (see {@link GaeAuthenticator} for real authentication).
	 *
	 * Note that GoogleAuthUtil.getToken() is completely different from AccountManager.
	 */
	public void checkUserPermission(final LoginActivity activity, final AuthenticatorCallback callback) {
		Account account = Application.preferences.getAccount();

		// It may start activity asking user for permission.
		AccountManagerCallback<Bundle> myCallback = new AccountManagerCallback<Bundle>() {
			@Override
			public void run(AccountManagerFuture<Bundle> future) {
				try {
					Bundle bundle = future.getResult();

					Intent intent = (Intent) bundle.get(AccountManager.KEY_INTENT);
					if (intent != null) {
						callback.onUserInputRequired(intent);
					}
					callback.onSuccess(bundle.getString(AccountManager.KEY_AUTHTOKEN));
				} catch (OperationCanceledException e) {
					callback.onOperationCanceledException(e);
				} catch (IOException e) {
					callback.onIOException(e);
				} catch (AuthenticatorException e) {
					callback.onAuthenticatorException(e);
				}
			}
		};
		AccountManager accountManager = getAccountManager();
		accountManager.getAuthToken(account, ApiConstants.ANDROID_AUTH_SCOPE, Bundle.EMPTY, activity, myCallback, null);
	}

	private AccountManager getAccountManager() {
		return (AccountManager) Application.context.getSystemService(Context.ACCOUNT_SERVICE);
	}
}
