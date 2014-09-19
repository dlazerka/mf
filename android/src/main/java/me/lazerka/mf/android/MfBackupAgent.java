package me.lazerka.mf.android;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataOutput;
import android.os.ParcelFileDescriptor;

import java.io.IOException;

/**
 * Gives opportunity to Preferences to clear non-backupable data.
 *
 * @author Dzmitry Lazerka
 */
public class MfBackupAgent extends BackupAgentHelper {
	@Override
	public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws IOException {
		Application.preferences.onBeforeBackup();
		super.onBackup(oldState, data, newState);
	}
}
