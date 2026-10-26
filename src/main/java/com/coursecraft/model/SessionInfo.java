package com.coursecraft.model;

/*
 * Holds one row of sessions table joined with its term.
 * Name is SessionInfo, not Session, so nobody mixes it up with login session.
 * Label reads like "Fall 2026" or "Summer 2026 (Summer Session 1)".
 * Term label is always just "Summer 2026", since unit totals count whole term.
 */
public class SessionInfo {

	private final int sessionId;
	private final int termId;
	private final String season;
	private final String label;
	private final String termLabel;

	public SessionInfo(int sessionId, int termId, String season, String label, String termLabel) {
		this.sessionId = sessionId;
		this.termId = termId;
		this.season = season;
		this.label = label;
		this.termLabel = termLabel;
	}

	public int getSessionId() {
		return sessionId;
	}

	public int getTermId() {
		return termId;
	}

	public String getSeason() {
		return season;
	}

	public String getLabel() {
		return label;
	}

	public String getTermLabel() {
		return termLabel;
	}
}
