package com.coursecraft.model;

/*
 * Holds one plan_entries row with its course and term, for plan card in catalog rail.
 * Petition fields stay null unless course depends on petition term (see PrerequisiteCheck).
 */
public class PlannedCourse {

	private final int planEntryId;
	private final int courseId;
	private final String code;
	private final String title;
	private final double units;
	private final int termId;
	private final String termLabel;
	private final String season;
	private final int termOrder;
	private final String riskLevel;
	private String petitionTermLabel;
	private String throughCourses;

	public PlannedCourse(int planEntryId, int courseId, String code, String title, double units, int termId,
			String termLabel, String season, int termOrder, String riskLevel) {
		this.planEntryId = planEntryId;
		this.courseId = courseId;
		this.code = code;
		this.title = title;
		this.units = units;
		this.termId = termId;
		this.termLabel = termLabel;
		this.season = season;
		this.termOrder = termOrder;
		this.riskLevel = riskLevel;
	}

	// gives plan_entries key, My plan Remove button sends it to POST /plan/remove
	public int getPlanEntryId() {
		return planEntryId;
	}

	public int getCourseId() {
		return courseId;
	}

	public String getCode() {
		return code;
	}

	// gives full name like "Data Structures and Algorithms", My plan page shows it next to code
	public String getTitle() {
		return title;
	}

	public double getUnits() {
		return units;
	}

	public int getTermId() {
		return termId;
	}

	public String getTermLabel() {
		return termLabel;
	}

	public String getSeason() {
		return season;
	}

	// gives number that sorts terms in time order, like 20264 for Fall 2026
	public int getTermOrder() {
		return termOrder;
	}

	// gives "Low", "Medium" or "High", null when course is not scored for this term
	public String getRiskLevel() {
		return riskLevel;
	}

	// gives "Fall 2026" when this course needs petition term to be approved, null otherwise
	public String getPetitionTermLabel() {
		return petitionTermLabel;
	}

	// gives earlier planned courses that link this course to petition term, like "CS 46B, MATH 30"
	public String getThroughCourses() {
		return throughCourses;
	}

	// PrerequisiteCheck calls this when course needs course from petition term
	public void setPetitionDependency(String petitionTermLabel, String throughCourses) {
		this.petitionTermLabel = petitionTermLabel;
		this.throughCourses = throughCourses;
	}
}
