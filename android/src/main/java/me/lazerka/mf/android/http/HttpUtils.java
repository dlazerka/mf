package me.lazerka.mf.android.http;

import android.util.Log;
import com.android.volley.NetworkResponse;
import com.android.volley.toolbox.HttpHeaderParser;
import org.slf4j.Logger;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

/**
 * Only pure functions here (except for {@link Log}).
 *
 * @author Dzmitry Lazerka
 */
public class HttpUtils {

	/**
	 * Converts response from binary to {@link String, using charset from hearders.
	 */
	public static String decodeNetworkResponseCharset(NetworkResponse networkResponse, Logger logger) {

		byte[] data = networkResponse.data;
		if (data == null || data.length == 0) {
			return "";
		}

		String charsetName = HttpHeaderParser.parseCharset(networkResponse.headers);
		Charset charset;
		try {
			charset = Charset.forName(charsetName);
		} catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
			logger.warn("Unable to find charset by name: " + charsetName);
			charset = StandardCharsets.UTF_8;
		}
		return new String(data, charset);
	}
}
