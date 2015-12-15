package me.lazerka.mf.android.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.Builder;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import me.lazerka.mf.android.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.android.gms.auth.api.Auth.GoogleSignInApi;

/**
 * Obtains OAuth token to talk to our backend API.
 *
 * @author Dzmitry Lazerka
 */
public class AndroidAuthenticator {
	private static final Logger logger = LoggerFactory.getLogger(AndroidAuthenticator.class);

	public Builder getGoogleApiClient(Context context) {
		GoogleSignInOptions gso = new GoogleSignInOptions.Builder()
				.requestId()
				//.requestProfile() // We don't need profile.
				.requestEmail()
				.requestIdToken(context.getString(R.string.server_oauth_key))
				.build();

		// If there's only one account on device, we're sure user would want to use it.
		AccountManager accountManager = AccountManager.get(context);
		Account[] accounts = accountManager.getAccountsByType("com.google");
		logger.info("Found {} accounts of type 'com.google'", accounts.length);
		if (accounts.length == 1) {
			String accountName = accounts[0].name;
			gso = new GoogleSignInOptions.Builder(gso)
					.setAccountName(accountName)
					.build();
		}

		return new Builder(context)
				.addApi(Auth.GOOGLE_SIGN_IN_API, gso);
	}

	/**
	 * Creates a new GoogleApiClient and retrieves SignIn account.
	 *
	 * If signInResult is ready, calls callback immediately, otherwise enqueues it.
	 *
	 * @param client must be connected/disconnected externally.
	 */
	public void getAccountAsync(GoogleApiClient client, ResultCallback<GoogleSignInResult> callback) {
		OptionalPendingResult<GoogleSignInResult> opr = GoogleSignInApi.silentSignIn(client);

		if (opr.isDone()) {
			logger.info("silentSignIn.isDone");
			// If the user's cached credentials are valid, the OptionalPendingResult will be "done"
			// and the GoogleSignInResult will be available instantly.
			GoogleSignInResult signInResult = opr.get();

			callback.onResult(signInResult);
		} else {
			// If the user has not previously signed in on this device or the sign-in has expired,
			// this asynchronous branch will attempt to sign in the user silently.  Cross-device
			// single sign-on will occur in this branch.
			logger.info("silentSignIn.is not Done, setting resultCallback to {}", opr);
			opr.setResultCallback(callback);
		}
	}

	/**
	 * Creates a new GoogleApiClient and synchronously requests account.
	 */
	public GoogleSignInAccount getAccountBlocking(Context context) throws GoogleApiException {
		GoogleApiClient client = getGoogleApiClient(context).build();

		ConnectionResult connectionResult = client.blockingConnect();

		if (!connectionResult.isSuccess()) {
			throw new GoogleApiConnectionException(connectionResult);
		}
		return getAccountBlocking(client);
	}

	/**
	 * Synchronously requests account.
	 * @param client must be already connected.
     */
	public GoogleSignInAccount getAccountBlocking(GoogleApiClient client) throws GoogleApiException {

		OptionalPendingResult<GoogleSignInResult> opr = GoogleSignInApi.silentSignIn(client);

		// TODO debug, remove
		logger.info("requesting signin from {}", opr, new Exception());

		if (!opr.isDone()) {
			logger.info("SignIn not done, blocking with await()...");
			opr.await();// Blocks
		}

		GoogleSignInResult signInResult = opr.get();
		if (!signInResult.isSuccess()) {
			throw new GoogleSignInException(signInResult.getStatus());
		}

		return signInResult.getSignInAccount();
	}

}
