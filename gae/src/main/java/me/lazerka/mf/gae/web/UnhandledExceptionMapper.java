/*
 *     Find Us: privacy oriented location tracker for your friends and family.
 *     Copyright (C) 2015 Dzmitry Lazerka dlazerka@gmail.com
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package me.lazerka.mf.gae.web;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import java.net.URI;

/**
 * Handle various exceptions.
 *
 * Note that if you throw a WebApplicationException WITH Response, it doesn't come here.
 *
 * @author Dzmitry Lazerka
 */
@Singleton
@javax.ws.rs.ext.Provider
public class UnhandledExceptionMapper implements ExceptionMapper<Exception> {
	private static final Logger logger = LoggerFactory.getLogger(UnhandledExceptionMapper.class);

	@Inject
	Provider<UriInfo> uriInfoProvider;

	@Inject
	Provider<Request> requestProvider;

	@Override
	public Response toResponse(Exception exception) {
		String method = requestProvider.get().getMethod();
		URI uri = uriInfoProvider.get().getRequestUri();
		String head = method + ' ' + uri;

		if (exception instanceof WebApplicationException) {
			Response response = ((WebApplicationException) exception).getResponse();
			if (response.getStatus() == 404) {
				logger.warn("{} Not Found", head);
			} else {
				logger.warn("{} WebApplicationException: {} {}",
						head,
						response.getStatus(),
						response.getEntity() == null ? "" : response.getEntity());
			}

			return response;
		}

		if (exception instanceof UnrecognizedPropertyException) {
			logger.warn("{}: {}", head, exception.getMessage());
			return Response.status(Status.BAD_REQUEST)
					.entity(exception.getMessage())
					.type(MediaType.TEXT_PLAIN)
					.build();
		}

		logger.error("{}: Unhandled exception", head, exception);

		return Response.status(Status.INTERNAL_SERVER_ERROR)
				.entity(exception.getMessage())
				.type(MediaType.TEXT_PLAIN)
				.build();
	}
}
