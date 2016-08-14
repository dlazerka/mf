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
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResolvingResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.*;
import com.google.firebase.crash.FirebaseCrash;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.auth.SignInManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.android.gms.auth.api.Auth.GoogleSignInApi;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dzmitry Lazerka
 */
public abstract class GoogleApiActivity extends Activity
		implements OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks
{
	private static final Logger logger = LoggerFactory.getLogger(GoogleApiActivity.class);

	private static final int RC_SIGN_IN = 9001;
	private static final int RC_RESOLUTION = 9002;
	private static final int RC_PLAY_ERROR_DIALOG = 9003;

	protected SignInManager authenticator = new SignInManager();

	private GoogleApiClient googleApiClient;
	private final SignInCallbacks signInCallbacks = new SignInCallbacks();
	private FirebaseAnalytics firebaseAnalytics;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		googleApiClient = authenticator.getGoogleApiClientBuilder(this)
		                               .addOnConnectionFailedListener(this)
		                               .addConnectionCallbacks(this)
		                               .build();
	}

	public EventLogger buildEvent(String eventName) {
		if (firebaseAnalytics == null) {
			FirebaseCrash.logcat(Log.WARN, logger.getName(), "firebaseAnalytics is null");
			FirebaseCrash.report(new NullPointerException("firebaseAnalytics is null"));
			return new EventLogger(eventName, null);
		}
		return new EventLogger(eventName, firebaseAnalytics);
	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
		int errorCode = connectionResult.getErrorCode();
		logger.info("onConnectionFailed: {} {}", errorCode, connectionResult.getErrorMessage());

		Application.getEventLogger("gapi_connection_failed")
				.param("error_code", connectionResult.getErrorCode())
				.param("error_message", connectionResult.getErrorMessage())
				// If this is not null sometimes we could probably use it.
				.param("has_resolution", connectionResult.getResolution() != null)
				.send();

		Dialog errorDialog = GoogleApiAvailability.getInstance()
		                                          .getErrorDialog(this, errorCode, RC_PLAY_ERROR_DIALOG);
		errorDialog.show();
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

		if (!googleApiClient.isConnected()) {
			googleApiClient.connect();
		}

		if (authenticator.getCurrentUser() == null) {
			// Note this is not in onResume(), otherwise we might get infinite loop.
			// E.g. with wrong OAuth client IDs, we get SIGN_IN_CANCELLED after clicking on account and try again and
			// again.
			authenticator.getAccountAsync(googleApiClient, signInCallbacks);
		}
	}

	@Override
	protected void onStop() {
		logger.info("onStop");
		super.onStop();
		googleApiClient.disconnect();
	}

	/**
	 * Does nothing. We shouldn't save the result and use it later -- it may expire.
	 */
	protected void onSignInSuccess(FirebaseUser user) {
		Application.getEventLogger("gapi_signin_success")
			// FirebaseCrash doesn't support user email, so in case an exception happens
			// we had to match exception timing with this log event in order to contact the user.
			.param("display_name", user.getDisplayName())
			.param("email", user.getEmail())
			.send();
	}

	protected abstract void onSignInFailed();

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
					FirebaseCrash.report(new Exception(msg));
					onSignInFailed();
				}
				break;
			case RC_PLAY_ERROR_DIALOG:
				Application.getEventLogger("gapi_connection_retry").send();
				onSignInFailed();
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
		public void onSuccess(@NonNull GoogleSignInResult result) {
			logger.info("SignIn successful");

			GoogleSignInAccount signInAccount = checkNotNull(result.getSignInAccount());
			AuthCredential authCredential = GoogleAuthProvider.getCredential(signInAccount.getIdToken(), null);

			FirebaseAuth.getInstance()
			            .signInWithCredential(authCredential)
			            .addOnCompleteListener(GoogleApiActivity.this, new OnCompleteListener<AuthResult>() {
				            @Override
				            public void onComplete(@NonNull Task<AuthResult> task) {
					            if (task.isSuccessful()) {
						            AuthResult authResult = task.getResult();
						            onSignInSuccess(authResult.getUser());
					            } else {
						            Application.getEventLogger("firebase_auth_unsuccessful").send();
						            onSignInFailed();
					            }
				            }
			            });
		}

		@Override
		public void onUnresolvableFailure(@NonNull Status status) {
			logger.info("SignIn unsuccessful: {} {}", status.getStatusCode(), status.getStatusMessage());

			Application.getEventLogger("gapi_unresolvable_failure")
					.param("status_code", status.getStatusCode())
					.param("error_message", status.getStatusMessage())
					// If this is not null sometimes we could probably use it.
					.param("has_resolution", status.getResolution() != null)
					.send();

			if (status.getStatusCode() == GoogleSignInStatusCodes.SIGN_IN_REQUIRED) {
				launchSignInActivityForResult();
				return;
			}

			// User pressed "Deny"
			if (status.getStatusCode() == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
				onSignInFailed();
				return;
			}

			// To reproduce, change GoogleSignInOptions on the fly. // There's nothing user can do about it.
			if (status.getStatusCode() == GoogleSignInStatusCodes.SIGN_IN_FAILED) {
				GoogleSignInApi.revokeAccess(googleApiClient);
				GoogleSignInApi.signOut(googleApiClient);
				Toast.makeText(GoogleApiActivity.this, "Sign-In failed", Toast.LENGTH_LONG).show();
			}
			onSignInFailed();
		}
	}
}
