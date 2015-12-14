package me.lazerka.mf.api.gcm;

/**
 * The `data` field of an incoming GCM request.
 *
 * @author Dzmitry Lazerka
 */
public abstract class GcmPayload {
	public static final String TYPE_FIELD = "type";
	public static final String PAYLOAD_FIELD = "payload";

	public abstract String getType();
}
