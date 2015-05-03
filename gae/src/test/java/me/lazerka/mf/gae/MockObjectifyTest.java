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
