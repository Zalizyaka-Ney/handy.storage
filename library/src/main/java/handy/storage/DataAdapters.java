package handy.storage;

import android.net.Uri;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import handy.storage.ColumnInfo.ColumnId;
import handy.storage.api.CursorValues;
import handy.storage.api.Model;
import handy.storage.api.ObjectCreator;
import handy.storage.util.ReflectionUtils;

/**
 * Holds all type adapters and object creators for the {@link HandyStorage} instance.
 */
final class DataAdapters {

	private final Map<Class<?>, TypeAdapter<?>> typeAdapters = new HashMap<>();
	private final Map<Class<?>, ObjectCreator<?>> objectCreators = new HashMap<>();
	private Gson gson;

	DataAdapters() {
		gson = new GsonBuilder().disableHtmlEscaping().create();

		typeAdapters.put(Byte.class, TypeAdapter.BYTE_TYPE_ADAPTER);
		typeAdapters.put(byte.class, TypeAdapter.BYTE_TYPE_ADAPTER);
		typeAdapters.put(Short.class, TypeAdapter.SHORT_TYPE_ADAPTER);
		typeAdapters.put(short.class, TypeAdapter.SHORT_TYPE_ADAPTER);
		typeAdapters.put(Integer.class, TypeAdapter.INT_TYPE_ADAPTER);
		typeAdapters.put(int.class, TypeAdapter.INT_TYPE_ADAPTER);
		typeAdapters.put(Long.class, TypeAdapter.LONG_TYPE_ADAPTER);
		typeAdapters.put(long.class, TypeAdapter.LONG_TYPE_ADAPTER);
		typeAdapters.put(Double.class, TypeAdapter.DOUBLE_TYPE_ADAPTER);
		typeAdapters.put(double.class, TypeAdapter.DOUBLE_TYPE_ADAPTER);
		typeAdapters.put(Float.class, TypeAdapter.FLOAT_TYPE_ADAPTER);
		typeAdapters.put(float.class, TypeAdapter.FLOAT_TYPE_ADAPTER);
		typeAdapters.put(Boolean.class, TypeAdapter.BOOLEAN_TYPE_ADAPTER);
		typeAdapters.put(boolean.class, TypeAdapter.BOOLEAN_TYPE_ADAPTER);
		typeAdapters.put(byte[].class, TypeAdapter.BLOB_TYPE_ADAPTER);
		typeAdapters.put(String.class, TypeAdapter.STRING_TYPE_ADAPTER);
		typeAdapters.put(Date.class, TypeAdapter.DATE_TYPE_ADAPTER);
		typeAdapters.put(Calendar.class, TypeAdapter.CALENDAR_TYPE_ADAPTER);
		typeAdapters.put(Uri.class, TypeAdapter.URI_TYPE_ADAPTER);
	}

	DataAdapters(DataAdapters dataAdapters) {
		typeAdapters.putAll(dataAdapters.typeAdapters);
		objectCreators.putAll(dataAdapters.objectCreators);
		gson = dataAdapters.gson;
	}

	<T> void addTypeAdapter(Class<? extends T> type, TypeAdapter<? extends T> typeAdapter) {
		typeAdapters.put(type, typeAdapter);
	}

	<T extends Model> void addObjectCreator(Class<? extends T> modelClass, ObjectCreator<T> objectCreator) {
		objectCreators.put(modelClass, objectCreator);
	}

	@SuppressWarnings("unchecked")
	<T> TypeAdapter<T> getTypeAdapter(Class<T> type) {
		TypeAdapter<?> typeAdapter = typeAdapters.get(type);
		if (typeAdapter != null) {
			return (TypeAdapter<T>) typeAdapter;
		} else {
			if (type.isEnum()) {
				return (TypeAdapter<T>) new TypeAdapter.EnumTypeAdapter((Class<Enum<?>>) type);
			} else {
				return new TypeAdapter.ObjectTypeAdapter<>(gson, type);
			}
		}
	}

	boolean hasTypeAdapter(Class<?> type) {
		TypeAdapter<?> typeAdapter = typeAdapters.get(type);
		return typeAdapter != null || type.isEnum();
	}

	@SuppressWarnings("unchecked")
	<T> ObjectCreator<T> getObjectCreator(Class<T> modelClass, TableInfo tableInfo) {
		if (objectCreators.containsKey(modelClass)) {
			return (ObjectCreator<T>) objectCreators.get(modelClass);
		} else {
			return new ReflectionObjectCreator<>(modelClass, tableInfo);
		}
	}

	void setCustomGson(Gson customGson) {
		gson = customGson;
	}

	/**
	 * Default implementation of object creator.
	 *
	 * @param <T> object type.
	 */
	static final class ReflectionObjectCreator<T> implements ObjectCreator<T> {

		private final Class<T> modelClass;
		private final TableInfo tableInfo;

		ReflectionObjectCreator(Class<T> modelClass, TableInfo tableInfo) {
			this.modelClass = modelClass;
			this.tableInfo = tableInfo;
		}

		@Override
		public T createObject(CursorValues values) {
			T object = ReflectionUtils.createNewObject(modelClass);
			for (ColumnInfo column : tableInfo.getColumns()) {
				Field field = column.getField();
				ColumnId columnId = column.getColumnId();
				Object value = values.getValue(columnId.getName());
				ReflectionUtils.setFieldValue(field, object, value);
			}
			return object;
		}

	}

}
