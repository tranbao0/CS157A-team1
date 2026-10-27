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
	private final int termOrder;

	public SessionInfo(int sessionId, int termId, String season, String label, String termLabel, int termOrder) {
		this.sessionId = sessionId;
		this.termId = termId;
		this.season = season;
		this.label = label;
		this.termLabel = termLabel;
		this.termOrder = termOrder;
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

	// gives number that sorts terms in time order, like 20264 for Fall 2026
	public int getTermOrder() {
		return termOrder;
	}
}
