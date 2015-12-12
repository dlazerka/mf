package me.lazerka.mf.android.auth;

import java.io.IOException;

/**
 * @author Dzmitry Lazerka
 */
public abstract class GoogleApiException extends IOException {
	public GoogleApiException(String detailMessage) {
		super(detailMessage);
	}

	public abstract int getCode();
}
