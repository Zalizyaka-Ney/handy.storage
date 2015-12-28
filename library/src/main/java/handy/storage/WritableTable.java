package handy.storage;

import android.content.ContentValues;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import handy.storage.ColumnInfo.ColumnId;
import handy.storage.api.Delete;
import handy.storage.api.Model;
import handy.storage.api.Select;
import handy.storage.api.Update;
import handy.storage.base.DatabaseAdapter;
import handy.storage.base.OnConflictStrategy;
import handy.storage.base.QueryParams;
import handy.storage.exception.ConstraintFailedException;
import handy.storage.exception.OperationException;
import handy.storage.log.PerformanceTimer;
import handy.storage.util.ReflectionUtils;

/**
 * Represents database table.
 *
 * @param <T> model class
 */
public class WritableTable<T extends Model> extends ReadableTable<T> {

	private boolean setIdOnInsert = true;

	WritableTable(Class<T> modelClass, TableInfo tableInfo, DatabaseAdapter databaseAdapter, DatabaseCore databaseCore) {
		super(modelClass, tableInfo, databaseAdapter, databaseCore, QueryParams.DEFAULT_FACTORY);
		setIdOnInsert = databaseCore.getConfiguration().setIdOnInsertByDefault();
	}

	/**
	 * Enables/disables setting rowids to objects after insertion.
	 */
	public void setIdOnInsert(boolean set) {
		setIdOnInsert = set;
	}

	/**
	 * Inserts the object (with the default on conflict strategy) in the table
	 * and returns its rowid. If this object conflicts with rows in the table,
	 * {@link ConstraintFailedException} will be thrown.
	 *
	 * @param object object to insert
	 * @throws OperationException if any error happen
	 */
	public long insert(T object) throws OperationException {
		return insert(object, OnConflictStrategy.DEFAULT);
	}

	/**
	 * Inserts the objects (with the default on conflict strategy) in the table
	 * in a single transaction and returns the list of their rowids.
	 *
	 * @param objects objects to insert
	 * @throws OperationException if any error happen
	 */
	public List<Long> insert(Collection<T> objects) throws OperationException {
		return insert(objects, OnConflictStrategy.DEFAULT);
	}

	/**
	 * Inserts the object in the table, replaces all old conflicting rows and
	 * returns its rowid.
	 *
	 * @param object object to insert
	 * @throws OperationException if any error happen
	 */
	public long insertOrReplace(T object) throws OperationException {
		return insert(object, OnConflictStrategy.REPLACE);
	}

	/**
	 * Inserts the objects in the table and replaces all old conflicting rows in
	 * a single transaction and returns the list of their rowids.
	 *
	 * @param objects objects to insert
	 * @throws OperationException if any error happen
	 */
	public List<Long> insertOrReplace(Collection<T> objects) throws OperationException {
		return insert(objects, OnConflictStrategy.REPLACE);
	}

	/**
	 * Inserts the object with the specified on conflict strategy in the table
	 * and returns its rowid. May return <code>-1</code> as rowid in case of
	 * {@link OnConflictStrategy#IGNORE}.
	 *
	 * @param object object to insert
	 * @throws OperationException if any error happen
	 */
	public long insert(T object, OnConflictStrategy onConflictStrategy) throws OperationException {
		PerformanceTimer.startInterval("insertWithOnConflict, single object");
		PerformanceTimer.startInterval("convert model to ContentValues");
		ContentValues cv = getContentValuesParser().parseContentValues(object);
		PerformanceTimer.endInterval();
		long id;
		if (onConflictStrategy == OnConflictStrategy.DEFAULT) {
			id = getDatabaseAdapter().insert(getTableName(), cv);
		} else {
			id = getDatabaseAdapter().insert(getTableName(), cv, onConflictStrategy);
		}
		if (setIdOnInsert) {
			setIdToObjects(Collections.singleton(object), Collections.singletonList(id));
		}
		PerformanceTimer.endInterval();
		return id;
	}

	/**
	 * Inserts the objects with the specified on conflict strategy in the table
	 * in a single transaction and returns the list of their rowids. May return
	 * <code>-1</code> as rowid in case of {@link OnConflictStrategy#IGNORE}.
	 *
	 * @param objects objects to insert
	 * @throws OperationException if any error happen
	 */
	public List<Long> insert(Collection<T> objects, OnConflictStrategy onConflictStrategy) throws OperationException {
		PerformanceTimer.startInterval("insertWithOnConflict, collection of objects");
		PerformanceTimer.startInterval("convert " + objects.size() + " models to ContentValues");
		ContentValuesParser<T> contentValuesParser = getContentValuesParser();
		List<ContentValues> modelsContentValues = new ArrayList<>(objects.size());
		for (T object : objects) {
			modelsContentValues.add(contentValuesParser.parseContentValues(object));
		}
		PerformanceTimer.endInterval();
		List<Long> result;
		if (onConflictStrategy == OnConflictStrategy.DEFAULT) {
			result = getDatabaseAdapter().insert(getTableName(), modelsContentValues);
		} else {
			result = getDatabaseAdapter().insert(getTableName(), modelsContentValues, onConflictStrategy);
		}
		if (setIdOnInsert) {
			setIdToObjects(objects, result);
		}
		PerformanceTimer.endInterval();
		return result;
	}

	private void setIdToObjects(Collection<T> objects, List<Long> ids) {
		ColumnInfo rowIdColumn = getTableInfo().getRowIdColumn();
		if (rowIdColumn != null) {
			Field field = rowIdColumn.getField();
			Iterator<Long> idIterator = ids.iterator();
			for (T object : objects) {
				long id = idIterator.next();
				if (id != -1) {
					ReflectionUtils.setFieldValue(field, object, id);
				}
			}
		}
	}

	/**
	 * Starts a deletion operation.
	 */
	public Delete<T> delete() {
		return new DeleteOperation<>(this);
	}

	/**
	 * Deletes all rows from this table.
	 *
	 * @return the number of the deleted rows
	 * @throws OperationException if any error happen
	 */
	public int deleteAll() throws OperationException {
		return delete().execute();
	}

	/**
	 * Deletes these objects from the table. You must declare at least one
	 * unique (or primary key) column in this table to use this method.
	 *
	 * @param elements objects to delete
	 * @return the number of the deleted rows
	 * @throws OperationException if any error happen
	 */
	@SuppressWarnings("unchecked")
	public int delete(T... elements) throws OperationException {
		return delete(Arrays.asList(elements));
	}

	/**
	 * Deletes these objects from the table. You must declare at least one
	 * unique (or primary key) column in this table to use this method.
	 *
	 * @param elements objects to delete
	 * @return the number of the deleted rows
	 * @throws OperationException if any error happen
	 */
	public int delete(Collection<T> elements) throws OperationException {
		ColumnInfo uniqueColumn = getUniqueColumnOrThrow();
		List<Object> uniqueColumnValues = getColumnValues(elements, uniqueColumn);
		ColumnId uniqueColumnId = uniqueColumn.getColumnId();
		return delete().where(uniqueColumnId.getName()).in(uniqueColumnValues).execute();
	}

	/**
	 * Deletes all objects from this selection.
	 *
	 * @param selectToDelete {@link Select} object built on this table
	 * @return the number of the deleted rows
	 * @throws OperationException if any error happen
	 */
	public int deleteAllFrom(Select<T> selectToDelete) throws OperationException {
		return delete().where(expressions().oneOf(selectToDelete)).execute();
	}

	/**
	 * Deletes all objects except ones from this selection.
	 *
	 * @param selectToKeep {@link Select} object built on this table
	 * @return the number of the deleted rows
	 * @throws OperationException if any error happen
	 */
	public int deleteAllExcept(Select<T> selectToKeep) throws OperationException {
		return delete().where(expressions().exclude(selectToKeep)).execute();
	}

	/**
	 * Starts an update operation.
	 */
	public Update update() {
		return new UpdateOperation(this);
	}

	/**
	 * Starts an update operation on the rows in the database corresponding to
	 * these <code>objects</code>. The <code>objects</code> itself won't be
	 * changed; if you need to get the updated values - use the selection
	 * operations. You must declare at least one unique (or primary key) column
	 * in this table to use this method.
	 */
	@SuppressWarnings("unchecked")
	public Update update(T... objects) {
		return new UpdateOperation(this, expressions().oneOf(objects));
	}

	/**
	 * Starts an update operation on the rows in the database corresponding to
	 * these <code>objects</code>. The <code>objects</code> itself won't be
	 * changed; if you need to get the updated values - use the selection
	 * operations. You must declare at least one unique (or primary key) column
	 * in this table to use this method.
	 */
	public Update update(Collection<T> objects) {
		return new UpdateOperation(this, expressions().oneOf(objects));
	}

}
