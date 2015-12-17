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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Bypasses _ah/* requests except some.
 *
 * @author Dzmitry Lazerka
 */
public class GuiceFilter extends com.google.inject.servlet.GuiceFilter {
	private final static Logger logger = LoggerFactory.getLogger(GuiceFilter.class);

	private final Pattern NOT_BYPASS = Pattern.compile(
			"^(/_ah/warmup)" +
			"|(/_ah/upload/.*)" +
			"|(/_ah/channel/connected/.*)" +
			"|(/_ah/channel/disconnected/.*)"
	);

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (request instanceof HttpServletRequest) {
			HttpServletRequest req = (HttpServletRequest) request;

			// Break the chain for dev server except warmup (must be handled by app).
			String requestUri = req.getRequestURI();
			boolean isAhRequest = requestUri.startsWith("/_ah/");

			if (!isAhRequest) {
				logger.trace("Handle with Guice: {}", requestUri);
				super.doFilter(request, response, chain);
				return;
			}

			if (shouldNotBypass(requestUri)) {
				logger.info("Not bypassing Guice: {} {}", req.getMethod(), requestUri);
				super.doFilter(request, response, chain);
				return;
			}

			logger.trace("Bypassing Guice: {} {}", req.getMethod(), requestUri);
			chain.doFilter(request, response);
		} else {
			logger.warn("Got non-HTTP request (length={})", request.getContentLength());
			chain.doFilter(request, response);
		}
	}

	private boolean shouldNotBypass(String requestUri) {
		return NOT_BYPASS.matcher(requestUri).matches();
	}
}
