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

/**b
 * Extension of {@link android.app.Application}.
 *
 * @author Dzmitry
 */

@ReportsCrashes(
		sharedPreferencesName = "ACRA",
		httpMethod = Method.PUT,
		mode = ReportingInteractionMode.TOAST,
		resToastText = R.string.acra_toast_text
)
public class Application extends MultiDexApplication {
	private static final Logger logger = LoggerFactory.getLogger(Application.class);

	/**
	 * Shared static instance, as it's a little expensive to create a new one each time.
	 */
	public static JsonMapper jsonMapper;
	public static Preferences preferences;
	public static Context context;

	@Override
	public void onCreate() {
		super.onCreate();

		ACRA.init(this);
		ACRAConfiguration config = ACRA.getConfig();
		// Unable to set that in annotation, because not constant.
		config.setFormUri(BuildConfig.BACKEND_ROOT + AcraException.PATH);

		jsonMapper = createJsonMapper();
		context = getApplicationContext();
		preferences = new Preferences(this);
	}

	private static boolean isInsideEmulator() {
		return Build.DEVICE.startsWith("generic");
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
