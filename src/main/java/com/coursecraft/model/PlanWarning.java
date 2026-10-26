package com.coursecraft.model;

/*
 * Holds one warning shown under term in plan card, like "Petition required" or "Part-time".
 * Key stays same while warning means same thing, so browser can remember student minimized it.
 * Kind is "petition", "part-time" or "depends", CSS picks color from it.
 */
public class PlanWarning {

	private final String key;
	private final String kind;
	private final String title;
	private final String text;

	public PlanWarning(String key, String kind, String title, String text) {
		this.key = key;
		this.kind = kind;
		this.title = title;
		this.text = text;
	}

	public String getKey() {
		return key;
	}

	public String getKind() {
		return kind;
	}

	public String getTitle() {
		return title;
	}

	public String getText() {
		return text;
	}
}
