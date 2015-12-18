/*
 *     Find Us: privacy oriented location tracker for your friends and family.
 *     Copyright (C) 2015 Dzmitry Lazerka dlazerka@gmail.com
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package me.lazerka.mf.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResolvingResultCallbacks;
import com.google.android.gms.common.api.Status;

import org.acra.ACRA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.lazerka.mf.android.auth.SignInManager;

import static com.google.android.gms.auth.api.Auth.GoogleSignInApi;

/**
 * @author Dzmitry Lazerka
 */
public abstract class GoogleApiActivity extends Activity implements OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {
	private static final Logger logger = LoggerFactory.getLogger(GoogleApiActivity.class);

	private static final int RC_SIGN_IN = 9001;
	private static final int RC_RESOLUTION = 9002;
	private static final int RC_PLAY_ERROR_DIALOG = 9003;

	private SignInManager authenticator = new SignInManager();

	private GoogleApiClient googleApiClient;
	private final SignInCallbacks signInCallbacks = new SignInCallbacks();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		googleApiClient = authenticator.getGoogleApiClientBuilder(this)
				.addOnConnectionFailedListener(this)
				.addConnectionCallbacks(this)
				.build();
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		int errorCode = connectionResult.getErrorCode();
		logger.info("onConnectionFailed: {} {}", errorCode, connectionResult.getErrorMessage());
		GooglePlayServicesUtil.getErrorDialog(errorCode, this, RC_PLAY_ERROR_DIALOG)
				.show();
	}

	@Override
	public void onConnected(Bundle bundle) {
		logger.info("onConnected");
	}

	@Override
	public void onConnectionSuspended(int i) {
		logger.info("onConnectionSuspended: {}", i);
	}


	@Override
	protected void onStart() {
		super.onStart();

		googleApiClient.connect();

		logger.info("1");
		// Note this is not in onResume(), otherwise we might get infinite loop.
		// E.g. with wrong OAuth client IDs, we get SIGN_IN_CANCELLED after clicking on account and try again and again.
		authenticator.getAccountAsync(googleApiClient, signInCallbacks);
		logger.info("2");
	}

	@Override
	protected void onStop() {
		logger.info("onStop");
		super.onStop();
		googleApiClient.disconnect();
	}

	/** Does nothing. We shouldn't save the result and use it later -- it may expire. */
	protected void handleSignInSuccess(GoogleSignInAccount account) {}

	protected abstract void handleSignInFailed();

	/**
	 * @param callbacks to run after signing in, on main thread.
	 */
	public void runWithAccount(SignInCallbacks callbacks) {
		authenticator.getAccountAsync(googleApiClient, callbacks);
	}

	protected void launchSignInActivityForResult() {
		logger.info("Launching SignIn Activity for result");

		Intent signInIntent = GoogleSignInApi.getSignInIntent(googleApiClient);

		startActivityForResult(signInIntent, RC_SIGN_IN);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
			// Result returned from GoogleSignInApi.getSignInIntent()
			case RC_SIGN_IN:
				logger.info("Got result from SignIn Activity: " + resultCode);

				GoogleSignInResult resultFromSignIn = GoogleSignInApi.getSignInResultFromIntent(data);
				// Will handle non-OK results.
				signInCallbacks.onResult(resultFromSignIn);
				break;
			case RC_RESOLUTION: // This path will be never called, but just do it anyway.
				GoogleSignInResult resultFromResolution = GoogleSignInApi.getSignInResultFromIntent(data);
				if (resultFromResolution != null) {
					signInCallbacks.onResult(resultFromResolution);
				} else if (resultCode == RESULT_OK) {
					// Start over.
					authenticator.getAccountAsync(googleApiClient, signInCallbacks);
				} else {
					String msg = "Resolution " + RC_RESOLUTION + " resultCode: " + resultCode;
					logger.warn(msg);
					ACRA.getErrorReporter().handleSilentException(new Exception(msg));
					handleSignInFailed();
				}
				break;
			case RC_PLAY_ERROR_DIALOG:
				handleSignInFailed();
				break;
			default:
				logger.warn("Not ours: " + requestCode);
		}
	}

	protected class SignInCallbacks extends ResolvingResultCallbacks<GoogleSignInResult> {
		public SignInCallbacks() {
			super(GoogleApiActivity.this, RC_RESOLUTION);
		}

		@Override
		public void onSuccess(GoogleSignInResult result) {
			logger.info("SignIn successful");
			handleSignInSuccess(result.getSignInAccount());
		}

		@Override
		public void onUnresolvableFailure(Status status) {
			logger.info("SignIn unsuccessful: {} {}", status.getStatusCode(), status.getStatusMessage());

			if (status.getStatusCode() == CommonStatusCodes.SIGN_IN_REQUIRED) {
				launchSignInActivityForResult();
				return;
			}

			// User pressed "Deny"
			if (status.getStatusCode() == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
				handleSignInFailed();
				return;
			}

			// To reproduce, change GoogleSignInOptions on the fly. // There's nothing user can do about it.
			if (status.getStatusCode() == GoogleSignInStatusCodes.SIGN_IN_FAILED) {
				GoogleSignInApi.revokeAccess(googleApiClient);
				GoogleSignInApi.signOut(googleApiClient);
				Toast.makeText(GoogleApiActivity.this, "Sign-In failed", Toast.LENGTH_LONG).show();
			}
			handleSignInFailed();
		}
	}
}
