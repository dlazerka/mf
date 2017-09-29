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

package me.lazerka.mf.android.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.baraded.mf.logging.LogService;
import me.lazerka.mf.android.PermissionAsker;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.di.Injector;

import javax.inject.Inject;

/**
 * Extends FragmentActivity only for GoogleApiClient.
 *
 * @author Dzmitry Lazerka
 */
public class MainActivity extends Activity {
	public final PermissionAsker permissionAsker;

	@Inject
	LogService logService;

	public MainActivity() {
		Injector.applicationComponent().inject(this);

		this.permissionAsker = new PermissionAsker(400, 499, this);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		logService.getEventLogger("app_launched").send();

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					// replace(), not add, because this is called
					.replace(R.id.bottom_fragment_container, new ContactsFragment())
					.commit();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();

		// Make sure server knows our GCM token.
		//startService(new Intent(this, SendTokenToServerService.class));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	// TODO implement settings
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_logout:
				//Intent intent = new Intent(this, LoginActivity.class);
				//startActivity(intent);
				//finish();
				break;
			case R.id.clear_token:
				//Application.gcmManager.clearGcmToken();
				recreate();
				break;
			case R.id.action_quit:
				logService.getEventLogger("app_quit").send();
				this.finish();
				break;
		}
		return super.onOptionsItemSelected(item);
	}
}
