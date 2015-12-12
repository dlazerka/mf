package me.lazerka.mf.android.background.gcm;

import android.os.Bundle;
import com.google.android.gms.gcm.GcmListenerService;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.api.gcm.GcmPayload;
import me.lazerka.mf.api.gcm.LocationRequest;
import me.lazerka.mf.api.gcm.MyLocationGcmPayload;
import org.acra.ACRA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Observer;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

/**
 * @author Dzmitry Lazerka
 */
public class GcmReceiveService extends GcmListenerService {
	private static final Logger logger = LoggerFactory.getLogger(GcmReceiveService.class);

	private static final BehaviorSubject<MyLocationGcmPayload> locationReceivedSubject = BehaviorSubject.create();
	private static final BehaviorSubject<LocationRequest> locationRequestSubject = BehaviorSubject.create();

	/**
	 * @return Interface to get {@link MyLocationGcmPayload} GCM messages by other services.
	 */
	@Nonnull
	public static Observable<MyLocationGcmPayload> getLocationReceivedObservable() {
		return locationReceivedSubject;
	}

	/**
	 * @return Interface to get {@link MyLocationGcmPayload} GCM messages by other services.
	 */
	@Nonnull
	public static Observable<LocationRequest> getLocationRequestObservable() {
		return locationRequestSubject;
	}

	@Override
	public void onMessageReceived(String from, Bundle data) {
		logger.info("Received message from " + from);

		String type = data.getString(GcmPayload.CLASS);
		String json = data.getString(GcmPayload.DATA);

		if (type == null) {
			logger.warn("Unknown message class " + data);
			return;
		}
		if (json == null) {
			logger.warn("No {} field", GcmPayload.DATA);
			return;
		}

		if (type.equals(MyLocationGcmPayload.class.getName())) {
			parseAndEmit(json, locationReceivedSubject, MyLocationGcmPayload.class);
		} else if (type.equals(LocationRequest.class.getName())) {
			parseAndEmit(json, locationRequestSubject, LocationRequest.class);
		} else {
			logger.warn("Unknown message type: " + type);
		}
	}

	private <T extends GcmPayload> void parseAndEmit(String json, Observer<T> observer, Class<T> clazz) {
		try {
			T value = Application.jsonMapper.readValue(json, clazz);
			observer.onNext(value);
		} catch (IOException e) {
			logger.warn("Cannot parse {}: {}", clazz.getSimpleName(), json, e);
			ACRA.getErrorReporter().handleSilentException(e);
			// Don't call onError(), because then well-behaved observables can issue onError only once.
			// Observers probably cannot even do much with it anyway.
			// observer.onError(e);
		}
	}

	private static class Parser<T extends GcmPayload> implements Func1<Bundle, T> {
		private final Class<T> type;

		public Parser(Class<T> type) {
			this.type = type;
		}

		@Override
		public T call(Bundle bundle) {
			@Nullable
			String json = bundle.getString(GcmPayload.DATA);

			try {
				return Application.jsonMapper.readValue(json, type);
			} catch (IOException e) {
				ACRA.getErrorReporter().handleSilentException(e);

				String msg = "Cannot parse json as " + type.getSimpleName() + ": " + json;
				logger.warn(msg);

				// Move to onError().
				throw new RuntimeException(e);
			}
		}
	}


/*
	private void sendNotification(String message) {
		Intent intent = new Intent(this, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
				PendingIntent.FLAG_ONE_SHOT);

		Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.ic_stat_ic_notification)
				.setContentTitle("GCM Message")
				.setContentText(message)
				.setAutoCancel(true)
				.setSound(defaultSoundUri)
				.setContentIntent(pendingIntent);

		NotificationManager notificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		notificationManager.notify(0, notificationBuilder.build());
	}
	*/

}
