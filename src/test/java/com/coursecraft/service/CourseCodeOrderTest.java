package com.coursecraft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

// no database needed, only course codes
class CourseCodeOrderTest {

	@Test
	void sortsBySubjectAndCourseNumber() {
		List<String> codes = new ArrayList<>(List.of("MATH 42", "CS 146", "CS 46B", "CS 100W", "CS 46A"));
		codes.sort(new CourseCodeOrder());
		assertEquals(List.of("CS 46A", "CS 46B", "CS 100W", "CS 146", "MATH 42"), codes);
	}

	@Test
	void handlesOddInputsAndEqualCodes() {
		CourseCodeOrder order = new CourseCodeOrder();
		assertTrue(order.compare("CS", "MATH") < 0);
		assertTrue(order.compare("CS ABC", "CS XYZ") < 0);
		assertTrue(order.compare("CS 46A", "CS ABC") < 0);
		assertEquals(0, order.compare("CS 46A", "CS 46A"));
	}
}
