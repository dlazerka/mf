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

package me.lazerka.mf.android;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataOutput;
import android.os.ParcelFileDescriptor;
import me.lazerka.mf.android.di.Injector;

import javax.inject.Inject;
import java.io.IOException;

/**
 * Gives opportunity to Preferences to clear non-backupable data.
 *
 * @author Dzmitry Lazerka
 */
public class BackupAgent extends BackupAgentHelper {

	@Inject
	GcmManager gcmManager;

	public BackupAgent() {
		Injector.applicationComponent().inject(this);
	}

	@Override
	public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState)
			throws IOException {
		gcmManager.onBeforeBackup();
		super.onBackup(oldState, data, newState);
	}
}
