package handy.storage;

import android.content.Context;

import com.google.gson.Gson;

import handy.storage.annotation.GsonSerializable;
import handy.storage.api.Model;
import handy.storage.api.ObjectCreator;
import handy.storage.log.DatabaseLog;
import handy.storage.util.ReflectionUtils;

/**
 * Entry point for Handy Storage library.
 */
public final class HandyStorage {

	private final Configuration configuration;
	private final DataAdapters dataAdapters;

	private HandyStorage(Configuration configuration, DataAdapters dataAdapters) {
		this.configuration = configuration;
		this.dataAdapters = dataAdapters;
	}

	/**
	 * Creates a new database builder. Note that only one instance of the
	 * database with the same name can be opened at the same time.
	 *
	 * @param context   valid context
	 * @param dbName    name of the database
	 * @param dbVersion version of the database
	 */
	public DatabaseBuilder newDatabase(Context context, String dbName, int dbVersion) {
		return new DatabaseBuilder(context, dbName, dbVersion, configuration, dataAdapters);
	}

	/**
	 * Returns a {@link ModelAdapter} instance for the model class. It can be used for parsing objects of this class from a cursor
	 * and for converting such objects to {@link android.content.ContentValues}.
	 *
	 * @param modelClass model's class
	 * @param <T>        model's type
	 */
	public <T extends Model> ModelAdapter<T> getModelAdapter(Class<T> modelClass) {
		TableInfo tableInfo = TableParser.parseProjectionTableInfo(modelClass, configuration);
		return new ModelAdapter<>(
			new ContentValuesParser<>(dataAdapters, tableInfo),
			new CursorReader(tableInfo.getColumns(), dataAdapters),
			dataAdapters.getObjectCreator(modelClass, tableInfo));
	}

	/**
	 * Creates a new instance with the default settings. If you want to customize it - use {@link handy.storage.HandyStorage.Builder}.
	 */
	public static HandyStorage defaultInstance() {
		return new HandyStorage(new Configuration(), new DataAdapters());
	}

	/**
	 * Builder for {@link HandyStorage}.
	 */
	public static class Builder {

		private final Configuration configuration;
		private final DataAdapters dataAdapters;

		/**
		 * Creates a builder instance with default settings.
		 */
		public Builder() {
			configuration = new Configuration();
			dataAdapters = new DataAdapters();
		}

		/**
		 * Creates a builder instance with settings copied from the other {@link HandyStorage} instance.
		 *
		 * @param handyStorage HandyStorage object to copy settings from
		 */
		public Builder(HandyStorage handyStorage) {
			configuration = new Configuration(handyStorage.configuration);
			dataAdapters = new DataAdapters(handyStorage.dataAdapters);
		}

		/**
		 * <p>
		 * Sets whether the framework should consider column declarations in the
		 * super classes of model classes.
		 * </p>
		 * <p>
		 * By default the framework considers such declarations.
		 * </p>
		 */
		public Builder setUseColumnsFromSuperclasses(boolean useColumnsFromSuperclasses) {
			configuration.setUseColumnsFromSuperclasses(useColumnsFromSuperclasses);
			return this;
		}

		/**
		 * <p>Set the way to serialize/deserialize values to/from cursor. Use this
		 * method customise the way to serialize values of this type or expand the
		 * list of supported column types.</p>
		 * <p>It is recommended to override methods {@link #equals(Object)} and
		 * {@link #hashCode()} for this type.</p>
		 */
		public <T> Builder setTypeAdapter(Class<T> type, CustomTypeAdapter<T> typeAdapter) {
			if (!ReflectionUtils.areEqualsMethodsOverridden(type)) {
				DatabaseLog.w("It is recommended to override methods equals() and hashCode() in class " + type.getName());
			}
			dataAdapters.addTypeAdapter(type, typeAdapter);
			return this;
		}

		/**
		 * Sets the creator to create table model instances from cursor's values
		 * during select operations. This allows to customise objects creation or
		 * get rid of a required default constructor.
		 */
		public <T extends Model> Builder setObjectCreator(Class<T> modelClass, ObjectCreator<T> objectCreator) {
			dataAdapters.addObjectCreator(modelClass, objectCreator);
			return this;
		}

		/**
		 * Sets the {@link Gson} object to use for the serialization of columns
		 * annotated with {@link GsonSerializable} instead of the default one.
		 */
		public Builder setCustomGson(Gson gson) {
			dataAdapters.setCustomGson(gson);
			return this;
		}

		/**
		 * Builds a customized {@link HandyStorage} instance.
		 */
		public HandyStorage build() {
			return new HandyStorage(configuration, dataAdapters);
		}

	}

	/**
	 * Base configuration for the library.
	 */
	static class Configuration {

		private boolean useColumnsFromSuperclasses = true;

		private Configuration() {
		}

		Configuration(Configuration configuration) {
			useColumnsFromSuperclasses = configuration.useColumnsFromSuperclasses;
		}

		boolean addFieldsFromSuperclasses() {
			return useColumnsFromSuperclasses;
		}

		void setUseColumnsFromSuperclasses(boolean useColumnsFromSuperclasses) {
			this.useColumnsFromSuperclasses = useColumnsFromSuperclasses;
		}
	}

}
