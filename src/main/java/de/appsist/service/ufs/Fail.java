package de.appsist.service.ufs;

import org.vertx.java.core.AsyncResult;

public class Fail<T> implements AsyncResult<T> {
	private final Throwable cause;
	
	public Fail(Throwable cause) {
		this.cause = cause;
	}

	@Override
	public T result() {
		return null;
	}

	@Override
	public Throwable cause() {
		return cause;
	}

	@Override
	public boolean succeeded() {
		return false;
	}

	@Override
	public boolean failed() {
		return true;
	}

}
