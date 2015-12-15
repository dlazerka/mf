package me.lazerka.mf.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.*;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import me.lazerka.mf.android.auth.AndroidAuthenticator;
import org.acra.ACRA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import static com.google.android.gms.auth.api.Auth.GoogleSignInApi;

/**
 * @author Dzmitry Lazerka
 */
public abstract class GoogleApiActivity extends FragmentActivity implements OnConnectionFailedListener {
	private static final Logger logger = LoggerFactory.getLogger(GoogleApiActivity.class);

	private static final int RC_SIGN_IN = 9001;
	private static final int RC_RESOLUTION = 9002;
	private static final int RC_PLAY_ERROR_DIALOG = 9003;

	AndroidAuthenticator authenticator = new AndroidAuthenticator();

	private GoogleApiClient googleApiClient;
	private GoogleSignInAccount account;
	private final SignInCallbacks signInCallbacks = new SignInCallbacks();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		googleApiClient = authenticator.getGoogleApiClient(this)
				.enableAutoManage(this, this)
				.build();
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		int errorCode = connectionResult.getErrorCode();
		GooglePlayServicesUtil.getErrorDialog(errorCode, this, RC_PLAY_ERROR_DIALOG)
				.show();
	}

	@Override
	protected void onStart() {
		super.onStart();

		// Note this is not in onResume(), otherwise we might get infinite loop.
		// E.g. with wrong OAuth client IDs, we get SIGN_IN_CANCELLED after clicking on account and try again and again.


		authenticator.getAccountAsync(googleApiClient, signInCallbacks);
	}

	protected void handleSignInSuccess(GoogleSignInAccount account) {
		logger.info("SignIn successful");
		this.account = account;
	}

	protected abstract void handleSignInFailed();

	@Nullable
	public GoogleSignInAccount getAccount() {
		return account;
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

	private class SignInCallbacks extends ResolvingResultCallbacks<GoogleSignInResult> {
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
