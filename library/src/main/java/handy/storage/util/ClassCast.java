package handy.storage.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Util for objects casting.
 */
public final class ClassCast {

	private ClassCast() {}

	private static final Map<Class<?>, Class<?>[]> ASSIGNABLE_TO_CLASSES_MAP = new HashMap<>();

	static {
		
		Class<?>[] assignableFromBoolean = {Boolean.class, boolean.class };
		ASSIGNABLE_TO_CLASSES_MAP.put(Boolean.class, assignableFromBoolean);
		ASSIGNABLE_TO_CLASSES_MAP.put(boolean.class, assignableFromBoolean);
		
		Class<?>[] assignableFromDouble = {Double.class, double.class };
		ASSIGNABLE_TO_CLASSES_MAP.put(Double.class, assignableFromDouble);
		ASSIGNABLE_TO_CLASSES_MAP.put(double.class, assignableFromDouble);

		Class<?>[] floatClasses = new Class<?>[] {Float.class, float.class };
		Class<?>[] assignableFromFloat = unionOf(assignableFromDouble, floatClasses);
		ASSIGNABLE_TO_CLASSES_MAP.put(Float.class, assignableFromFloat);
		ASSIGNABLE_TO_CLASSES_MAP.put(float.class, assignableFromFloat);

		Class<?>[] assignableFromLong = unionOf(assignableFromDouble, new Class<?>[] {Long.class, long.class });
		ASSIGNABLE_TO_CLASSES_MAP.put(Long.class, assignableFromLong);
		ASSIGNABLE_TO_CLASSES_MAP.put(long.class, assignableFromLong);

		Class<?>[] assignableFromInt = unionOf(assignableFromLong, floatClasses, new Class<?>[] {int.class, Integer.class });
		ASSIGNABLE_TO_CLASSES_MAP.put(Integer.class, assignableFromInt);
		ASSIGNABLE_TO_CLASSES_MAP.put(int.class, assignableFromInt);

		Class<?>[] assignableFromShort = unionOf(assignableFromInt, new Class<?>[] {short.class, Short.class });
		ASSIGNABLE_TO_CLASSES_MAP.put(Short.class, assignableFromShort);
		ASSIGNABLE_TO_CLASSES_MAP.put(short.class, assignableFromShort);

		Class<?>[] assignableFromByte = unionOf(assignableFromShort, new Class<?>[] {byte.class, Byte.class });
		ASSIGNABLE_TO_CLASSES_MAP.put(Byte.class, assignableFromByte);
		ASSIGNABLE_TO_CLASSES_MAP.put(byte.class, assignableFromByte);
	}

	private static final Map<Class<?>, Object> PRIMITIVE_DEFAULTS = new HashMap<>();

	static {
		PRIMITIVE_DEFAULTS.put(boolean.class, false);
		PRIMITIVE_DEFAULTS.put(byte.class, (byte) 0);
		PRIMITIVE_DEFAULTS.put(short.class, (short) 0);
		PRIMITIVE_DEFAULTS.put(int.class, 0);
		PRIMITIVE_DEFAULTS.put(long.class, 0);
		PRIMITIVE_DEFAULTS.put(double.class, 0);
		PRIMITIVE_DEFAULTS.put(float.class, 0);
	}

	private static Class<?>[] unionOf(Class<?>[]... arrays) {
		int size = 0;
		for (Class<?>[] array : arrays) {
			size += array.length;
		}
		Class<?>[] union = new Class<?>[size];
		int i = 0;
		for (Class<?>[] array : arrays) {
			for (Class<?> type : array) {
				union[i++] = type;
			}
		}
		return union;
	}

	/**
	 * Checks if a value of type <code>fromClass</code> is assignable to a
	 * variable with type <code>toClass</code> in java.
	 */
	public static boolean isValueAssignable(Class<?> fromClass, Class<?> toClass) {
		if (fromClass.equals(toClass)) {
			return true;
		}
		Class<?>[] assignableToClasses = ASSIGNABLE_TO_CLASSES_MAP.get(fromClass);
		if (assignableToClasses != null) {
			for (Class<?> assignableClass : assignableToClasses) {
				if (assignableClass.equals(toClass)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns the default value for variables with this type.
	 */
	public static Object getDefaultValueForType(Class<?> type) {
		return PRIMITIVE_DEFAULTS.get(type);
	}

	/**
	 * Advanced object casting operation. <br>
	 * 
	 * For numbers, this method supports casting to wider number types, for
	 * example - from <b>int</b> to <b>Long</b>. <br>
	 * 
	 * If the object is not a number, the usual java casting is used.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T castObject(Object object, Class<T> toClass) {
		if (object == null) {
			return null;
		}

		Class<?> objectClass = object.getClass();
		if (objectClass.equals(toClass)) {
			return (T) object;
		}
		if (!isValueAssignable(objectClass, toClass)) {
			throw new IllegalArgumentException(String.format("Can't cast from %s to %s",
					objectClass.getName(), toClass));
		}
		if (isNumber(toClass)) {
			return castToNumber(object, toClass);
		}
		return (T) object;
	}

	private static boolean isLong(Class<?> type) {
		return type.equals(Long.class) || type.equals(long.class);
	}

	private static boolean isInteger(Class<?> type) {
		return type.equals(Integer.class) || type.equals(int.class);
	}

	private static boolean isShort(Class<?> type) {
		return type.equals(Short.class) || type.equals(short.class);
	}

	private static boolean isByte(Class<?> type) {
		return type.equals(Byte.class) || type.equals(byte.class);
	}

	private static boolean isDouble(Class<?> type) {
		return type.equals(Double.class) || type.equals(double.class);
	}

	private static boolean isFloat(Class<?> type) {
		return type.equals(Float.class) || type.equals(float.class);
	}

	/**
	 * Whether this type represents number.
	 */
	public static boolean isNumber(Class<?> type) {
		return isByte(type) || isDouble(type) || isFloat(type) || isInteger(type) || isLong(type) || isShort(type);
	}

	/**
	 * Whether this type represents fractional number.
	 */
	public static boolean isFractionalNumber(Class<?> type) {
		return isDouble(type) || isFloat(type);
	}

	@SuppressWarnings("unchecked")
	private static <T> T castToNumber(Object object, Class<?> toClass) {
		Number number = (Number) object;
		if (isByte(toClass)) {
			return (T) Byte.valueOf(number.byteValue());
		} else if (isDouble(toClass)) {
			return (T) Double.valueOf(number.doubleValue());
		} else if (isFloat(toClass)) {
			return (T) Float.valueOf(number.floatValue());
		} else if (isInteger(toClass)) {
			return (T) Integer.valueOf(number.intValue());
		} else if (isLong(toClass)) {
			return (T) Long.valueOf(number.longValue());
		} else if (isShort(toClass)) {
			return (T) Short.valueOf(number.shortValue());
		}
		throw new RuntimeException("broken logic in ClassCast.castObject()");
	}

}
