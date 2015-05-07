package me.lazerka.mf.api.gcm;

import javax.annotation.Nonnull;

/**
 * @author Dzmitry Lazerka
 */
public abstract class GcmPayload {
	@Nonnull
	public abstract String getType();
}
