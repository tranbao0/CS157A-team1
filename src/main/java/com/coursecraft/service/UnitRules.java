package com.coursecraft.service;

/*
 * Holds SJSU unit rules for undergraduates, checked on Registrar pages 2026-10-25.
 * Fall and Spring allow 19 units, or up to 21 with approved Excess Units Petition.
 * Summer allows 12 units and Winter allows 4.
 * Full-time is 12 or more, three-quarter time 9 to 11, half-time 6 to 8, less than half-time under 6.
 * Team decided plan add blocks only above 21, so 20 and 21 get loud petition warning instead.
 */
public final class UnitRules {

	public static final double NORMAL_LIMIT = 19;
	public static final double PETITION_LIMIT = 21;
	public static final double SUMMER_LIMIT = 12;
	public static final double WINTER_LIMIT = 4;
	public static final double FULL_TIME = 12;

	private UnitRules() {
	}

	// gives highest unit total plan add allows in one term of this season
	public static double capFor(String season) {
		if ("Summer".equals(season)) {
			return SUMMER_LIMIT;
		}
		if ("Winter".equals(season)) {
			return WINTER_LIMIT;
		}
		return PETITION_LIMIT;
	}

	/*
	 * Gives short status word for term total, browser picks look and text from it.
	 * Summer and Winter have no full-time tiers on Registrar page, so they get "short-term".
	 */
	public static String statusFor(String season, double units) {
		if ("Summer".equals(season) || "Winter".equals(season)) {
			return "short-term";
		}
		if (units > NORMAL_LIMIT) {
			return "petition";
		}
		if (units >= FULL_TIME) {
			return "full-time";
		}
		if (units >= 9) {
			return "three-quarter";
		}
		if (units >= 6) {
			return "half-time";
		}
		return "under-half";
	}

	// builds sentence about term total that student sees right after add
	public static String describe(String termLabel, String season, double units) {
		String total = termLabel + " now has " + format(units) + " units.";
		String status = statusFor(season, units);

		if ("short-term".equals(status)) {
			return total + " SJSU allows up to " + format(capFor(season)) + " units in "
					+ season.toLowerCase() + ".";
		}
		if ("petition".equals(status)) {
			return total + " PETITION REQUIRED: SJSU allows " + format(NORMAL_LIMIT)
					+ " units per semester. You must get Excess Units Petition approved to take more,"
					+ " up to " + format(PETITION_LIMIT) + ". If petition is not approved, this plan does not work.";
		}
		if ("full-time".equals(status)) {
			return total + " You are full-time.";
		}
		if ("three-quarter".equals(status)) {
			return total + " Under 12 units, SJSU counts you as part-time (three-quarter time) for this semester.";
		}
		if ("half-time".equals(status)) {
			return total + " Under 12 units, SJSU counts you as part-time (half-time) for this semester.";
		}
		return total + " Under 12 units, SJSU counts you as part-time (less than half-time) for this semester.";
	}

	// gives warning title for plan card, null when status needs no warning (full-time, summer, winter)
	public static String warningTitle(String status) {
		if ("petition".equals(status)) {
			return "Petition required";
		}
		if ("three-quarter".equals(status) || "half-time".equals(status) || "under-half".equals(status)) {
			return "Part-time";
		}
		return null;
	}

	/*
	 * Builds warning text for plan card.
	 * Dependent count is how many later planned courses need this term, only petition text uses it.
	 * Financial aid and F-1 note comes from Registrar enrollment status page and ISSS course load page.
	 */
	public static String warningText(String status, double units, int dependentCount) {
		if ("petition".equals(status)) {
			String text = format(units) + " units is over SJSU's " + format(NORMAL_LIMIT) + "-unit limit."
					+ " You must file an Excess Units Petition and get it approved (" + format(PETITION_LIMIT)
					+ " units at most). If the petition is denied, this term does not work";
			if (dependentCount == 1) {
				return text + ", and 1 later course in your plan breaks too.";
			}
			if (dependentCount > 1) {
				return text + ", and " + dependentCount + " later courses in your plan break too.";
			}
			return text + ".";
		}

		String tier = "less than half-time";
		if ("three-quarter".equals(status)) {
			tier = "three-quarter time";
		} else if ("half-time".equals(status)) {
			tier = "half-time";
		}
		return "Under " + format(FULL_TIME) + " units, SJSU counts you as part-time (" + tier
				+ ") for this semester. Part-time can affect financial aid and F-1 visa status.";
	}

	// builds 409 sentence when course would push term over cap
	public static String capMessage(String courseCode, String termLabel, String season, double newTotal) {
		String start = "Adding " + courseCode + " would put " + termLabel + " at " + format(newTotal) + " units.";
		if ("Summer".equals(season) || "Winter".equals(season)) {
			return start + " SJSU allows at most " + format(capFor(season)) + " units in "
					+ season.toLowerCase() + ".";
		}
		return start + " SJSU allows at most " + format(PETITION_LIMIT) + " units per semester,"
				+ " and anything above " + format(NORMAL_LIMIT) + " needs approved Excess Units Petition.";
	}

	// writes 20.0 as "20" and 1.5 as "1.5"
	public static String format(double units) {
		if (units == Math.floor(units)) {
			return String.valueOf((int) units);
		}
		return String.valueOf(units);
	}
}
