package vn.edu.uit.nearby.nearby_backend.identity.domain;

import java.util.Locale;
import java.util.regex.Pattern;

import vn.edu.uit.nearby.nearby_backend.shared.constant.ErrorCodes;
import vn.edu.uit.nearby.nearby_backend.shared.error.InvalidRequestException;

public record EmailAddress(String value) {

	private static final int MAX_LENGTH = 255;

	private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

	public EmailAddress {
		value = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
		if (value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches()) {
			throw new InvalidRequestException(ErrorCodes.INVALID_EMAIL, "The email address is not valid.");
		}
	}

	public static EmailAddress of(String value) {
		return new EmailAddress(value);
	}

	public String localPart() {
		return value.substring(0, value.indexOf('@'));
	}

}
