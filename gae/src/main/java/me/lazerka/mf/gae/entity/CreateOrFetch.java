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
