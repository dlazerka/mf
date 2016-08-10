/*
 *     Find Us: privacy oriented location tracker for your friends and family.
 *     Copyright (C) 2016 Dzmitry Lazerka dlazerka@gmail.com
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

package me.lazerka.mf.android;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Helper to reduce spagettification of Activity working with runtime Android permissions.
 * Checks for permission granted, and if not, requests it.
 *
 * Activity must pass results to {@link #onRequestPermissionsResult}.
 *
 * @author Dzmitry Lazerka
 */
public class PermissionAsker {
    private static final String TAG = PermissionAsker.class.getSimpleName();

    private final Activity activity;
    private final Range<Integer> requestCodesPool;

    private final ConcurrentHashMap<Integer, Callback> tasks = new ConcurrentHashMap<>(1);

    /**
     * @param requestCodesPool From which range to get request codes.
     *                         Should not intersect with other requestCodes for activity.
     */
    public PermissionAsker(Range<Integer> requestCodesPool, Activity activity) {
        this.requestCodesPool = requestCodesPool;
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
            Log.d(TAG, "Callback for requestCode " + requestCode + " is null");
            return false;
        }

        if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
            Log.d(TAG, format("Permissions %s GRANTED on %d", Lists.newArrayList(permissions), requestCode));
            callback.onGranted.run();
        } else {
            Log.d(TAG, format("Permissions %s DECLINED on %d", Lists.newArrayList(permissions), requestCode));
            if (callback.onDeclined != null) {
                callback.onDeclined.run();
            }
        }

        return true;
    }

    private int getNextRequestCode(Callback callback) {
        for (int requestCode = requestCodesPool.lowerEndpoint(); requestCodesPool.contains(requestCode); requestCode++) {
            Callback existingCallback = tasks.putIfAbsent(requestCode, callback);
            if (existingCallback == null) {
                return requestCode;
            }
        }

        throw new IllegalStateException(
                format("All request codes in range %s are occupied. Try larger range?", requestCodesPool));
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
