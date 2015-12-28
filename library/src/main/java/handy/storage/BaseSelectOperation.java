package handy.storage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import handy.storage.api.DataSelection;
import handy.storage.base.Order;
import handy.storage.base.QueryParams;
import handy.storage.util.Factory;

/**
 * Base implementation of {@link handy.storage.api.DataSelection} operation.
 *
 * @param <Interface> interface to realize
 * @param <Subclass>  final realization type
 */
@SuppressWarnings("unchecked")
abstract class BaseSelectOperation<Interface extends DataSelection<?>, Subclass extends BaseSelectOperation<Interface, Subclass>>
	extends BaseOperation<Subclass> implements DataSelection<Interface> {

	private final QueryParams queryParams;
	private final TableInfo tableInfo;
	private final List<ColumnInfo> queryColumns;
	private final QueryAdapter queryAdapter;

	BaseSelectOperation(Table table, List<ColumnInfo> queryColumns, Factory<QueryParams> queryParamsFactory) {
		super(table);
		queryParams = queryParamsFactory.newObject();
		this.queryColumns = queryColumns;
		tableInfo = table.getTableInfo();
		queryAdapter = table.getQueryAdapter();
	}

	protected static SelectOperation.DataReaderFactory getDataReaderFactory() {
		return new SelectOperation.DataReaderFactory() {
			@Override
			public DataReader newDataReader(Table table, List<ColumnInfo> queryColumns, QueryParams queryParams) {
				return new DataReader.ModelDataReader(
					table.getDatabaseCore(),
					table.getDatabaseAdapter(),
					new CursorReader(queryColumns, table.getDataAdapters()),
					queryParams,
					initColumnIndexes(queryColumns)
				);
			}

			private Map<ColumnInfo, Integer> initColumnIndexes(List<ColumnInfo> queryColumns) {
				Map<ColumnInfo, Integer> indexes = new HashMap<>();
				int index = 0;
				for (ColumnInfo column : queryColumns) {
					indexes.put(column, index++);
				}
				return indexes;
			}
		};
	}

	private void fillQueryFromTableInfo() {
		queryParams.from(tableInfo.getEntity());
		String[] columnNames = new String[queryColumns.size()];
		int index = 0;
		for (ColumnInfo column : queryColumns) {
			columnNames[index++] = column.getEntityDeclaration();
		}
		queryParams.columns(columnNames);
	}

	protected void completeConfiguringQuery() {
		fillQueryFromTableInfo();
		queryParams.where(getWhereClause());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Table asTable(String tableName) {
		completeConfiguringQuery();
		return new Table(
			TableInfoFactory.createSelectionTableInfo(tableName, queryColumns, queryParams.toRawSqlQuery()),
			getDatabaseAdapter(),
			getOwner().getDatabaseCore(),
			QueryParams.DEFAULT_FACTORY);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Table asTable() {
		return asTable("");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Interface limit(int limit) {
		queryParams.limit(limit);
		return (Interface) this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Interface offset(int offset) {
		queryParams.offset(offset);
		return (Interface) this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Interface groupBy(String... columns) {
		String[] fullColumnNames = new String[columns.length];
		for (int i = 0; i < columns.length; i++) {
			fullColumnNames[i] = queryAdapter.getFullColumnName(columns[i]);
		}
		queryParams.groupBy(fullColumnNames);
		return (Interface) this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Interface having(Expression expression) {
		getOwner().checkExpressionOwner(expression);
		queryParams.having(expression.toString());
		return (Interface) this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Interface orderBy(String column, Order order) {
		queryParams.orderBy(queryAdapter.getFullColumnName(column), order);
		return (Interface) this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Interface orderBy(String column) {
		queryParams.orderBy(queryAdapter.getFullColumnName(column));
		return (Interface) this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public QueryParams asQueryParams() {
		completeConfiguringQuery();
		return queryParams.clone();
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public Interface addVirtualColumn(String alias, String entity) {
		if (tableInfo.getColumnInfo(alias) != null) {
			throw new IllegalArgumentException("table already has a column " + alias);
		}
		ColumnInfo virtualColumn = ColumnInfo.createVirtualColumn(tableInfo.getName(), alias, entity);
		queryColumns.add(virtualColumn);
		return (Interface) this;
	}

	protected QueryParams getQueryParams() {
		return queryParams;
	}

	protected List<ColumnInfo> getQueryColumns() {
		return queryColumns;
	}

	protected TableInfo getTableInfo() {
		return tableInfo;
	}
}
