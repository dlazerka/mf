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

package me.lazerka.mf.gae.entity;

import com.googlecode.objectify.Work;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * INSERT IF NOT EXISTS an entity, or returns existing entity.
 *
 * @author Dzmitry Lazerka
 */
public class CreateOrFetch<T> implements Work<T> {
	private final T newEntity;

	public CreateOrFetch(T newEntity) {
		this.newEntity = checkNotNull(newEntity);
	}

	@Override
	public T run() {
		T existingEntity = ofy().load().entity(newEntity).now();
		if (existingEntity != null) {
			return existingEntity;
		}
		ofy().save().entity(newEntity).now();
		return newEntity;
	}
}
