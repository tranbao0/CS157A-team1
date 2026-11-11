package com.coursecraft.service;

import java.math.BigInteger;
import java.util.Comparator;

/*
 * Puts course codes in order student expects: subject first, then number, then letters after number.
 * Example: "CS 46A", "CS 46B", "CS 100W", "CS 146", "MATH 42".
 * Code with no space or no number falls back to text order.
 */
public class CourseCodeOrder implements Comparator<String> {

	@Override
	public int compare(String first, String second) {
		int firstSpace = first.indexOf(' ');
		int secondSpace = second.indexOf(' ');
		int firstEnd = numberEnd(first, firstSpace);
		int secondEnd = numberEnd(second, secondSpace);
		String firstSubject = first;
		String secondSubject = second;
		if (firstEnd > 0) {
			firstSubject = first.substring(0, firstSpace);
		}
		if (secondEnd > 0) {
			secondSubject = second.substring(0, secondSpace);
		}
		int order = firstSubject.compareTo(secondSubject);
		if (order != 0) {
			return order;
		}
		if (firstEnd > 0 && secondEnd > 0) {
			// BigInteger keeps long course numbers within numeric order
			BigInteger firstNumber = new BigInteger(first.substring(firstSpace + 1, firstEnd));
			BigInteger secondNumber = new BigInteger(second.substring(secondSpace + 1, secondEnd));
			order = firstNumber.compareTo(secondNumber);
			if (order != 0) {
				return order;
			}
			order = first.substring(firstSpace + 1).compareTo(second.substring(secondSpace + 1));
			if (order != 0) {
				return order;
			}
		}
		return first.compareTo(second);
	}

	// zero means code has no space or no leading digits
	private int numberEnd(String code, int space) {
		if (space < 0) {
			return 0;
		}
		int end = space + 1;
		for (int i = end; i < code.length(); i++) {
			if (!Character.isDigit(code.charAt(i))) {
				break;
			}
			end = i + 1;
		}
		if (end == space + 1) {
			return 0;
		}
		return end;
	}
}
