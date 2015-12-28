package handy.storage.api;

import android.text.TextUtils;

/**
 * Represents SQLite function.
 */
public enum Function {

	/**
	 * Absolute value. Has a single parameter.
	 */
	ABS,

	/**
	 * Average value. Has a single parameter.
	 */
	AVG,

	/**
	 * A count of the number of times that X is not NULL in a group. Has one or
	 * zero parameters (an equivalent of COUNT(*)).
	 */
	COUNT,

	/**
	 * Maximum. If it is used as an aggregation function, it has a single
	 * parameter, otherwise it has two or more parameters.
	 */
	MAX,

	/**
	 * Minimum. If it is used as an aggregation function, it has a single
	 * parameter, otherwise it has two or more parameters.
	 */
	MIN,

	/**
	 * Sum of values. Has a single parameter.
	 */
	TOTAL;

	/**
	 * Returns the SQLite's representation of this function with passed
	 * <code>arguments</code>.
	 */
	public String toSQLEntity(String... arguments) {
		if (arguments == null) {
			return toSQLEntity();
		}
		if (this == COUNT && arguments.length == 0) {
			return "COUNT(*)";
		}
		return String.format("%s(%s)", name(), TextUtils.join(", ", arguments));
	}

}
