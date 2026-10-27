package com.coursecraft.model;

import java.util.ArrayList;
import java.util.List;

/*
 * Holds one prerequisite group that student has not met yet with completed course or placement.
 * Course needs every group, any one option inside group is enough (team rule 2).
 * Options list courses that count towards group, placement options are read only for messages.
 */
public class PrerequisiteGroup {

	private final int groupId;
	private final int courseId;
	private final List<Integer> optionCourseIds = new ArrayList<>();
	private final List<String> optionCodes = new ArrayList<>();

	public PrerequisiteGroup(int groupId, int courseId) {
		this.groupId = groupId;
		this.courseId = courseId;
	}

	public int getGroupId() {
		return groupId;
	}

	// gives course that needs this group, like CS 146 for group "CS 46B or CS 49J"
	public int getCourseId() {
		return courseId;
	}

	// gives option codes like "CS 46B", sorted by subject and number
	public List<String> getOptionCodes() {
		return optionCodes;
	}

	public List<Integer> getOptionCourseIds() {
		return optionCourseIds;
	}

	// PrerequisiteDao calls this once per counts_towards row
	public void addOption(int optionCourseId, String optionCode) {
		optionCourseIds.add(optionCourseId);
		optionCodes.add(optionCode);
	}

	// tells if course counts towards this group
	public boolean hasOption(int optionCourseId) {
		return optionCourseIds.contains(optionCourseId);
	}
}
