package handy.storage;

import android.text.TextUtils;

import java.util.List;

import handy.storage.api.InspectData;
import handy.storage.api.JoinType;
import handy.storage.api.Mapping;
import handy.storage.api.Model;
import handy.storage.api.Result;
import handy.storage.api.Select;
import handy.storage.api.Value;
import handy.storage.base.DatabaseAdapter;
import handy.storage.base.QueryParams;
import handy.storage.exception.OperationException;
import handy.storage.util.ClassCast;
import handy.storage.util.Factory;

/**
 * General table entity without binding to a model.
 */
public class Table extends AbstractTable {

	private final Expressions<?> expressions;

	Table(TableInfo tableInfo, DatabaseAdapter databaseAdapter, DatabaseCore databaseCore, Factory<QueryParams> queryParamsFactory) {
		super(tableInfo, databaseAdapter, databaseCore, queryParamsFactory);
		this.expressions = new Expressions(this);
	}

	/**
	 * Builds a new {@link ReadableTable} referencing to the same SQL entity as
	 * this table. By using this method you can set or change the model's class
	 * reading from the database.
	 */
	public <T extends Model> ReadableTable<T> asReadableTable(Class<T> modelClass) {
		return getDatabaseCore().getTablesFactory().createProjection(modelClass, getTableInfo());
	}

	/**
	 * Creates a new joined table builder (this table will be the left table in
	 * the join, <code>anotherTable</code> - the right table). The joining
	 * tables must have a name (if a table doesn't have a name, you can set it
	 * via {@link Table#as(String)}).
	 *
	 * @param anotherTable table to join
	 * @param joinType     type of the join
	 */
	public JoinBuilder join(Table anotherTable, JoinType joinType) {
		return new JoinBuilder(this, anotherTable, joinType);
	}

	/**
	 * Joins the table with the default join type ({@link JoinType#INNER}). See
	 * {@link #join(Table, JoinType)} for details.
	 */
	public JoinBuilder join(Table anotherTable) {
		return join(anotherTable, JoinType.INNER);
	}

	/**
	 * Provides a batch of common selects of aggregated SQL functions (count, exists etc.).
	 */
	public InspectData inspectData() {
		return new InspectDataOperation(this);
	}

	/**
	 * Counts the all records in this table.
	 *
	 * @throws OperationException if any error happen during this operation
	 */
	public int countAll() throws OperationException {
		return inspectData().count();
	}

	/**
	 * Creates a new {@link Table} instance which referencing to the same SQL
	 * entity as this one and has the given alias.
	 *
	 * @param tableAlias alias for table
	 */
	public Table as(String tableAlias) {
		return createNewTableEntity(TableInfoFactory.createAliasTableInfo(getTableInfo(), tableAlias));
	}

	/**
	 * Returns the table representing values of a single column or SQL entity
	 * (i.e. function etc.).
	 *
	 * @param columnOrEntity column's name or SQL entity
	 */
	public ColumnValuesTable columnValues(String columnOrEntity) {
		return getColumnValues(false, Value.of(columnOrEntity), null);
	}

	/**
	 * Returns the table representing values of a single column or SQL entity
	 * (i.e. function etc.).
	 *
	 * @param resultOf value to select
	 */
	public ColumnValuesTable columnValues(Result resultOf) {
		return getColumnValues(false, resultOf, null);
	}

	/**
	 * Returns the table representing filtered values of a single column or SQL
	 * entity (i.e. function etc.).
	 *
	 * @param columnOrEntity column's name or SQL entity
	 * @param filter         expression which must be fulfilled by values included in the
	 *                       result table
	 */
	public ColumnValuesTable columnValues(String columnOrEntity, Expression filter) {
		return getColumnValues(false, Value.of(columnOrEntity), filter);
	}

	/**
	 * Returns the table representing filtered values of a single column or SQL
	 * entity (i.e. function etc.).
	 *
	 * @param resultOf value to select
	 * @param filter   expression which must be fulfilled by values included in the
	 */
	public ColumnValuesTable columnValues(Result resultOf, Expression filter) {
		return getColumnValues(false, resultOf, filter);
	}

	/**
	 * Returns the table representing distinct values of a single column or SQL
	 * entity (i.e. function etc.).
	 *
	 * @param column column's name or SQL entity
	 */
	public ColumnValuesTable distinctColumnValues(String column) {
		return getColumnValues(true, Value.of(column), null);
	}

	/**
	 * Returns the table representing distinct values of a single column or SQL
	 * entity (i.e. function etc.).
	 *
	 * @param resultOf value to select
	 */
	public ColumnValuesTable distinctColumnValues(Result resultOf) {
		return getColumnValues(true, resultOf, null);
	}

	/**
	 * Returns the table representing filtered distinct values of a single
	 * column or SQL entity (i.e. function etc.).
	 *
	 * @param column column's name or SQL entity
	 * @param filter expression which must be fulfilled by values included in the
	 *               result table
	 */
	public ColumnValuesTable distinctColumnValues(String column, Expression filter) {
		return getColumnValues(true, Value.of(column), filter);
	}

	/**
	 * Returns the table representing filtered distinct values of a single
	 * column or SQL entity (i.e. function etc.).
	 *
	 * @param resultOf value to select
	 * @param filter   expression which must be fulfilled by values included in the
	 */
	public ColumnValuesTable distinctColumnValues(Result resultOf, Expression filter) {
		return getColumnValues(true, resultOf, filter);
	}

	private void checkType(Class<?> argument, Class<?> declared) {
		if (declared != null && !ClassCast.isValueAssignable(declared, argument)) {
			throw new IllegalArgumentException("you should pass the same type as declared in the model: " + declared);
		}
	}

	private ColumnValuesTable getColumnValues(boolean distinct, Value value, Expression filter) {
		// TODO: use all value data
		String column = value.getEntity();
		checkExpressionOwner(filter);
		return new ColumnValuesTable(
			column,
			TableInfoFactory.createSingleColumnTableInfo(getTableInfo(), distinct, column, filter),
			getDatabaseAdapter(),
			getDatabaseCore(),
			getQueryParamsFactory()
		);
	}

	/**
	 * Starts a selection operation for column's values.
	 *
	 * @param columnOrExpression column's name or SQL entity
	 * @param columnType         type of selecting values (must be assignable from the one from
	 *                           the model's declaration)
	 */
	public <T> Select<T> select(String columnOrExpression, Class<T> columnType) {
		return createSelect(Value.of(columnOrExpression), columnType);
	}

	/**
	 * Starts a selection operation for column's values.
	 *
	 * @param resultOf   value type
	 * @param resultType type of selecting values (must be assignable from the one from
	 */
	public <T> Select<T> select(Result resultOf, Class<T> resultType) {
		return createSelect(resultOf, resultType);
	}

	private <T> Select<T> createSelect(Value value, Class<T> valueType) {
		TypeAdapter<T> typeAdapter = getDataAdapters().getTypeAdapter(valueType);
		ColumnInfo column = ColumnInfo.createQueryColumnInfo(getTableInfo(), value, valueType, typeAdapter.getColumnType());
		if (column.isReferenceToTable()) {
			checkType(valueType, column.getReferencedTable());
			@SuppressWarnings("unchecked")
			Select<T> select = (Select<T>) SelectOperation.createReferencedColumnValuesSelect(this, column, (Class<Model>) valueType, getQueryParamsFactory());
			return select;
		} else {
			checkType(valueType, column.getFieldType());
			return SelectOperation.createColumnValuesSelect(this, column, valueType, getQueryParamsFactory());
		}
	}

	/**
	 * Selects all column's values.
	 *
	 * @param columnOrExpression column's name or SQL entity
	 * @param columnType         type of selecting values (must be assignable from the one from
	 *                           the model's declaration)
	 * @throws OperationException if any error happen
	 */
	public <T> List<T> selectAll(String columnOrExpression, Class<T> columnType) throws OperationException {
		return select(columnOrExpression, columnType).execute();
	}

	/**
	 * Selects all column's values.
	 *
	 * @param resultOf   value to select
	 * @param resultType type of selecting values (must be assignable from the one from
	 *                   the model's declaration)
	 * @throws OperationException if any error happen
	 */
	public <T> List<T> selectAll(Result resultOf, Class<T> resultType) throws OperationException {
		return select(resultOf, resultType).execute();
	}

	/**
	 * Selects a single result of an aggregating SQL function.
	 *
	 * @param resultOf   value to select
	 * @param resultType type for the result
	 * @throws OperationException if any error happen during reading from the database
	 */
	public <T> T selectSingle(Result resultOf, Class<T> resultType) throws OperationException {
		return select(resultOf, resultType).executeSingle();
	}

	/**
	 * Builds a full name for the column (i.e. with a table's name).
	 *
	 * @param table  name of the table the column belongs to
	 * @param column name of the column
	 */
	public static String fullColumnName(String table, String column) {
		if (TextUtils.isEmpty(table)) {
			return column;
		} else {
			return table + '.' + column;
		}
	}

	/**
	 * Starts a creation of a {@link Mapping} operation.
	 *
	 * @param column      name of column which values will be used as keys
	 * @param columnClass class of that column
	 * @param <K>         type of key in mapping
	 */
	public <K> MappingOperation.Builder<?, K> map(String column, Class<K> columnClass) {
		return MappingOperation.builder(this, Value.of(column), columnClass, null);
	}

	/**
	 * Starts a creation of a {@link Mapping} operation.
	 *
	 * @param <K>         type of key in mapping
	 * @param resultOf    function which result will be used as a key
	 * @param resultClass class of result
	 */
	public <K> MappingOperation.Builder<?, K> map(Result resultOf, Class<K> resultClass) {
		return MappingOperation.builder(this, resultOf, resultClass, null);
	}

	/**
	 * Returns a factory of expressions to use with this table. Note that some factory methods throws an exception if the table is not bound to a model class
	 * (i.e. is not a {@link ReadableTable}).
	 */
	public Expressions<?> expressions() {
		return expressions;
	}
}
