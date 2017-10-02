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

package com.baraded.mf.android

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.baraded.mf.logging.LogService
import me.lazerka.mf.android.PermissionAsker
import me.lazerka.mf.android.R
import me.lazerka.mf.android.di.Injector
import javax.inject.Inject

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


/**
 * Extends FragmentActivity only for GoogleApiClient.
 *
 * @author Dzmitry Lazerka
 */
class MainActivity2 : Activity() {
    private val permissionAsker: PermissionAsker

    @Inject
    lateinit var logService: LogService

    init {
        Injector.applicationComponent().inject(this)

        this.permissionAsker = PermissionAsker(400, 499, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logService.getEventLogger("app_launched").send()

        val sendButton = findViewById(R.id.send_my_button)
//        val sendButton = findViewById(R.id.send_my_button) as ImageButton

        sendButton.setOnClickListener(SendClickListener())

        //if (savedInstanceState == null) {
        //	getFragmentManager().beginTransaction()
        //			// replace(), not add, because this is called
        //			.replace(R.id.bottom_frame, new ContactsFragment())
        //			.commit();
        //}
    }

    override fun onStart() {
        super.onStart()

        // Make sure server knows our GCM token.
        //startService(new Intent(this, SendTokenToServerService.class));
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    // TODO implement settings
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_logout -> {
            }
            R.id.clear_token ->
                //Application.gcmManager.clearGcmToken();
                recreate()
            R.id.action_quit -> {
                logService!!.getEventLogger("app_quit").send()
                this.finish()
            }
        } //Intent intent = new Intent(this, LoginActivity.class);
        //startActivity(intent);
        //finish();
        return super.onOptionsItemSelected(item)
    }

    private inner class SendClickListener : View.OnClickListener {
        override fun onClick(v: View) {
            logService!!.getEventLogger("send_my_clicked").send()
        }
    }
}
