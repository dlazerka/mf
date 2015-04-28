package me.lazerka.mf.gae.entity;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

/**
 * @author Dzmitry Lazerka
 */
@Cache
@Entity(name = "AcraException")
public class AcraExceptionEntity {
	@Id
	String id;

	@Index
	long recMs;

	String message;
	String report;

	public AcraExceptionEntity() {}

	public AcraExceptionEntity(String id, String message, String report) {
		this.id = id;
		this.message = message;
		this.report = report;
		this.recMs = System.currentTimeMillis();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getMessage() {
		return message;
	}

	public String getReport() {
		return report;
	}
}
