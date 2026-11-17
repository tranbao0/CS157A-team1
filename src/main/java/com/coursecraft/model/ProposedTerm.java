package com.coursecraft.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import com.coursecraft.service.UnitRules;

public class ProposedTerm {

	private final int termId;
	private final String label;
	private double units;
	private final String season;
	private final List<ProposedEntry> entries = new ArrayList<>();
	private final List<String> warnings = new ArrayList<>();

	public ProposedTerm(int termId, String label, String season) {
		this.termId = termId;
		this.label = label;
		this.season = season;
	}

	public void addEntry(ProposedEntry entry) {
		entries.add(entry);
		units = units + entry.getUnits();
	}

	public void addPlannedUnits(double plannedUnits) {
		units = units + plannedUnits;
	}

	public int getTermId() {
		return termId;
	}

	public String getLabel() {
		return label;
	}

	public double getUnits() {
		return units;
	}

	public String getUnitsText() {
		return UnitRules.format(units);
	}

	public String getUnitWarningTitle() {
		return UnitRules.warningTitle(getUnitStatus());
	}

	public String getUnitWarningText() {
		if (getUnitWarningTitle() == null) {
			return null;
		}
		return UnitRules.warningText(getUnitStatus(), units, 0);
	}

	public String getUnitWarningKind() {
		if (getUnitWarningTitle() == null) {
			return null;
		}
		if ("petition".equals(getUnitStatus())) {
			return "petition";
		}
		return "part-time";
	}

	public String getUnitStatus() {
		return UnitRules.statusFor(season, units);
	}

	public double getUnitCap() {
		if ("Fall".equals(season) || "Spring".equals(season)) {
			return UnitRules.NORMAL_LIMIT;
		}
		return UnitRules.capFor(season);
	}

	public List<ProposedEntry> getEntries() {
		return Collections.unmodifiableList(entries);
	}

	public List<String> getWarnings() {
		return warnings;
	}
}
