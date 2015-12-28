package handy.storage.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import handy.storage.exception.ObjectCreationException;
import handy.storage.log.DatabaseLog;

/**
 * helper class to use java reflection API.
 */
public final class ReflectionUtils {

	/**
	 * Filters fields.
	 */
	public interface FieldsFilter {

		/**
		 * Whether this field should be included/processed.
		 */
		boolean filterField(Field field);

	}

	private ReflectionUtils() {}

	/**
	 * Reads the value of this field.
	 */
	public static Object getFieldValue(Field field, Object object) {
		field.setAccessible(true);
		Object result = null;
		try {
			result = field.get(object);
		} catch (IllegalAccessException | IllegalArgumentException e) {
			DatabaseLog.logException(e);
		}
		return result;
	}

	/**
	 * Sets the field's value.
	 */
	public static void setFieldValue(Field field, Object object, Object value) {
		field.setAccessible(true);
		try {
			field.set(object, value);
		} catch (IllegalAccessException | IllegalArgumentException e) {
			DatabaseLog.logException(e);
		}
	}

	/**
	 * Returns the list of all non-private string constants declared in this
	 * class.
	 */
	public static Set<String> getStringConstantNames(Class<?> clazz) {
		List<Field> fields = getFields(clazz, true);
		Set<String> constants = new HashSet<>(fields.size());
		for (Field field : fields) {
			int modifiers = field.getModifiers();
			if (Modifier.isFinal(modifiers) && Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers)) {
				constants.add(field.getName());
			}
		}
		return constants;
	}

	/**
	 * Returns the list of all fields declared in this class.
	 * 
	 * @param modelClass
	 *            class
	 * @param addFieldsFromSuperclass
	 *            whether fields from super classes should be included in the
	 *            result
	 */
	public static List<Field> getFields(Class<?> modelClass, boolean addFieldsFromSuperclass) {
		List<Field> allFields = new LinkedList<>(Arrays.asList(modelClass.getDeclaredFields()));
		if (addFieldsFromSuperclass) {
			addFieldsFromSuperclass(modelClass.getSuperclass(), allFields);
		}
		return allFields;
	}

	private static void addFieldsFromSuperclass(Class<?> clazz, List<Field> fields) {
		if (clazz != null && clazz != Object.class) {
			fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
			addFieldsFromSuperclass(clazz.getSuperclass(), fields);
		}
	}

	/**
	 * Returns the list of all fields declared in this class and passed the
	 * filter.
	 * 
	 * @param modelClass
	 *            class
	 * @param addFieldsFromSuperclass
	 *            whether fields from super classes should be included in the
	 *            result
	 * @param filter
	 *            determines what fields should be included
	 */
	public static List<Field> getFields(Class<?> modelClass, boolean addFieldsFromSuperclass, FieldsFilter filter) {
		List<Field> allFields = getFields(modelClass, addFieldsFromSuperclass);
		List<Field> result = new LinkedList<>();
		for (Field field : allFields) {
			if (filter.filterField(field)) {
				result.add(field);
			}
		}
		return result;
	}

	/**
	 * Checks whether this class overrides {@link #equals(Object)} and
	 * {@link #hashCode()} methods from its superclasses.
	 */
	public static boolean areEqualsMethodsOverridden(Class<?> clazz) {
		try {
			clazz.getDeclaredMethod("equals", Object.class);
			clazz.getDeclaredMethod("hashCode");
			return true;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}

	/**
	 * Creates a new instance of the class by calling a default constructor.
	 * 
	 * @throws ObjectCreationException
	 *             in an error happened during the object creation
	 */
	public static <T> T createNewObject(Class<T> objectClass) {
		try {
			Constructor<T> defaultConstructor = objectClass.getDeclaredConstructor();
			defaultConstructor.setAccessible(true);
			return defaultConstructor.newInstance();
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException e) {
			DatabaseLog.logException(e);
			throw new ObjectCreationException(objectClass.getName() + " should have a default constructor");
		} catch (Exception e) {
			DatabaseLog.logException(e);
			throw new ObjectCreationException("can't instantiate " + objectClass.getName());
		}
	}

}
