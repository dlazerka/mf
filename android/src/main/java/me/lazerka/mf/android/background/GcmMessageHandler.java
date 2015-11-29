package me.lazerka.mf.android.background;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;
import me.lazerka.mf.android.Application;
import me.lazerka.mf.android.R;
import me.lazerka.mf.android.activity.MainActivity;
import me.lazerka.mf.api.gcm.GcmPayload;
import org.acra.ACRA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Dzmitry Lazerka
 */
public abstract class GcmMessageHandler<P extends GcmPayload> extends Handler {
	private static final Logger logger = LoggerFactory.getLogger(GcmMessageHandler.class);

	private static final int NOTIFICATION_ID = 1;

	protected P parseGcmPayload(Message message, Class<P> clazz, Context context) throws IOException {
		Bundle bundle = message.getData();
		String json = bundle.getString(GcmIntentService.JSON);

		try {
			return Application.jsonMapper.readValue(json, clazz);
		} catch (IOException e) {
			ACRA.getErrorReporter().handleSilentException(e);

			// TODO remove for production
			String msg = "Cannot parse json as " + clazz.getSimpleName() + ": " + json;
			logger.error(msg);
			Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
			throw e;
		}
	}

	// Put the message into a notification and post it.
	// This is just one simple example of what you might choose to do with
	// a GCM message.
	protected void sendNotification(String msg, String contentTitle, Context context) {
		NotificationManager notificationManager =
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		Intent intent = new Intent(context, MainActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

		NotificationCompat.Builder builder =
				new NotificationCompat.Builder(context)
						.setSmallIcon(R.mipmap.ic_launcher)
						.setContentTitle(contentTitle)
						.setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
						.setContentText(msg);

		builder.setContentIntent(pendingIntent);
		notificationManager.notify(NOTIFICATION_ID, builder.build());
	}
}
