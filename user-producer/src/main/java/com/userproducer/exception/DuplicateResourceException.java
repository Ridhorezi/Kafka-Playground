package com.userproducer.exception;

public class DuplicateResourceException extends RuntimeException {
	public DuplicateResourceException(String message) {
		super(message);
	}
}