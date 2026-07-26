package services.export;

import java.util.List;

/**
 * Wrapper for a numeric JSON array that may contain null entries, used for answer-option score
 * arrays.
 *
 * <p>The JSON export dispatch ({@code
 * JsonExporterService#exportApplicationEntriesToJsonApplication}) silently drops values that
 * aren't one of its known types, and Guava's {@link com.google.common.collect.ImmutableList}
 * rejects nulls outright, so null-holed score arrays cross the dispatch inside this explicit
 * wrapper and are written with null positions preserved.
 */
public record NullableLongArray(List<Long> values) {}
