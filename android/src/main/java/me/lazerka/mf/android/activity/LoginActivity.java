package me.lazerka.mf.android.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.*;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.auth.AndroidAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dzmitry Lazerka
 */
public class LoginActivity extends FragmentActivity {
	private static final Logger logger = LoggerFactory.getLogger(LoginActivity.class);

	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	private static final int RC_SIGN_IN = 9001;
	private static final int RC_PLAY_ERROR_DIALOG = 123;

	private GoogleApiClient googleApiClient;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_signing_progress);

		AndroidAuthenticator authenticator = new AndroidAuthenticator(this);
		GoogleApiClient.Builder builder = authenticator.getGoogleApiClient();
		googleApiClient = builder.enableAutoManage(
				this, new OnConnectionFailedListener() {
					@Override
					public void onConnectionFailed(ConnectionResult connectionResult) {
						int errorCode = connectionResult.getErrorCode();
						GooglePlayServicesUtil.getErrorDialog(errorCode, LoginActivity.this, RC_PLAY_ERROR_DIALOG)
								.show();
						showSignInButton();
					}
				})
		.build();
	}

	@Override
	protected void onStart() {
		super.onStart();

		OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(googleApiClient);

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
			// Signed in successfully, show authenticated UI.
			// Token is usually valid for 60 minutes.
			Application.account = result.getSignInAccount();
			replaceWithActivity(MainActivity.class);
		} else {
			Status status = result.getStatus();
			logger.info("SignIn unsuccessful: {} {}", status.getStatusCode(), status.getStatusMessage());

			if (status.getStatusCode() == CommonStatusCodes.SIGN_IN_REQUIRED) {
				launchSignInActivityForResult();
				return;
			}

			// User pressed "Deny"
			if (status.getStatusCode() == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
				showSignInButton();
				return;
			}

			// To reproduce, change GoogleSignInOptions on the fly. // There's nothing user can do about it.
			if (status.getStatusCode() == GoogleSignInStatusCodes.SIGN_IN_FAILED) {
				Auth.GoogleSignInApi.revokeAccess(googleApiClient);
				Auth.GoogleSignInApi.signOut(googleApiClient);
				Toast.makeText(this, "Sign-In failed", Toast.LENGTH_LONG).show();
			}
			showSignInButton();
		}
	}

	private void launchSignInActivityForResult() {
		logger.info("Launching SignIn Activity for result");

		Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);

		startActivityForResult(signInIntent, RC_SIGN_IN);
	}

	private void showSignInButton() {
		setContentView(R.layout.activity_sign_in_button);
		SignInButton button = (SignInButton) findViewById(R.id.sign_in_button);
		button.setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View v) {
						logger.info("SignInButton onClick()");
						launchSignInActivityForResult();
					}
				});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
			// Result returned from GoogleSignInApi.getSignInIntent()
			case RC_SIGN_IN:
				logger.info("Got result from SignIn Activity: " + resultCode);

				//if (resultCode == RESULT_OK) {
				GoogleSignInResult resultFromIntent = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
				// Will handle non-OK results.
				handleSignInResult(resultFromIntent);
				//} else {
				//	showSignInButton();
				//}
				break;
			case RC_PLAY_ERROR_DIALOG:
				showSignInButton();
				break;
			default:
				logger.warn("Not ours: " + requestCode);
		}
	}

	private void replaceWithActivity(Class<? extends Activity> activityClass) {
		Intent intent = new Intent(this, activityClass);
		startActivity(intent);
		finish();
	}
}

