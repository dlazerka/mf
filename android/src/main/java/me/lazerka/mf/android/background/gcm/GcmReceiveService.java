package me.lazerka.mf.android.background.gcm;

import android.os.Bundle;
import android.support.annotation.WorkerThread;
import com.google.android.gms.gcm.GcmListenerService;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.api.gcm.GcmPayload;
import me.lazerka.mf.api.gcm.MyLocationGcmPayload;
import me.lazerka.mf.api.object.LocationRequest;
import org.acra.ACRA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.subjects.BehaviorSubject;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Handler for incoming messages from GCM.
 *
 * GcmListenerService will completeWakefulIntent as soon as onMessageReceived method exits.
 *
 * @author Dzmitry Lazerka
 */
public class GcmReceiveService extends GcmListenerService {
	private static final Logger logger = LoggerFactory.getLogger(GcmReceiveService.class);

	private static final BehaviorSubject<MyLocationGcmPayload> locationReceivedSubject = BehaviorSubject.create();

	/**
	 * @return Interface to get {@link MyLocationGcmPayload} GCM messages by other services.
	 */
	@Nonnull
	public static Observable<MyLocationGcmPayload> getLocationReceivedObservable() {
		return locationReceivedSubject;
	}

	@WorkerThread
	@Override
	public void onMessageReceived(String from, Bundle data) {
		logger.info("Received message from " + from);

		String type = data.getString(GcmPayload.TYPE_FIELD);
		String json = data.getString(GcmPayload.PAYLOAD_FIELD);

		if (type == null) {
			logger.warn("Unknown message class " + data);
			return;
		}
		if (json == null) {
			logger.warn("No {} field", GcmPayload.PAYLOAD_FIELD);
			return;
		}

		try {
			switch (type) {
				case MyLocationGcmPayload.TYPE:
					MyLocationGcmPayload payload = Application.jsonMapper.readValue(json, MyLocationGcmPayload.class);
					locationReceivedSubject.onNext(payload);
					break;
				case LocationRequest.TYPE:
					LocationRequest locationRequest = Application.jsonMapper.readValue(json, LocationRequest.class);
					new LocationRequestHandler(this).handle(locationRequest);
					break;
				default:
					logger.warn("Unknown message type: " + type);
			}
		} catch (IOException e) {
			logger.warn("Cannot parse {}: {}", type, json, e);
			ACRA.getErrorReporter().handleSilentException(e);
			// Don't call onError(), because well-behaved observables can issue onError only once.
			// Observers probably cannot even do much with it anyway.
			// observer.onError(e);
		}

	}
}
