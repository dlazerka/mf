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

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
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

