package vn.edu.uit.nearby.nearby_backend.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import vn.edu.uit.nearby.nearby_backend.shared.error.InvalidRequestException;
import vn.edu.uit.nearby.nearby_backend.shared.web.CursorPage.Cursor;

class CursorPageTests {

	@Test
	void cursorSurvivesEncodeDecodeRoundTrip() {
		Cursor cursor = new Cursor(Instant.ofEpochMilli(1_760_000_000_123L), UUID.randomUUID());

		assertThat(Cursor.decode(cursor.encode())).contains(cursor);
	}

	@Test
	void pageDropsTheExtraRowAndPointsNextCursorAtTheLastReturnedItem() {
		List<Cursor> fetched = List.of(cursorAt(3), cursorAt(2), cursorAt(1));

		CursorPage<Cursor> page = CursorPage.of(fetched, 2, cursor -> cursor);

		assertThat(page.items()).containsExactly(fetched.get(0), fetched.get(1));
		assertThat(page.hasMore()).isTrue();
		assertThat(Cursor.decode(page.nextCursor())).contains(fetched.get(1));
	}

	@Test
	void pageWithExactlyLimitRowsIsTheLastPage() {
		CursorPage<Cursor> page = CursorPage.of(List.of(cursorAt(2), cursorAt(1)), 2, cursor -> cursor);

		assertThat(page.hasMore()).isFalse();
		assertThat(page.nextCursor()).isNull();
	}

	@Test
	void emptyResultIsAnEmptyLastPage() {
		CursorPage<Cursor> page = CursorPage.of(List.of(), 20, cursor -> cursor);

		assertThat(page.items()).isEmpty();
		assertThat(page.hasMore()).isFalse();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "   " })
	void missingCursorMeansFirstPage(String token) {
		assertThat(Cursor.decode(token)).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "not-a-cursor", "bm8tc2VwYXJhdG9y", "MTIzfG5vdC1hLXV1aWQ" })
	void malformedCursorIsRejected(String token) {
		assertThatThrownBy(() -> Cursor.decode(token)).isInstanceOf(InvalidRequestException.class);
	}

	@ParameterizedTest
	@CsvSource(nullValues = "null", value = { "null,20", "0,20", "-5,20", "1,1", "100,100", "101,100" })
	void limitIsClampedBetweenOneAndMax(Integer requested, int expected) {
		assertThat(CursorPage.clampLimit(requested)).isEqualTo(expected);
	}

	private static Cursor cursorAt(long epochMilli) {
		return new Cursor(Instant.ofEpochMilli(epochMilli), UUID.randomUUID());
	}

}
