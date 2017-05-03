package de.appsist.service.ufs;

import org.vertx.java.core.AsyncResult;

public class Success<T> implements AsyncResult<T> {
	private final T result;
	
	public Success(T result) {
		this.result = result;
	}

	@Override
	public T result() {
		return result;
	}

	@Override
	public Throwable cause() {
		return null;
	}

	@Override
	public boolean succeeded() {
		return true;
	}

	@Override
	public boolean failed() {
		return false;
	}

}
