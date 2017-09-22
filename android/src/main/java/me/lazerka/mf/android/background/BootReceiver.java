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

package me.lazerka.mf.android.background;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import me.lazerka.mf.android.background.location.LocationRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author Dzmitry Lazerka
 */
public class BootReceiver extends WakefulBroadcastReceiver {
	private static final Logger logger = LoggerFactory.getLogger(BootReceiver.class);

	@Override
	public void onReceive(Context context, Intent intent) {
		logger.info("onReceive");

		checkArgument(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED));

		// Renew GCM token.
		//intent.setComponent(new ComponentName(context, GcmRegisterIntentService.class));
		//startWakefulService(context, intent);

		Intent locationRequestHandler = new Intent(context, LocationRequestHandler.class);
		context.startService(locationRequestHandler);

		setResultCode(Activity.RESULT_OK);
	}
}
