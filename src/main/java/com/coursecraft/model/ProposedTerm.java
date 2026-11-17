package com.coursecraft.model;

import java.util.ArrayList;
import java.util.List;

public class ProposedTerm {

	private final int termId;
	private final String label;
	private final double units;
	private final String unitStatus;
	private final double unitCap;
	private final List<ProposedEntry> entries = new ArrayList<>();
	private final List<String> warnings = new ArrayList<>();

	public ProposedTerm(int termId, String label, double units, String unitStatus, double unitCap) {
		this.termId = termId;
		this.label = label;
		this.units = units;
		this.unitStatus = unitStatus;
		this.unitCap = unitCap;
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

	public String getUnitStatus() {
		return unitStatus;
	}

	public double getUnitCap() {
		return unitCap;
	}

	public List<ProposedEntry> getEntries() {
		return entries;
	}

	public List<String> getWarnings() {
		return warnings;
	}
}
