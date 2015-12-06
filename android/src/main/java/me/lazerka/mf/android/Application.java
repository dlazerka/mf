package me.lazerka.mf.android;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Debug;
import android.support.multidex.MultiDexApplication;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import me.lazerka.mf.android.activity.LoginActivity;
import me.lazerka.mf.android.http.GaeRequestQueue;
import me.lazerka.mf.api.JsonMapper;
import me.lazerka.mf.api.object.AcraException;
import org.acra.ACRA;
import org.acra.ACRAConfiguration;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

/**b
 * Extension of {@link android.app.Application}.
 *
 * @author Dzmitry
 */

@ReportsCrashes(
		formUri = Application.SERVER_ADDRESS + AcraException.PATH,
		sharedPreferencesName = "ACRA",
		httpMethod = Method.PUT,
		mode = ReportingInteractionMode.TOAST,
		resToastText = R.string.acra_toast_text
)
public class Application extends MultiDexApplication {
	private static final Logger logger = LoggerFactory.getLogger(Application.class);

	public static String USER_AGENT;

//	public static final boolean IS_SERVER_LOCAL = true;
	public static final boolean IS_SERVER_LOCAL = false;

	// Public for @ReportsCrashes above.
	public static final String SERVER_ADDRESS = IS_SERVER_LOCAL
			? "http://192.168.1.219:8380"
			: "https://lazerka-mf.appspot.com";

	public static final URI SERVER_ROOT = isInsideEmulator() // emulator
			? URI.create("http://10.0.2.2:8380")
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

	/**
	 * Account issued to us by Google Sign-In
	 * Usually valid for 60 minutes.
	 *
	 * @see LoginActivity where it's set.
	 */
	public static GoogleSignInAccount account;

	@Override
	public void onCreate() {
		super.onCreate();

		USER_AGENT = getApplicationContext().getPackageName();

		ACRA.init(this);
		ACRAConfiguration config = ACRA.getConfig();
		// Unable to set that in annotation, because not constant.
		config.setFormUri(SERVER_ROOT + AcraException.PATH);

		jsonMapper = createJsonMapper();
		context = getApplicationContext();
		preferences = new Preferences(this);
		requestQueue = GaeRequestQueue.create();
	}

	private static boolean isInsideEmulator() {
		return Build.DEVICE.startsWith("generic");
	}

	public static boolean isServerDev() {
		return IS_SERVER_LOCAL || isInsideEmulator();
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
				logger.warn(msg);
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
