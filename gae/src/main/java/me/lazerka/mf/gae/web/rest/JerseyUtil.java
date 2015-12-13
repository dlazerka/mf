package me.lazerka.mf.gae.web.rest;

import javax.annotation.Nonnull;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

public class JerseyUtil {
	@Nonnull
	public static <T> T throwIfNull(T obj, String field) throws WebApplicationException {
		if (obj == null) {
			throw new WebApplicationException(
					Response.status(BAD_REQUEST)
							.entity(field + " is null")
							.build());
		}

		return obj;
	}

}
