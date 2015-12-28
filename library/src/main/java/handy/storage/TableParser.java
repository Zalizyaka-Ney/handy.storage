package handy.storage;

import android.text.TextUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import handy.storage.ColumnInfo.ColumnId;
import handy.storage.TableInfo.Builder;
import handy.storage.annotation.AliasFor;
import handy.storage.annotation.AutoIncrement;
import handy.storage.annotation.Column;
import handy.storage.annotation.CompositePrimaryKey;
import handy.storage.annotation.CompositeUnique;
import handy.storage.annotation.CompositeUniques;
import handy.storage.annotation.ForeignKey;
import handy.storage.annotation.FunctionResult;
import handy.storage.annotation.GsonSerializable;
import handy.storage.annotation.NotNull;
import handy.storage.annotation.PrimaryKey;
import handy.storage.annotation.Reference;
import handy.storage.annotation.TableName;
import handy.storage.annotation.Unique;
import handy.storage.api.ColumnType;
import handy.storage.api.Model;
import handy.storage.api.UniqueObject;
import handy.storage.base.OnConflictStrategy;
import handy.storage.exception.InvalidDatabaseSchemaException;
import handy.storage.log.DatabaseLog;
import handy.storage.log.PerformanceTimer;
import handy.storage.util.ReflectionUtils;
import handy.storage.util.ReflectionUtils.FieldsFilter;

/**
 * Helper class to parse table info.
 */
final class TableParser {

	private static final FieldsFilter DATABASE_COLUMN_FIELDS_FILTER = new FieldsFilter() {

		@Override
		public boolean filterField(Field field) {
			boolean isColumn = field.isAnnotationPresent(Column.class);
			if (isColumn) {
				if (Modifier.isStatic(field.getModifiers())) {
					DatabaseLog.w("Static field can't be a column. Ignoring field " + field.getName());
					return false;
				} else {
					return true;
				}
			} else {
				return false;
			}
		}

	};

	private TableParser() {
	}

	private static List<Field> getColumnFields(Class<?> modelClass, HandyStorage.Configuration configuration) {
		return ReflectionUtils.getFields(
			modelClass,
			configuration.addFieldsFromSuperclasses(),
			DATABASE_COLUMN_FIELDS_FILTER);
	}

	static TableInfo parseTableFromIModel(Class<? extends Model> modelClass, DatabaseConfiguration configuration, DataAdapters dataAdapters) {

		PerformanceTimer.startInterval("parsing table " + modelClass.getName());

		if (!configuration.addFieldsFromSuperclasses() && UniqueObject.class.isAssignableFrom(modelClass)) {
			throwDeclarationException(modelClass, "Don't use UniqueObject without enabling usage of fields from superclasses.");
		}

		String tableName = resolveTableName(modelClass);
		Builder builder = new Builder(tableName);
		builder.setOriginClass(modelClass);
		List<Field> fields = getColumnFields(modelClass, configuration);
		Set<String> constants = ReflectionUtils.getStringConstantNames(modelClass);
		for (Field field : fields) {
			ColumnInfo columnInfo = parseColumn(modelClass, field, tableName, dataAdapters);
			builder.addColumn(columnInfo);
			if (configuration.enforceColumnNameConstants()) {
				checkConvenientColumnConstant(constants, columnInfo.getName(), modelClass);
			}
		}

		CompositePrimaryKey compositePrimaryKey = modelClass.getAnnotation(CompositePrimaryKey.class);
		if (compositePrimaryKey != null) {
			builder.setPrimaryKeyColumns(compositePrimaryKey.onConflictStrategy(), compositePrimaryKey.columns());
		}

		builder.setCompositeUniques(parseCompositeUniques(modelClass));

		TableInfo tableInfo = builder.buildDatabaseTable();

		PerformanceTimer.endInterval();

		return tableInfo;
	}

	private static CompositeUnique[] parseCompositeUniques(Class<?> modelClass) {
		CompositeUniques compositeUniques = modelClass.getAnnotation(CompositeUniques.class);
		CompositeUnique compositeUnique = modelClass.getAnnotation(CompositeUnique.class);
		if (compositeUniques != null) {
			if (compositeUnique != null) {
				throwDeclarationException(modelClass, "Don't use " + CompositeUnique.class.getName() + " and "
					+ CompositeUniques.class.getName() + " annotations simultaneously.");
			}
			CompositeUnique[] result = compositeUniques.value();
			if (result.length == 0) {
				throwDeclarationException(modelClass, "Empty value for annotation " + CompositeUniques.class.getName() + ".");
			}
			return result;
		} else if (compositeUnique != null) {
			return new CompositeUnique[]{compositeUnique};
		} else {
			return new CompositeUnique[0];
		}

	}

	private static ColumnInfo parseColumn(Class<?> modelClass, Field field, String tableName, DataAdapters dataAdapters) {
		ColumnId columnId = resolveColumnId(field).withTableName(tableName);
		if (field.isAnnotationPresent(Reference.class)) {
			if (Model.class.isAssignableFrom(field.getType())) {
				return parseReferenceColumn(modelClass, field, columnId, field.getAnnotation(Reference.class));
			} else {
				throw new InvalidDatabaseSchemaException("You can't use Reference annotation on non-Model field");
			}
		} else {
			return parseSimpleColumn(modelClass, field, dataAdapters, columnId);
		}

	}

	@SafeVarargs
	private static void checkFieldIsNotAnnotated(Class<?> modelClass, Field field, Class<? extends Annotation>... annotations) {
		for (Class<? extends Annotation> annotation : annotations) {
			if (field.isAnnotationPresent(annotation)) {
				throwDeclarationException(modelClass, "Don't use annotation " + annotation.getName() + " on reference declaration.");
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static ColumnInfo parseReferenceColumn(Class<?> modelClass, Field field, ColumnId columnId, Reference referenceAnnotation) {
		checkFieldIsNotAnnotated(
			modelClass,
			field,
			AutoIncrement.class, FunctionResult.class, GsonSerializable.class, AliasFor.class, ForeignKey.class);
		// PrimaryKey.class, Unique.class were removed from this list 
		ColumnInfo.Builder columnBuilder = new ColumnInfo.Builder(columnId, null);
		columnBuilder.setField(field);
		columnBuilder.setReferencedTo(
			(Class<? extends Model>) field.getType(),
			referenceAnnotation.onUpdateAction(),
			referenceAnnotation.onDeleteAction());
		return columnBuilder.build();
	}

	private static ColumnInfo parseSimpleColumn(Class<?> modelClass, Field field, DataAdapters dataAdapters, ColumnId columnId) {
		int flags = resolveColumnModifiers(modelClass, field);
		TypeAdapter<?> typeAdapter = dataAdapters.getTypeAdapter(field.getType());
		ColumnType columnType = typeAdapter.getColumnType();
		ColumnInfo.Builder columnInfoBuilder = new ColumnInfo.Builder(columnId, columnType);
		columnInfoBuilder.setField(field);
		columnInfoBuilder.setFlags(flags);
		resolveOnConflictValue(field, columnInfoBuilder, flags);
		ForeignKey foreignKey = field.getAnnotation(ForeignKey.class);
		if (foreignKey != null) {
			columnInfoBuilder.setForeignKeyTo(
				foreignKey.modelClass(),
				foreignKey.column(),
				foreignKey.onUpdateAction(),
				foreignKey.onDeleteAction());
		}
		ColumnInfo columnInfo = columnInfoBuilder.build();
		checkDeclarationErrors(modelClass, field, columnId.getName(), columnInfo, dataAdapters);

		return columnInfoBuilder.build();
	}

	private static void checkDeclarationErrors(Class<?> modelClass, Field field, String columnName, ColumnInfo columnInfo, DataAdapters dataAdapters) {
		if (columnInfo.isPrimaryKeyFlagSet() && columnInfo.isUniqueFlagSet()) {
			throw new InvalidDatabaseSchemaException("invalid column \"" + columnName + "\" definition: "
				+ "don't use PrimaryKey and Unique annotations simultaneously");
		}
		if (columnInfo.isFlagSet(ColumnInfo.PRIMARY_KEY_AUTO_INCREMENT) && (field.getType() != long.class && field.getType() != Long.class)) {
			throw new InvalidDatabaseSchemaException("invalid column \"" + columnName + "\" definition: "
				+ "please declare rowid field as long");
		}
		if (field.isAnnotationPresent(FunctionResult.class) || field.isAnnotationPresent(AliasFor.class)) {
			throw new InvalidDatabaseSchemaException("You can't use annotations FunctionResult, AliasFor in database table declaration");
		}
		checkJsonObjectDeclaration(modelClass, field, dataAdapters);
	}

	private static void checkJsonObjectDeclaration(Class<?> modelClass, Field field, DataAdapters dataAdapters) {
		boolean isGsonSerializable = field.isAnnotationPresent(GsonSerializable.class);
		boolean hasTypeAdapter = dataAdapters.hasTypeAdapter(field.getType());
		if (isGsonSerializable && hasTypeAdapter) {
			throwDeclarationException(modelClass, "JsonObject annotation can't be used for types that already has a type adapter.");
		} else if (!isGsonSerializable && !hasTypeAdapter) {
			throwDeclarationException(modelClass, "Don't know how to serialize field " + field.getName()
				+ ". Use annotations JsonObject, Reference or set a custom type adapter.");
		}
	}

	private static void resolveOnConflictValue(Field field, ColumnInfo.Builder columnInfoBuilder, int flags) {
		if (ColumnInfo.isFlagPresent(flags, ColumnInfo.PRIMARY_KEY)) {
			OnConflictStrategy onConflict;
			onConflict = field.getAnnotation(PrimaryKey.class).value();
			columnInfoBuilder.setOnConflictStrategy(onConflict);
		} else if (ColumnInfo.isFlagPresent(flags, ColumnInfo.UNIQUE)) {
			columnInfoBuilder.setOnConflictStrategy(field.getAnnotation(Unique.class).value());
		}
	}

	private static void checkConvenientColumnConstant(Collection<String> fields, String columnName, Class<?> modelClass) {
		String constant = generateColumnNameConstantName(columnName);
		if (!fields.contains(constant)) {
			constant = constant
				+ (constant.endsWith("_") ? "" : "_")
				+ "COLUMN";
			if (!fields.contains(constant)) {
				throwDeclarationException(modelClass, "Model class must have " + constant + " constant.");
			}
		}
		try {
			Field constantField = modelClass.getDeclaredField(constant);
			if (constantField.getType() != String.class) {
				throwDeclarationException(modelClass, constant + " must be String.");
			}
			boolean columnNameMatched;
			try {
				Object constantValue = ReflectionUtils.getFieldValue(constantField, null);
				columnNameMatched = columnName.equals(constantValue);
			} catch (Exception e) {
				columnNameMatched = false;
			}
			if (!columnNameMatched) {
				throwDeclarationException(modelClass, "Value of " + constant + " must be " + columnName);
			}
		} catch (NoSuchFieldException e) {
			// shouldn't get here
		}
	}

	private static int resolveColumnModifiers(Class<?> modelClass, Field field) {
		int flags = 0;
		if (field.isAnnotationPresent(NotNull.class)) {
			flags |= ColumnInfo.NOT_NULL;
		}
		if (field.isAnnotationPresent(Unique.class)) {
			flags |= ColumnInfo.UNIQUE;
		}
		boolean primaryKey;
		boolean autoIncrement;
		primaryKey = field.isAnnotationPresent(PrimaryKey.class);
		autoIncrement = field.isAnnotationPresent(AutoIncrement.class);
		if (autoIncrement && !primaryKey) {
			throwDeclarationException(modelClass, "you can use AutoIncrement only with PrimaryKey annotation");
		}
		if (primaryKey) {
			flags |= (autoIncrement ? ColumnInfo.PRIMARY_KEY_AUTO_INCREMENT : ColumnInfo.PRIMARY_KEY);
		}
		return flags;
	}

	private static ColumnId resolveColumnId(Field field) {
		String fieldName = field.getName();
		Column column = field.getAnnotation(Column.class);
		return new ColumnId(getColumnName(field, column), "", fieldName);
	}

	private static String getColumnName(Field field, Column columnAnnotation) {
		if (columnAnnotation == null) {
			return field.getName();
		} else {
			String name = columnAnnotation.value();
			return TextUtils.isEmpty(name) ? field.getName() : name;
		}
	}

	static String resolveTableName(Class<? extends Model> modelClass) {
		TableName tableNameAnnotation = modelClass.getAnnotation(TableName.class);
		if (tableNameAnnotation == null) {
			return modelClass.getSimpleName();
		} else {
			return tableNameAnnotation.value();
		}
	}

	static TableInfo parseProjectionTableInfo(Class<? extends Model> modelClass, HandyStorage.Configuration configuration) {

		TableInfo.Builder builder = new Builder(null);
		List<Field> fields = getColumnFields(modelClass, configuration);
		for (Field field : fields) {
			ColumnInfo.Builder columnBuilder = new ColumnInfo.Builder(resolveColumnId(field), null).setField(field);
			String entity = resolveEntity(modelClass, field);
			if (!TextUtils.isEmpty(entity)) {
				columnBuilder.setEntity(entity);
			}
			ColumnInfo column = columnBuilder.build();

			builder.addColumn(column);
		}
		return builder.build();
	}

	private static String resolveEntity(Class<?> modelClass, Field field) {
		String entity = null;
		FunctionResult functionResult = field.getAnnotation(FunctionResult.class);
		AliasFor entityAnnotation = field.getAnnotation(AliasFor.class);
		if (functionResult != null && entityAnnotation != null) {
			throwDeclarationException(modelClass, "You can't use FunctionResult and AliasFor annotations simultaneously");
		}
		if (functionResult != null) {
			entity = functionResult.type().toSQLEntity(functionResult.arguments());
		}
		if (entityAnnotation != null) {
			entity = entityAnnotation.value();
		}
		return entity;
	}

	private static final Pattern ELEMENT_NAME_PATTERN = Pattern.compile("(\\d+_*|([A-Z]*|(^[a-z]))[_a-z\\$]*)");

	private static String generateColumnNameConstantName(String elementName) {
		List<String> parts = new ArrayList<>();
		String result;

		try {
			Matcher matcher = ELEMENT_NAME_PATTERN.matcher(elementName);

			while (matcher.find()) {
				String part = elementName.substring(matcher.start(), matcher.end());
				if (!TextUtils.isEmpty(part.trim())) {
					parts.add(part.toUpperCase());
				}
			}

			result = parts.isEmpty() ? elementName : joinNameElements(parts);
		} catch (Exception e) {
			result = elementName;
		}

		return result;
	}

	private static String joinNameElements(Iterable<String> iterable) {
		StringBuilder result = new StringBuilder();
		Iterator<String> i = iterable.iterator();
		boolean hasNext = i.hasNext();
		while (hasNext) {
			String part = i.next();
			hasNext = i.hasNext();
			result.append(part);
			if (hasNext && !part.endsWith("_")) {
				result.append('_');
			}
		}
		return result.toString();
	}

	static void throwDeclarationException(Class<?> modelClass, String message) {
		throw new InvalidDatabaseSchemaException("Error in " + modelClass.getName() + " declaration. " + message);
	}

}
