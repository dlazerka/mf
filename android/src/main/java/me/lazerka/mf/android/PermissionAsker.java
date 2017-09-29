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

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import com.baraded.mf.logging.LogService;
import com.baraded.mf.logging.Logger;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static java.lang.String.format;
import static me.lazerka.mf.android.Util.checkNotNull;

/**
 * Helper to reduce spagettification of Activity working with runtime Android permissions.
 * Checks for permission granted, and if not, requests it.
 *
 * Activity must pass results to {@link #onRequestPermissionsResult}.
 *
 * @author Dzmitry Lazerka
 */
public class PermissionAsker {
	private static final Logger logger = LogService.getLogger(PermissionAsker.class);

    private final Activity activity;

    private final ConcurrentHashMap<Integer, Callback> tasks = new ConcurrentHashMap<>(1);
	private final int requestCodesStart;
	private final int requestCodesEnd;

	/**
     * @param requestCodesStart From which range to get request codes.
     *                         Should not intersect with other requestCodes for activity.
     */
    public PermissionAsker(int requestCodesStart, int requestCodesEnd, Activity activity) {
        this.requestCodesStart = requestCodesStart;
        this.requestCodesEnd = requestCodesEnd;
        this.activity = activity;
    }

    /**
     * @return Whether request is handled by this Permissioner.
     */
    public boolean onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        Callback callback = tasks.remove(requestCode);

        if (callback == null) {
            logger.debug("Callback for requestCode {} is null", requestCode);
            return false;
        }

        if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
            logger.debug("Permissions {} GRANTED on {}", Arrays.asList(permissions), requestCode);
            callback.onGranted.run();
        } else {
            logger.debug("Permissions {} DECLINED on {}", Arrays.asList(permissions), requestCode);
            if (callback.onDeclined != null) {
                callback.onDeclined.run();
            }
        }

        return true;
    }

    private int getNextRequestCode(Callback callback) {
        for (int requestCode = requestCodesStart; requestCode <= requestCodesEnd; requestCode++) {
            Callback existingCallback = tasks.putIfAbsent(requestCode, callback);
            if (existingCallback == null) {
                return requestCode;
            }
        }

	    String fmt = "All request codes in range %s-%s are occupied. Try larger range?";
	    throw new IllegalStateException(format(fmt, requestCodesStart, requestCodesEnd));
    }

	public static boolean hasPermission(String permission, Context context) {
		return ContextCompat.checkSelfPermission(context, permission) == PERMISSION_GRANTED;
	}

    /**
     * @param onGranted To call on success.
     * @param onDeclined To call on decline.
     *                   Will be called right away (in separate run scope) if set to "Never ask again" by user.
     * @return If was already granted.
     */
    public boolean checkAndRun(String permission, Runnable onGranted, @Nullable Runnable onDeclined) {
	    if (ContextCompat.checkSelfPermission(activity, permission) == PERMISSION_GRANTED) {
		    onGranted.run();
		    return true;
	    }

	    Callback callback = new Callback(onGranted, onDeclined);

	    // It's possible that user checked "Never ask again", so this will return PERMISSION_DENIED
	    // immediately. The only way to fix this is to re-install the app.
	    ActivityCompat.requestPermissions(
			    activity,
			    new String[]{permission},
			    getNextRequestCode(callback));

	    return false;
    }

    private static class Callback {
        final Runnable onGranted;

        @Nullable
        final Runnable onDeclined;

        private Callback(Runnable onGranted, @Nullable Runnable onDeclined) {
            this.onGranted = checkNotNull(onGranted);
            this.onDeclined = onDeclined;
        }
    }
}
