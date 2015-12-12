package me.lazerka.mf.api.gcm;

/**
 * @author Dzmitry Lazerka
 */
public abstract class GcmPayload {
	public static final String CLASS = "class";
	public static final String DATA = "data";

	public abstract String getType();
}
