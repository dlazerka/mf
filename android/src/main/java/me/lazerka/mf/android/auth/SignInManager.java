/*
 *     Copyright (C) 2017 Dzmitry Lazerka
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package me.lazerka.mf.android.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.support.annotation.WorkerThread;
import com.baraded.mf.logging.LogService;
import com.baraded.mf.logging.Logger;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.Builder;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import me.lazerka.mf.android.R;

import javax.annotation.Nullable;

import static com.google.android.gms.auth.api.Auth.GoogleSignInApi;

/**
 * Obtains OAuth token to talk to our backend API.
 *
 * @author Dzmitry Lazerka
 */
public class SignInManager {
	private static final Logger logger = LogService.getLogger(SignInManager.class);

	/**
	 * @return new instance of GoogleApiClient. You have to connect()/disconnect() it yourself.
	 */
	public GoogleApiClient buildClient(Context context) {
		return getGoogleApiClientBuilder(context)
				.build();
	}

	public Builder getGoogleApiClientBuilder(Context context) {
		GoogleSignInOptions gso = new GoogleSignInOptions.Builder()
				.requestId()
				//.requestProfile() // We don't need profile.
				.requestEmail()
				.requestIdToken(context.getString(R.string.server_oauth_key))
				.build();

		// If there's only one account on device, we're sure user would want to use it.
		AccountManager accountManager = AccountManager.get(context);
		Account[] accounts = accountManager.getAccountsByType("com.google");
		logger.trace("Found {} accounts of type 'com.google'", accounts.length);
		if (accounts.length == 1) {
			String accountName = accounts[0].name;
			gso = new GoogleSignInOptions.Builder(gso)
					.setAccountName(accountName)
					.build();
		}

		return new Builder(context)
				.addApi(Auth.GOOGLE_SIGN_IN_API, gso)
				.addApi(LocationServices.API);
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
			logger.trace("silentSignIn.isDone");
			// If the user's cached credentials are valid, the OptionalPendingResult will be "done"
			// and the GoogleSignInResult will be available instantly.
			GoogleSignInResult signInResult = opr.get();

			callback.onResult(signInResult);
		} else {
			// If the user has not previously signed in on this device or the sign-in has expired,
			// this asynchronous branch will attempt to sign in the user silently.  Cross-device
			// single sign-on will occur in this branch.
			logger.trace("silentSignIn.is not Done, setting resultCallback to {}", opr);
			opr.setResultCallback(callback);
		}
	}

	/**
	 * Creates a new GoogleApiClient and synchronously requests account.
	 */
	@WorkerThread
	public GoogleSignInAccount getAccountBlocking(Context context) throws GoogleApiException {
		GoogleApiClient client = buildClient(context);

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
	@WorkerThread
	public GoogleSignInAccount getAccountBlocking(GoogleApiClient client) throws GoogleApiException {

		OptionalPendingResult<GoogleSignInResult> opr = GoogleSignInApi.silentSignIn(client);

		GoogleSignInResult signInResult = opr.await();// Blocks.
		if (!signInResult.isSuccess()) {
			throw new GoogleSignInException(signInResult.getStatus());
		}

		return signInResult.getSignInAccount();
	}

	@Nullable
	public FirebaseUser getCurrentUser() {
		return FirebaseAuth.getInstance().getCurrentUser();
	}
}
