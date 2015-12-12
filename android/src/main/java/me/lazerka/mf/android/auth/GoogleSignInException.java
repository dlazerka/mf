package me.lazerka.mf.android.auth;

import com.google.android.gms.common.api.Status;

/**
 * @author Dzmitry Lazerka
 */
class GoogleSignInException extends GoogleApiException {
	private final Status status;

	public GoogleSignInException(Status status) {
		super(status.getStatusMessage());
		this.status = status;
	}

	@Override
	public int getCode() {
		return status.getStatusCode();
	}

	public Status getStatus() {
		return status;
	}
}
