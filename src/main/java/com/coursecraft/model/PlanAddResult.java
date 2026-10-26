package com.coursecraft.model;

/*
 * Holds what POST /plan/add answers: HTTP status and message every time.
 * Only 200 also fills term units, unit status, risk level and open seats.
 * Risk level and open seats stay null when course has no score or no snapshot.
 */
public class PlanAddResult {

	private final int status;
	private final String message;
	private final String termLabel;
	private final double termUnits;
	private final String unitStatus;
	private final String riskLevel;
	private final Integer openSeats;

	// builds 400, 409 or 422 answer, nothing was saved
	public PlanAddResult(int status, String message) {
		this.status = status;
		this.message = message;
		this.termLabel = null;
		this.termUnits = 0;
		this.unitStatus = null;
		this.riskLevel = null;
		this.openSeats = null;
	}

	// builds 200 answer, row was saved
	public PlanAddResult(int status, String message, String termLabel, double termUnits,
			String unitStatus, String riskLevel, Integer openSeats) {
		this.status = status;
		this.message = message;
		this.termLabel = termLabel;
		this.termUnits = termUnits;
		this.unitStatus = unitStatus;
		this.riskLevel = riskLevel;
		this.openSeats = openSeats;
	}

	public int getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}

	public String getTermLabel() {
		return termLabel;
	}

	public double getTermUnits() {
		return termUnits;
	}

	public String getUnitStatus() {
		return unitStatus;
	}

	public String getRiskLevel() {
		return riskLevel;
	}

	public Integer getOpenSeats() {
		return openSeats;
	}
}
