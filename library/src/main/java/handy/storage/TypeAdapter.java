package handy.storage;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.google.gson.Gson;

import java.util.Calendar;
import java.util.Date;

import handy.storage.api.ColumnType;

/**
 * <p>Manages transformations of values of some type during writing to or reading
 * from the database.</p>
 * <p/>
 * <p>Please note, that equal objects should be converted to equal database values and otherwise.
 * If this rule is broken, it might lead to unexpected results.</p>
 *
 * @param <T> type
 */
abstract class TypeAdapter<T> {

	/**
	 * This method should transform a passed <code>value</code> to a value that
	 * should be stored to the database, and then put this new value in the
	 * <code>cv</code> with passed <code>key</code> using an appropriate
	 * <code>ContentValues.put()</code> method.
	 *
	 * @param cv    ContentValues to put in
	 * @param key   key to use
	 * @param value value to store
	 */
	protected abstract void putValue(ContentValues cv, String key, T value);

	/**
	 * This method should get a value (stored by
	 * {@link #putValue(ContentValues, String, Object)}) from the cursor at the
	 * passed index and transform it to an appropriate type's value. A value at
	 * this index is guaranteed to be not <code>null</code>.
	 *
	 * @param cursor      cursor to read from
	 * @param columnIndex index of a value in the cursor
	 */
	protected abstract T getValue(Cursor cursor, int columnIndex);

	/**
	 * Determines the column's type to use in tables declarations for columns
	 * having this type.
	 */
	protected abstract ColumnType getColumnType();

	/**
	 * Converts value to string representation of value stored in database. For
	 * example, if <code>true</code> is stored in database as 1, this method
	 * convert boolean true to string "1". Applied for  values
	 * in expressions. Default implementation just return the {@link String#valueOf(Object)}.
	 */
	protected String convertValue(Object value) {
		return String.valueOf(value);
	}

	@SuppressWarnings("unchecked")
	final void putValueObject(ContentValues cv, String key, Object value) {
		putValue(cv, key, (T) value);
	}

	/**
	 * Realization for byte.
	 */
	static final TypeAdapter<Byte> BYTE_TYPE_ADAPTER = new TypeAdapter<Byte>() {

		@Override
		public void putValue(ContentValues cv, String key, Byte value) {
			cv.put(key, value);
		}

		@Override
		public Byte getValue(Cursor cursor, int columnIndex) {
			return (byte) cursor.getShort(columnIndex);
		}

		@Override
		public ColumnType getColumnType() {
			return ColumnType.INTEGER;
		}

	};

	/**
	 * Realization for short.
	 */
	static final TypeAdapter<Short> SHORT_TYPE_ADAPTER = new TypeAdapter<Short>() {

		@Override
		public void putValue(ContentValues cv, String key, Short value) {
			cv.put(key, value);
		}

		@Override
		public Short getValue(Cursor cursor, int columnIndex) {
			return cursor.getShort(columnIndex);
		}

		@Override
		public ColumnType getColumnType() {
			return ColumnType.INTEGER;
		}

	};

	/**
	 * Realization for int.
	 */
	static final TypeAdapter<Integer> INT_TYPE_ADAPTER = new TypeAdapter<Integer>() {

		@Override
		public void putValue(ContentValues cv, String key, Integer value) {
			cv.put(key, value);
		}

		@Override
		public Integer getValue(Cursor cursor, int columnIndex) {
			return cursor.getInt(columnIndex);
		}

		@Override
		public ColumnType getColumnType() {
			return ColumnType.INTEGER;
		}

	};

	/**
	 * Realization for long.
	 */
	static final TypeAdapter<Long> LONG_TYPE_ADAPTER = new TypeAdapter<Long>() {

		@Override
		public void putValue(ContentValues cv, String key, Long value) {
			cv.put(key, value);
		}

		@Override
		public Long getValue(Cursor cursor, int columnIndex) {
			return cursor.getLong(columnIndex);
		}

		@Override
		public ColumnType getColumnType() {
			return ColumnType.INTEGER;
		}

	};

	/**
	 * Realization for boolean.
	 */
	static final TypeAdapter<Boolean> BOOLEAN_TYPE_ADAPTER = new TypeAdapter<Boolean>() {

		@Override
		public void putValue(ContentValues cv, String key, Boolean value) {
			cv.put(key, value ? 1 : 0);
		}

		@Override
		public Boolean getValue(Cursor cursor, int columnIndex) {
			return cursor.getInt(columnIndex) != 0;
		}

		@Override
		public ColumnType getColumnType() {
			return ColumnType.INTEGER;
		}

		@Override
		public String convertValue(Object value) {
			String sValue = String.valueOf(value);
			if ("true".equalsIgnoreCase(sValue)) {
				return "1";
			} else if ("false".equalsIgnoreCase(sValue)) {
				return "0";
			} else {
				return super.convertValue(value);
			}
		}

	};

	/**
	 * Realization for String.
	 */
	static final TypeAdapter<String> STRING_TYPE_ADAPTER = new TypeAdapter<String>() {

		@Override
		public void putValue(ContentValues cv, String key, String value) {
			cv.put(key, value);
		}

		@Override
		public String getValue(Cursor cursor, int columnIndex) {
			return cursor.getString(columnIndex);
		}

		@Override
		public ColumnType getColumnType() {
			return ColumnType.TEXT;
		}

	};

	/**
	 * Realization for bytes array.
	 */
	static final TypeAdapter<byte[]> BLOB_TYPE_ADAPTER = new TypeAdapter<byte[]>() {

		@Override
		public void putValue(ContentValues cv, String key, byte[] value) {
			cv.put(key, value);
		}

		@Override
		public byte[] getValue(Cursor cursor, int columnIndex) {
			return cursor.getBlob(columnIndex);
		}

		@Override
		public ColumnType getColumnType() {
			return ColumnType.BLOB;
		}

		@Override
		public String convertValue(Object value) {
			throw new RuntimeException("not implemented");
		}
	};

	/**
	 * Realization for double.
	 */
	static final TypeAdapter<Double> DOUBLE_TYPE_ADAPTER = new TypeAdapter<Double>() {

		@Override
		public void putValue(ContentValues cv, String key, Double value) {
			cv.put(key, value);
		}

		@Override
		public Double getValue(Cursor cursor, int columnIndex) {
			return cursor.getDouble(columnIndex);
		}

		@Override
		public ColumnType getColumnType() {
			return ColumnType.REAL;
		}
	};

	/**
	 * Realization for float.
	 */
	static final TypeAdapter<Float> FLOAT_TYPE_ADAPTER = new TypeAdapter<Float>() {

		@Override
		public void putValue(ContentValues cv, String key, Float value) {
			cv.put(key, value);
		}

		@Override
		public Float getValue(Cursor cursor, int columnIndex) {
			return cursor.getFloat(columnIndex);
		}

		@Override
		public ColumnType getColumnType() {
			return ColumnType.REAL;
		}
	};

	/**
	 * Realization for Date.
	 */
	static final TypeAdapter<Date> DATE_TYPE_ADAPTER = new TypeAdapter<Date>() {

		@Override
		public void putValue(ContentValues cv, String key, Date value) {
			cv.put(key, value.getTime());
		}

		@Override
		public Date getValue(Cursor cursor, int columnIndex) {
			return new Date(cursor.getLong(columnIndex));
		}

		@Override
		public ColumnType getColumnType() {
			return ColumnType.INTEGER;
		}

		@Override
		public String convertValue(Object value) {
			if (value instanceof Date) {
				return String.valueOf(((Date) value).getTime());
			} else {
				return super.convertValue(value);
			}
		}
	};

	/**
	 * Realization for float.
	 */
	static final TypeAdapter<Calendar> CALENDAR_TYPE_ADAPTER = new TypeAdapter<Calendar>() {

		@Override
		public void putValue(ContentValues cv, String key, Calendar value) {
			cv.put(key, value.getTimeInMillis());
		}

		@Override
		public Calendar getValue(Cursor cursor, int columnIndex) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(cursor.getLong(columnIndex));
			return calendar;
		}

		@Override
		public ColumnType getColumnType() {
			return ColumnType.INTEGER;
		}

		@Override
		public String convertValue(Object value) {
			if (value instanceof Calendar) {
				return String.valueOf(((Calendar) value).getTimeInMillis());
			} else {
				return super.convertValue(value);
			}
		}
	};

	/**
	 * Realization for {@link Uri}
	 */
	static final TypeAdapter<Uri> URI_TYPE_ADAPTER = new CustomTypeAdapter<Uri>() {
		@Override
		protected String valueToString(Uri value) {
			return value == null ? null : value.toString();
		}

		@Override
		protected Uri parseValue(String s) {
			return TextUtils.isEmpty(s) ? null : Uri.parse(s);
		}
	};

	/**
	 * Realization for enum.
	 */
	static final class EnumTypeAdapter extends TypeAdapter<Enum<?>> {

		private final Class<Enum<?>> enumClass;

		public EnumTypeAdapter(Class<Enum<?>> enumClass) {
			this.enumClass = enumClass;
		}

		@Override
		public void putValue(ContentValues cv, String key, Enum<?> value) {
			cv.put(key, value.ordinal());
		}

		@Override
		public Enum<?> getValue(Cursor cursor, int columnIndex) {
			int ordinal = cursor.getInt(columnIndex);
			return enumClass.getEnumConstants()[ordinal];
		}

		@Override
		public ColumnType getColumnType() {
			return ColumnType.INTEGER;
		}

		@Override
		public String convertValue(Object value) {
			for (Enum<?> enumValue : enumClass.getEnumConstants()) {
				if (enumValue == value || enumValue.name().equals(value)) {
					return String.valueOf(enumValue.ordinal());
				}
			}
			return super.convertValue(value);
		}

	}

	/**
	 * Realization for customs objects via Gson.
	 *
	 * @param <T> object type.
	 */
	static final class ObjectTypeAdapter<T> extends TypeAdapter<T> {

		private final Gson gson;
		private final Class<T> objectClass;

		ObjectTypeAdapter(Gson gson, Class<T> objectClass) {
			this.gson = gson;
			this.objectClass = objectClass;
		}

		@Override
		public void putValue(ContentValues cv, String key, Object value) {
			cv.put(key, gson.toJson(value));
		}

		@Override
		public T getValue(Cursor cursor, int columnIndex) {
			return gson.fromJson(cursor.getString(columnIndex), objectClass);
		}

		@Override
		public ColumnType getColumnType() {
			return ColumnType.TEXT;
		}

		@Override
		public String convertValue(Object value) {
			throw new RuntimeException("Can't convert value for custom class " + objectClass.getName()
				+ ". Please add a type adapter for it.");
		}

	}


}
