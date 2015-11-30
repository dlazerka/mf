package me.lazerka.mf.android.activity;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.Builder;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * @author Dzmitry Lazerka
 */
public class LoginActivity extends FragmentActivity {
	private static final Logger logger = LoggerFactory.getLogger(LoginActivity.class);

	private static final int RC_SIGN_IN = 9001;

	private GoogleApiClient googleApiClient;

	/**
	 * Simple one-view fragments holder.
	 */
	private final HashMap<Integer, Fragment> fragments = new HashMap<>(2);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_signing_progress);

		// We don't need PROFILE.
		GoogleSignInOptions gso = new GoogleSignInOptions.Builder()
			.requestId()
			.requestEmail()
			.build();

		googleApiClient = new Builder(this).enableAutoManage(
				this, new OnConnectionFailedListener() {
					@Override
					public void onConnectionFailed(ConnectionResult connectionResult) {
						logger.warn("ConnectionFailed: " + connectionResult.getErrorMessage());
						Toast.makeText(LoginActivity.this, connectionResult.getErrorMessage(), Toast.LENGTH_LONG)
								.show();
						showSignInButton();
					}
				})
				.addApi(Auth.GOOGLE_SIGN_IN_API, gso)
				.build();

		signIn();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	private void signIn() {
		OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(googleApiClient);

		if (opr.isDone()) {
			logger.info("Silent SignIn done");
			// If the user's cached credentials are valid, the OptionalPendingResult will be "done"
			// and the GoogleSignInResult will be available instantly.
			GoogleSignInResult signInResult = opr.get();

			handleSignInResult(signInResult);
		} else {
			// If the user has not previously signed in on this device or the sign-in has expired,
			// this asynchronous branch will attempt to sign in the user silently.  Cross-device
			// single sign-on will occur in this branch.
			logger.info("Silent SignIn not done, setting resultCallback");
			opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
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
			Application.account = result.getSignInAccount();
			replaceWithActivity(MainActivity.class);
		} else {
			logger.info("SignIn unsuccessful");
			// Signed out, show unauthenticated UI.
			launchSignInActivityForResult();
		}
	}

	private void showSignInButton() {
		setContentView(R.layout.activity_sign_in_button);
		SignInButton button = (SignInButton) findViewById(R.id.sign_in_button);
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				launchSignInActivityForResult();
			}
		});
	}

	private void launchSignInActivityForResult() {
		logger.info("Launching SignIn Activity for result");
		Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
		startActivityForResult(signInIntent, RC_SIGN_IN);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
			// Result returned from GoogleSignInApi.getSignInIntent()
			case RC_SIGN_IN:
				logger.info("Got result from SignIn Activity: " + resultCode);
				GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
				handleSignInResult(result);
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

