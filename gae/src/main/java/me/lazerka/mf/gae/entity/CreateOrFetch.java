/*
 *     Copyright (C) 2017 Dzmitry Lazerka
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
