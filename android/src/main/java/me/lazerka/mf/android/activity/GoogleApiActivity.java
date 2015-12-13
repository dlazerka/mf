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
	private static final int RC_PLAY_ERROR_DIALOG = 9123;

	private GoogleApiClient googleApiClient;
	private GoogleSignInAccount account;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AndroidAuthenticator authenticator = new AndroidAuthenticator(this);
		googleApiClient = authenticator.getGoogleApiClient()
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
	protected void onResume() {
		super.onResume();

		OptionalPendingResult<GoogleSignInResult> opr = GoogleSignInApi.silentSignIn(googleApiClient);

		if (opr.isDone()) {
			logger.info("Silent SignIn is done");
			// If the user's cached credentials are valid, the OptionalPendingResult will be "done"
			// and the GoogleSignInResult will be available instantly.
			GoogleSignInResult signInResult = opr.get();

			handleSignInResult(signInResult);
		} else {
			// If the user has not previously signed in on this device or the sign-in has expired,
			// this asynchronous branch will attempt to sign in the user silently.  Cross-device
			// single sign-on will occur in this branch.
			logger.info("Silent SignIn is not done, setting resultCallback");
			opr.setResultCallback(
					new ResultCallback<GoogleSignInResult>() {
						@Override
						public void onResult(GoogleSignInResult signInResult) {
							handleSignInResult(signInResult);
						}
					});
		}
	}

	private void handleSignInResult(GoogleSignInResult result) {
		if (result.isSuccess()) {
			logger.info("SignIn successful");
			handleSignInSuccess(result.getSignInAccount());
		} else {
			Status status = result.getStatus();
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
				Toast.makeText(this, "Sign-In failed", Toast.LENGTH_LONG).show();
			}
			handleSignInFailed();
		}
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

				GoogleSignInResult resultFromIntent = GoogleSignInApi.getSignInResultFromIntent(data);
				// Will handle non-OK results.
				handleSignInResult(resultFromIntent);
				break;
			case RC_PLAY_ERROR_DIALOG:
				handleSignInFailed();
				break;
			default:
				logger.warn("Not ours: " + requestCode);
		}
	}
}
