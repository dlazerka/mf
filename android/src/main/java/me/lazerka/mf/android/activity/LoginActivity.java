package me.lazerka.mf.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import me.lazerka.mf.android.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Obtains {@link GoogleSignInAccount}, which is usually valid for 60 minites.
 *
 * @author Dzmitry Lazerka
 */
public class LoginActivity extends GoogleApiActivity {
	private static final Logger logger = LoggerFactory.getLogger(LoginActivity.class);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_signing_progress);
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		super.onConnectionFailed(connectionResult);
		showSignInButton();
	}

	@Override
	protected void handleSignInFailed() {
		showSignInButton();
	}

	@Override
	protected void handleSignInSuccess(GoogleSignInAccount account) {
		super.handleSignInSuccess(account);

		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);
		finish();
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
}

