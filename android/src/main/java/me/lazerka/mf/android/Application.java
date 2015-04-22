package me.lazerka.mf.android;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Debug;
import android.support.multidex.MultiDexApplication;
import android.util.Log;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import me.lazerka.mf.android.http.GaeRequestQueue;
import me.lazerka.mf.api.JsonMapper;
import org.acra.ACRA;

import java.io.IOException;
import java.net.URI;

/**
 * Extension of {@link android.app.Application}.
 *
 * @author Dzmitry
 */

public class Application extends MultiDexApplication {
	public static final String VERSION = "1";
	public static String TAG;

	public static String USER_AGENT;

	public static final boolean IS_SERVER_LOCAL = true;
	//public static final boolean IS_SERVER_LOCAL = false;

	public static final String SERVER_ADDRESS = IS_SERVER_LOCAL
			? "http://192.168.1.220:8383"
			: "https://lazerka-mf.appspot.com";

	public static final URI SERVER_ROOT = "generic".equals(Build.DEVICE)
			? URI.create("http://10.0.2.2:8888")
			: URI.create(SERVER_ADDRESS);


	// Static holders of singletons.
	// Some people think that extending Application is discouraged,
	// but I think other way -- I really do want to keep all the singletons
	// in one place to keep track of them.

	/**
	 * Shared static instance, as it's a little expensive to create a new one each time.
	 */
	public static JsonMapper jsonMapper;
	public static Preferences preferences;
	public static Context context;
	public static GaeRequestQueue requestQueue;

	public Application() {
	}

	@Override
	public void onCreate() {
		super.onCreate();

		TAG = getApplicationContext().getPackageName();
		USER_AGENT = getApplicationContext().getPackageName();

		if (!isDebugRun()) {
			ACRA.init(this);
		}

		jsonMapper = createJsonMapper();
		context = getApplicationContext();
		preferences = new Preferences(this);
		requestQueue = GaeRequestQueue.create();
	}

	private JsonMapper createJsonMapper() {
		JsonMapper result = new JsonMapper();
		// Warn, but don't fail on unknown property.
		result.addHandler(new DeserializationProblemHandler() {
			@Override
			public boolean handleUnknownProperty(
					DeserializationContext deserializationContext,
					JsonParser jsonParser,
					JsonDeserializer<?> deserializer,
					Object beanOrClass,
					String propertyName
			) throws IOException {
				String msg = "Unknown property `" + propertyName + "` in " + beanOrClass;
				Log.w(TAG, msg);
				jsonParser.skipChildren();
				return true;
			}
		});
		return result;
	}

	private boolean isDebugRun() {
		return Debug.isDebuggerConnected();
	}

	private boolean isDebugBuild() {
		return ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) > 0);
	}

	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	public static int getVersion() {
		String packageName = Application.context.getPackageName();
		PackageManager packageManager = Application.context.getPackageManager();
		try {
			PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}
}
