package handy.storage.api;

import handy.storage.annotation.AutoIncrement;
import handy.storage.annotation.Column;
import handy.storage.annotation.PrimaryKey;

/**
 * Base {@link Model} implementation with a rowid (i.e. a column of type
 * <code>long</code>, which is an auto incrementing primary key, named
 * {@link UniqueObject#ID}). If you use this class, don't disable usage of
 * column from super classes (via
 * {@link handy.storage.HandyStorage.Builder#setUseColumnsFromSuperclasses(boolean)} method.
 */
public class UniqueObject implements Model {

	/**
	 * Name of the rowid column.
	 */
	public static final String ID = "id";

	@Column(ID)
	@PrimaryKey
	@AutoIncrement
	protected long id;

	/**
	 * Returns the rowid of this object or <code>0</code> if it has not been
	 * inserted into the database yet.
	 */
	public long getId() {
		return id;
	}

	/**
	 * Sets the rowid value.
	 *
	 * @param id new id
	 */
	protected void setId(long id) {
		this.id = id;
	}

	/**
	 * Returns whether the object has a valid id set.
	 */
	protected boolean isIdSet() {
		return id > 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!isIdSet()) {
			return super.equals(o);
		}
		return o != null && getClass() == o.getClass() && id == ((UniqueObject) o).id;

	}

	@Override
	public int hashCode() {
		if (isIdSet()) {
			final int shift = 32;
			return (int) (id ^ (id >>> shift));
		} else {
			return super.hashCode();
		}
	}
}
