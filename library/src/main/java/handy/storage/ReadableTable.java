package handy.storage;

import java.util.Iterator;
import java.util.List;

import handy.storage.api.Model;
import handy.storage.api.ObjectCreator;
import handy.storage.api.Result;
import handy.storage.api.Select;
import handy.storage.api.Value;
import handy.storage.base.DatabaseAdapter;
import handy.storage.base.QueryParams;
import handy.storage.exception.OperationException;
import handy.storage.util.Factory;

/**
 * Projection of database tables.
 *
 * @param <T> model class
 */
public class ReadableTable<T extends Model> extends Table implements Iterable<T> {

	private final Class<T> modelClass;

	protected ReadableTable(Class<T> modelClass, TableInfo tableInfo, DatabaseAdapter databaseAdapter, DatabaseCore databaseCore, Factory<QueryParams> queryParamsFactory) {
		super(tableInfo, databaseAdapter, databaseCore, queryParamsFactory);
		this.modelClass = modelClass;
	}

	protected ContentValuesParser<T> getContentValuesParser() {
		return new ContentValuesParser<>(getDataAdapters(), getTableInfo());
	}

	protected ObjectCreator<T> getObjectCreator() {
		return getDataAdapters().getObjectCreator(modelClass, getTableInfo());
	}

	@Override
	protected Class<T> getModelClass() {
		return modelClass;
	}

	/**
	 * Creates new selection operation.
	 */
	public Select<T> select() {
		return SelectOperation.createModelSelect(this, getQueryParamsFactory());
	}

	/**
	 * Selects all the records from this table.
	 *
	 * @throws OperationException if any error happen
	 */
	public List<T> selectAll() throws OperationException {
		return select().execute();
	}

	@Override
	public Iterator<T> iterator() {
		return select().iterator();
	}

	/**
	 * Returns the stored object with primary key column's value equal to
	 * <code>id</code> or <code>null</code> if there is no such object. The
	 * table must have a non-composite primary key.
	 *
	 * @param id value of the primary key column
	 * @throws OperationException if any error happen
	 */
	public T selectById(Object id) throws OperationException {
		ColumnInfo primaryKey = getPrimaryKeyColumnOrThrow();
		return selectByColumnValue(primaryKey.getFullName(), id);
	}

	/**
	 * Queries one stored object by the column equality. Returns the first
	 * matching object or <code>null</code> if there is no such object.
	 *
	 * @param column column's name
	 * @param value  column's value
	 * @throws OperationException if any error happen
	 */
	public T selectByColumnValue(String column, Object value) throws OperationException {
		return select().where(column).equalsTo(value).executeSingle();
	}

	/**
	 * Builds a new {@link ReadableTable} referencing to the same SQL entity as
	 * this table and aggregating all selections by specified columns (via 'group by' functionality).
	 * This is a way to build a {@link ReadableTable} instance with fields representing a result of some aggregation function and
	 * not to call {@link Select#groupBy(String...)} every time when you perform a select operation.
	 */
	public AggregatedTableBuilder<T> aggregate() {
		return new AggregatedTableBuilder<>(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <K> MappingOperation.Builder<T, K> map(String column, Class<K> columnClass) {
		return MappingOperation.builder(this, Value.of(column), columnClass, getModelClass());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <K> MappingOperation.Builder<T, K> map(Result resultOf, Class<K> resultClass) {
		return MappingOperation.builder(this, resultOf, resultClass, getModelClass());
	}

	public Expressions<T> expressions() {
		@SuppressWarnings("unchecked")
		Expressions<T> expressions = (Expressions<T>) super.expressions();
		return expressions;
	}
}
