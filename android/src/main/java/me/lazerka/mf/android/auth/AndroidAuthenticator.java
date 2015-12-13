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

	private final Context context;

	public AndroidAuthenticator(Context context) {
		this.context = context;
	}

	public Builder getGoogleApiClient() {
		GoogleSignInOptions gso = new GoogleSignInOptions.Builder()
				.requestId()
						//.requestProfile() // We don't need profile.
				.requestEmail()
				.requestIdToken(context.getString(R.string.server_oauth_key))
				.build();

		// If there's only one account on device, we're sure user would want to use it.
		AccountManager accountManager = AccountManager.get(context);
		Account[] accounts = accountManager.getAccountsByType("com.google");
		if (accounts.length == 1) {
			String accountName = accounts[0].name;
			gso = new GoogleSignInOptions.Builder(gso)
					.setAccountName(accountName)
					.build();
		}

		return new Builder(context)
				.addApi(Auth.GOOGLE_SIGN_IN_API, gso);
	}

	/** Creates a new GoogleApiClient and synchronously requests account. */
	public GoogleSignInAccount blockingGetAccount() throws GoogleApiException {
		GoogleApiClient client = getGoogleApiClient().build();

		ConnectionResult connectionResult = client.blockingConnect();

		if (!connectionResult.isSuccess()) {
			throw new GoogleApiConnectionException(connectionResult);
		}
		return blockingGetAccount(client);
	}

	/**
	 * Synchronously requests account.
	 * @param client must be already connected.
     */
	public GoogleSignInAccount blockingGetAccount(GoogleApiClient client) throws GoogleApiException {

		OptionalPendingResult<GoogleSignInResult> opr = GoogleSignInApi.silentSignIn(client);

		if (!opr.isDone()) {
			opr.await();// Blocks
		}
		GoogleSignInResult signInResult = opr.get();
		if (!signInResult.isSuccess()) {
			throw new GoogleSignInException(signInResult.getStatus());
		}

		return signInResult.getSignInAccount();
	}

}
