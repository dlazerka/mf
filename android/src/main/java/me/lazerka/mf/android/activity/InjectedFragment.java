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

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import me.lazerka.mf.android.di.ApplicationComponent;
import me.lazerka.mf.android.di.Injector;


/**
 * This class only makes sense if it contained any @Injects right here.
 */
public class InjectedFragment extends Fragment {

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		onInject(Injector.applicationComponent());
	}

	/**
	 * Performs dependency injection, using the applicationComponent as the injector.
	 * If a Fragment only needs injection into this base class, it does not need to override this method.
	 * However, if a Fragment requires extra injections (has one ore more @Inject annotations in it's source code),
	 * then it must override this method, and invoke <code>applicationComponent.inject(this);</code>
	 *
	 * @param applicationComponent injector class
	 */
	@SuppressWarnings("WeakerAccess")
	protected void onInject(ApplicationComponent applicationComponent) {
		//applicationComponent.inject(this);
	}
}
