package me.lazerka.mf.api.gcm;

/**
 * The `data` field of an incoming GCM request.
 *
 * @author Dzmitry Lazerka
 */
public interface GcmPayload {
	String TYPE_FIELD = "type";
	String PAYLOAD_FIELD = "payload";

	String getType();
}
