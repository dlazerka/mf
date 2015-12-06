package me.lazerka.mf.gae.oauth;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * @author Dzmitry Lazerka
 */
public interface TokenVerifier {
	UserPrincipal verify(String authToken) throws IOException, GeneralSecurityException;
}
