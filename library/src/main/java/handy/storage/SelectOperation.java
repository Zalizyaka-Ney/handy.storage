package handy.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import handy.storage.api.CursorValues;
import handy.storage.api.JoinType;
import handy.storage.api.Model;
import handy.storage.api.ObjectCreator;
import handy.storage.api.Select;
import handy.storage.base.QueryParams;
import handy.storage.exception.OperationException;
import handy.storage.log.DatabaseLog;
import handy.storage.util.ClassCast;
import handy.storage.util.Factory;

/**
 * Selects models from database.
 *
 * @param <T> model
 */
public abstract class SelectOperation<T> extends BaseSelectOperation<Select<T>, SelectOperation<T>> implements Select<T> {

	SelectOperation(Table table, List<ColumnInfo> queryColumns, Factory<QueryParams> queryParamsFactory) {
		super(table, queryColumns, queryParamsFactory);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public T executeSingle() throws OperationException {
		getQueryParams().limit(1);
		List<T> models = execute();
		if (models.isEmpty()) {
			return null;
		} else {
			return models.get(0);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Select<T> distinct() {
		getQueryParams().distinct(true);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterator<T> iterator() {
		try {
			return execute().iterator();
		} catch (OperationException e) {
			DatabaseLog.logException(e);
			return Collections.<T>emptyList().iterator();
		}
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public T executeSingleAndSafely() {
		try {
			return executeSingle();
		} catch (OperationException e) {
			DatabaseLog.logException(e);
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<T> executeSafely() {
		try {
			return execute();
		} catch (OperationException e) {
			DatabaseLog.logException(e);
			return Collections.emptyList();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final List<T> execute() throws OperationException {
		completeConfiguringQuery();
		List<T> result = doExecute(getQueryParams(), getQueryColumns());
		DatabaseLog.i(String.format("read %d objects from table '%s'", result.size(), getTableInfo().getEntity()));
		return result;
	}

	protected abstract List<T> doExecute(QueryParams filledQueryParams, List<ColumnInfo> queryColumns) throws OperationException;

	static <M extends Model> Select<M> createModelSelect(final ReadableTable<M> table, Factory<QueryParams> queryParamsFactory) {
		return createModelSelect(table, table.getObjectCreator(), queryParamsFactory);
	}

	static <M extends Model> Select<M> createModelSelect(Table table, final ObjectCreator<M> objectCreator, Factory<QueryParams> queryParamsFactory) {
		List<ColumnInfo> columns = new LinkedList<>(table.getTableInfo().getColumns());
		return new SimpleSelectOperation<>(table, columns, queryParamsFactory, getDataReaderFactory(), () -> new ModelListDataCollector<>(objectCreator));
	}

	static <C> Select<C> createColumnValuesSelect(Table table, ColumnInfo column, Class<C> type, Factory<QueryParams> queryParamsFactory) {
		final String columnName = column.getName();
		List<ColumnInfo> columns = Collections.singletonList(column);
		final TypeAdapter<C> typeAdapter = table.getDataAdapters().getTypeAdapter(type);
		return new SimpleSelectOperation<>(table, columns, queryParamsFactory,
			new DataReaderFactory() {
				@Override
				public DataReader newDataReader(Table table, List<ColumnInfo> queryColumns, QueryParams queryParams) {
					return new DataReader.ColumnDataReader(table.getDatabaseAdapter(), queryParams, typeAdapter, ClassCast.getDefaultValueForType(type), columnName);
				}
			},
			() -> new ColumnListDataCollector<>(columnName)
		);
	}

	static <C extends Model> Select<C> createReferencedColumnValuesSelect(Table table, ColumnInfo column, Class<C> type, Factory<QueryParams> queryParamsFactory) {
		return new ReferencedColumnSelect<>(table, column, type, queryParamsFactory);
	}

	/**
	 * Common implementation of select operation.
	 *
	 * @param <T> type of objects
	 */
	private static final class SimpleSelectOperation<T> extends SelectOperation<T> {
		private final DataReaderFactory dataReaderFactory;
		private final Factory<ListDataCollector<T>> dataConsumerFactory;

		SimpleSelectOperation(Table table, List<ColumnInfo> queryColumns, Factory<QueryParams> queryParamsFactory, DataReaderFactory dataReaderFactory, Factory<ListDataCollector<T>> dataConsumerFactory) {
			super(table, queryColumns, queryParamsFactory);
			this.dataReaderFactory = dataReaderFactory;
			this.dataConsumerFactory = dataConsumerFactory;
		}

		protected List<T> doExecute(QueryParams filledQueryParams, List<ColumnInfo> queryColumns) throws OperationException {
			ListDataCollector<T> dataConsumer = dataConsumerFactory.newObject();
			DataReader dataReader = dataReaderFactory.newDataReader(getOwner(), queryColumns, filledQueryParams);
			dataReader.readData(dataConsumer);
			return dataConsumer.getData();
		}
	}

	/**
	 * Implementation for selecting a column which is a reference.
	 *
	 * @param <T> model type
	 */
	private static final class ReferencedColumnSelect<T extends Model> extends SelectOperation<T> {

		private final ColumnInfo column;
		private final Class<T> columnType;
		private final String columnName;
		private final Factory<QueryParams> queryParamsFactory;

		private ReferencedColumnSelect(Table table, ColumnInfo column, Class<T> columnType, Factory<QueryParams> queryParamsFactory) {
			super(table, Collections.singletonList(column), queryParamsFactory);
			this.column = column;
			this.columnType = columnType;
			this.columnName = column.getName();
			this.queryParamsFactory = queryParamsFactory;
		}

		@Override
		protected List<T> doExecute(QueryParams queryParams, List<ColumnInfo> queryColumns) throws OperationException {
			DatabaseCore core = getOwner().getDatabaseCore();
			Table referencedTable = core.getTablesFactory().createTable(column.getReferencedTable());
			Table joinedTable = asTable("temp_table")
				.join(referencedTable, JoinType.LEFT_OUTER)
				.onReference()
				.asTable();
			final ObjectCreator<T> originalObjectCreator = core.getDataAdapters().getObjectCreator(columnType, referencedTable.getTableInfo());
			ObjectCreator<T> objectCreator = new ObjectCreator<T>() {

				@Override
				public T createObject(CursorValues values) {
					if (values.getValue(columnName) == null) {
						return null;
					} else {
						return originalObjectCreator.createObject(values);
					}
				}
			};
			return createModelSelect(joinedTable, objectCreator, queryParamsFactory).execute();
		}

	}

	/**
	 * {@link DataCollector} collecting data to a {@link List}.
	 *
	 * @param <T>
	 */
	abstract static class ListDataCollector<T> implements DataCollector {

		private ArrayList<T> data;

		void acceptValue(T value) {
			data.add(value);
		}

		@Override
		public void init(int size) {
			data = new ArrayList<>(size);
		}

		@Override
		public int getSize() {
			return data.size();
		}

		List<T> getData() {
			return data;
		}
	}

	/**
	 * {@link DataCollector} collecting model objects to a {@link List}.
	 *
	 * @param <T> model type
	 */
	static class ModelListDataCollector<T> extends ListDataCollector<T> {

		private final ObjectCreator<T> objectCreator;

		ModelListDataCollector(ObjectCreator<T> objectCreator) {
			this.objectCreator = objectCreator;
		}

		@Override
		public void accept(CursorValues values) {
			T object = objectCreator.createObject(values);
			acceptValue(object);
		}
	}

	/**
	 * {@link DataCollector} collecting column values to a {@link List}.
	 *
	 * @param <T> column type
	 */
	static class ColumnListDataCollector<T> extends ListDataCollector<T> {

		private final String columnName;

		ColumnListDataCollector(String columnName) {
			this.columnName = columnName;
		}

		@Override
		public void accept(CursorValues values) {
			T value = values.getValue(columnName);
			acceptValue(value);
		}
	}

	/**
	 * Produces {@link DataReader} objects.
	 */
	interface DataReaderFactory {
		DataReader newDataReader(Table table, List<ColumnInfo> queryColumns, QueryParams queryParams);
	}

}
