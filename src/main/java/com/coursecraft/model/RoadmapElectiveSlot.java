package com.coursecraft.model;

import com.coursecraft.service.UnitRules;

// holds one elective placeholder slot in a roadmap term, either open or filled by a completed/planned course
public class RoadmapElectiveSlot {

	private final int slotId;
	private final int groupId;
	private final String groupName;
	private final int suggestedTerm;
	private final double slotUnits;
	private final RoadmapCourse filledCourse;

	public RoadmapElectiveSlot(int slotId, int groupId, String groupName, int suggestedTerm,
			double slotUnits, RoadmapCourse filledCourse) {
		this.slotId = slotId;
		this.groupId = groupId;
		this.groupName = groupName;
		this.suggestedTerm = suggestedTerm;
		this.slotUnits = slotUnits;
		this.filledCourse = filledCourse;
	}

	public int getSlotId() {
		return slotId;
	}

	public int getGroupId() {
		return groupId;
	}

	public String getGroupName() {
		return groupName;
	}

	public int getSuggestedTerm() {
		return suggestedTerm;
	}

	public double getSlotUnits() {
		return slotUnits;
	}

	public String getSlotUnitsText() {
		return UnitRules.format(slotUnits);
	}

	public RoadmapCourse getFilledCourse() {
		return filledCourse;
	}

	public boolean isFilled() {
		return filledCourse != null;
	}
}