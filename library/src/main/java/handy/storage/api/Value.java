package handy.storage.api;

import android.text.TextUtils;

import handy.storage.Table;

/**
 * Value from a database (either a column or a result of SQL function).
 */
public class Value {

	private final String entity;
	private String alias;

	Value(String entity) {
		this.entity = entity;
	}

	public String getEntity() {
		return entity;
	}

	public String getAlias() {
		return alias;
	}

	/**
	 * Sets an alias for this value.
	 */
	void setAlias(String alias) {
		this.alias = alias;
	}

	public String getName() {
		return TextUtils.isEmpty(alias) ? entity : alias;
	}

	/**
	 * Creates a {@link Value} object representing a column or SQL entity.
	 */
	public static Value of(String columnOrEntity) {
		return new Value(columnOrEntity);
	}

	/**
	 * Creates a {@link Value} object representing a column.
	 */
	public static Value of(String table, String column) {
		return new Value(Table.fullColumnName(table, column));
	}

}
