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

package me.lazerka.mf.gae;

import com.googlecode.objectify.*;
import com.googlecode.objectify.cmd.Loader;
import com.googlecode.objectify.impl.Keys;
import com.googlecode.objectify.util.ResultNow;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Mocks Objectify instead of using it as a fixture (pure unit-test).
 *
 * @author Dzmitry Lazerka
 */
public class MockObjectifyTest {
	@Mock
	private ObjectifyFactory objectifyFactory;

	@Mock
	protected Keys keys;

	@Mock
	protected Objectify objectify;

	@Mock
	protected Loader loader;

	@BeforeMethod
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		ObjectifyService.setFactory(objectifyFactory);
		when(objectifyFactory.keys()).thenReturn(keys);
		when(objectifyFactory.begin()).thenReturn(objectify);
		when(objectify.load()).thenReturn(loader);
	}

	protected <T> Key<T> registerKey(T obj) {
		@SuppressWarnings("unchecked")
		Key<T> key = mock(Key.class);
		when(keys.keyOf(obj)).thenReturn(key);
		return key;
	}

	protected <T> Ref<T> registerRef(T obj) {
		Key<T> key = registerKey(obj);

		@SuppressWarnings("unchecked")
		Ref<T> result = mock(Ref.class);
		when(result.get()).thenReturn(obj);
		when(result.key()).thenReturn(key);
		when(result.isLoaded()).thenReturn(true);
		when(loader.key(key)).thenReturn(new LoadResult<T>(key, new ResultNow<T>(obj)));
		return result;
	}
}
