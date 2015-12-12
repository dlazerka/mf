package me.lazerka.mf.android.auth;

import com.google.android.gms.common.ConnectionResult;

/**
 * @author Dzmitry Lazerka
 */
public class GoogleApiConnectionException extends GoogleApiException {
	private ConnectionResult connectionResult;

	public GoogleApiConnectionException(ConnectionResult connectionResult) {
		super(connectionResult.getErrorMessage());
		this.connectionResult = connectionResult;
	}

	@Override
	public int getCode() {
		return connectionResult.getErrorCode();
	}

	public ConnectionResult getConnectionResult() {
		return connectionResult;
	}
}
