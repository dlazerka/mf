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

package me.lazerka.mf.android.di;

import com.baraded.mf.android.MainActivity;
import com.baraded.mf.logging.LogService;
import dagger.Component;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.BackupAgent;
import me.lazerka.mf.android.GcmManager;
import me.lazerka.mf.android.activity.*;
import me.lazerka.mf.android.background.gcm.GcmReceiveService;
import me.lazerka.mf.android.background.gcm.SendTokenToServerService;
import me.lazerka.mf.android.background.location.LocationStopListener;
import me.lazerka.mf.android.background.location.LocationUpdateListener;
import me.lazerka.mf.android.location.LocationService;

import javax.inject.Singleton;

@Singleton
@Component(modules = {ApplicationModule.class})
public interface ApplicationComponent {
    void inject(Application obj);

	// Context-dependent
    void inject(MainActivity obj);
    void inject(MainActivity obj);
	void inject(LoginActivity loginActivity);
    void inject(ContactsFragment obj);
    void inject(ContactFragment obj);
    void inject(MapFragment obj);
	void inject(GcmReceiveService obj);
	void inject(LocationUpdateListener obj);
	void inject(BackupAgent obj);

	// Own
    void inject(LocationService obj);
    void inject(LogService obj);
	void inject(GcmManager obj);
	void inject(SendTokenToServerService obj);
	void inject(LocationStopListener obj);

	LogService getLogService();

	LocationService getLocationService();
}
