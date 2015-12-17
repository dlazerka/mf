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

package me.lazerka.mf.android.background.gcm;

import android.content.Intent;
import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * @author Dzmitry Lazerka
 */
public class InstanceIdService extends InstanceIDListenerService {
	@Override
	public void onTokenRefresh() {
		// Fetch updated Instance ID token and notify our app's server of any changes (if applicable).
		Intent intent = new Intent(this, GcmRegisterIntentService.class);
		startService(intent);
	}
}
