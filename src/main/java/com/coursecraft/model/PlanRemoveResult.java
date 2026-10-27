package com.coursecraft.model;

// holds what POST /plan/remove answers: HTTP status (200, 400, 404 or 409) and message for student
public class PlanRemoveResult {

	private final int status;
	private final String message;

	public PlanRemoveResult(int status, String message) {
		this.status = status;
		this.message = message;
	}

	public int getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}
}
