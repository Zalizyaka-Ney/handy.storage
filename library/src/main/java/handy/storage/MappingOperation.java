package handy.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import handy.storage.api.CursorValues;
import handy.storage.api.Mapping;
import handy.storage.api.Model;
import handy.storage.api.ObjectCreator;
import handy.storage.api.Result;
import handy.storage.api.Value;
import handy.storage.base.QueryParams;
import handy.storage.exception.IllegalUsageException;
import handy.storage.exception.OperationException;
import handy.storage.log.DatabaseLog;
import handy.storage.util.Factory;

/**
 * An operation that maps values of one  column to a corresponding value of another column or a model object (or to a list of such values or objects).
 *
 * @param <K> key column type
 * @param <V> value column type
 */
public class MappingOperation<K, V> extends BaseSelectOperation<Mapping<K, V>, MappingOperation<K, V>> implements Mapping<K, V> {

	private final Factory<MapDataCollector<K, V>> dataCollectorFactory;

	MappingOperation(Table table, List<ColumnInfo> queryColumns, Factory<QueryParams> queryParamsFactory, Factory<MapDataCollector<K, V>> dataCollectorFactory) {
		super(table, queryColumns, queryParamsFactory);
		this.dataCollectorFactory = dataCollectorFactory;
		getQueryParams().distinct(true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<K, V> execute() throws OperationException {
		completeConfiguringQuery();
		DataReader dataReader = getDataReaderFactory().newDataReader(getOwner(), getQueryColumns(), getQueryParams());
		MapDataCollector<K, V> dataCollector = dataCollectorFactory.newObject();
		dataReader.readData(dataCollector);
		DatabaseLog.i(String.format("mapped %d values from table '%s'", dataCollector.getSize(), getTableInfo().getEntity()));
		return dataCollector.getData();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<K, V> executeSafely() {
		try {
			return execute();
		} catch (OperationException e) {
			DatabaseLog.logException(e);
			return Collections.emptyMap();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Mapping<K, V> setDistinct(boolean distinct) {
		getQueryParams().distinct(distinct);
		return this;
	}

	/**
	 * Builds a {@link MappingOperation}.
	 *
	 * @param <T> type of model
	 * @param <K> type of keys
	 */
	public static final class Builder<T extends Model, K> {

		private final Class<T> modelClass;
		private final Table table;
		private final ColumnInfo keyColumn;
		private final GetValueMethod<K> getKeyMethod;

		private List<ColumnInfo> columns;

		private Builder(Table table, Class<T> originalModelClass, ColumnInfo keyColumn) {
			this.modelClass = originalModelClass;
			this.table = table;
			this.keyColumn = keyColumn;
			this.getKeyMethod = getValueMethod(keyColumn.getName(), null);
		}

		/**
		 * Builds a mapping to another column's value. If there are a few values corresponding to the value of the key column,
		 * the first value will be used (according to the ordering settings).
		 *
		 * @param column name of column
		 * @param valueClass type of column
		 * @param <V> type of column
		 */
		public <V> Mapping<K, V> to(String column, Class<V> valueClass) {
			return createMappingToValue(Value.of(column), valueClass);
		}

		/**
		 * Builds a mapping to a list of another column's values. By default it returns only unique values,
		 * if you want to change this - use {@link Mapping#setDistinct(boolean)}.
		 *
		 * @param column     name of column
		 * @param valueClass type of column
		 * @param <V>        type of column
		 */
		public <V> Mapping<K, List<V>> toListOf(String column, Class<V> valueClass) {
			return createMappingToList(Value.of(column), valueClass);
		}

		/**
		 * Builds a mapping to a list of values. By default it returns only unique values,
		 * if you want to change this - use {@link Mapping#setDistinct(boolean)}.
		 *
		 * @param resultOf   function call
		 * @param resultClass type of resultOf
		 * @param <V>        type of resultOf
		 */
		public <V> Mapping<K, List<V>> toListOf(Result resultOf, Class<V> resultClass) {
			return createMappingToList(resultOf, resultClass);
		}

		private <V> Mapping<K, List<V>> createMappingToList(Value value, Class<V> valueClass) {
			prepareMappingToColumn(value, valueClass);
			return createMapping(
				listDataCollectorFactory(getValueMethod(value.getName(), valueClass))
			);
		}

		/**
		 * Builds a mapping to this <code>value</code> (either column or SQL function). If there are a few values corresponding
		 * to the value of the key column (that is possible when the function depends not only on the key column),
		 * the first value will be used (according to the ordering settings).
		 *
		 * @param resultOf    function call
		 * @param resultClass type of value
		 * @param <V> type of function result
		 */
		public <V> Mapping<K, V> to(Result resultOf, Class<V> resultClass) {
			return createMappingToValue(resultOf, resultClass);
		}

		private <V> Mapping<K, V> createMappingToValue(Value value, Class<V> valueClass) {
			prepareMappingToColumn(value, valueClass);
			return createMapping(
				flatDataCollectorFactory(getValueMethod(value.getName(), valueClass))
			);
		}

		/**
		 * Builds a mapping to a model object having that key column value. If there are a few such objects,
		 * the first one will be used (according to the ordering settings).
		 */
		public Mapping<K, T> toModel() {
			prepareMappingToModel();
			return createMapping(
				flatDataCollectorFactory(getModelMethod())
			);
		}

		/**
		 * Builds a mapping to a list of model objects having that key column value. By default it returns only unique objects,
		 * if you want to change this - use {@link Mapping#setDistinct(boolean)}.
		 */
		public Mapping<K, List<T>> toListOfModels() {
			prepareMappingToModel();
			return createMapping(
				listDataCollectorFactory(getModelMethod())
			);
		}

		private GetValueMethod<T> getModelMethod() {
			ObjectCreator<T> objectCreator = table.getDataAdapters().getObjectCreator(modelClass, table.getTableInfo());
			return objectCreator::createObject;
		}

		@SuppressWarnings("UnusedParameters")
		private <V> GetValueMethod<V> getValueMethod(String column, Class<V> columnClass) {
			return values -> values.getValue(column);
		}

		private void prepareMappingToColumn(Value value, Class<?> columnClass) {
			ColumnInfo valueColumn = createColumnInfo(table, value, columnClass);
			columns = Arrays.asList(keyColumn, valueColumn);
		}

		private void prepareMappingToModel() {
			checkTableHasBoundModelClass();
			columns = table.getTableInfo().getColumns();
		}

		private void checkTableHasBoundModelClass() {
			if (modelClass == null) {
				throw new IllegalUsageException("the table is not bound to a model (i.e. it is not a ReadableTable)");
			}
		}

		private <V> Mapping<K, V> createMapping(Factory<MapDataCollector<K, V>> dataCollectorFactory) {
			return new MappingOperation<>(
				table,
				columns,
				table.getQueryParamsFactory(),
				dataCollectorFactory);
		}

		private <V> Factory<MapDataCollector<K, V>> flatDataCollectorFactory(GetValueMethod<V> getValueMethod) {
			return () -> new FlatMapDataCollector<>(getKeyMethod, getValueMethod);
		}

		private <V> Factory<MapDataCollector<K, List<V>>> listDataCollectorFactory(GetValueMethod<V> getValueMethod) {
			return () -> new MapToListDataCollector<>(getKeyMethod, getValueMethod);
		}

		private static ColumnInfo createColumnInfo(Table table, Value value, Class<?> columnClass) {
			return ColumnInfo.createQueryColumnInfo(
				table.getTableInfo(),
				value,
				columnClass,
				table.getDataAdapters().getTypeAdapter(columnClass).getColumnType());
		}

	}

	static <T extends Model, K> Builder<T, K> builder(Table table, Value keyValue, Class<K> columnClass, Class<T> modelClass) {
		return new Builder<>(table, modelClass, Builder.createColumnInfo(table, keyValue, columnClass));
	}

	/**
	 * Collects data to a map.
	 * @param <K> key type
	 * @param <V> value type
	 */
	private interface MapDataCollector<K, V> extends DataCollector {
		Map<K, V> getData();
	}

	/**
	 * base implementation of {@link handy.storage.MappingOperation.MapDataCollector}.
	 * @param <K> key type
	 * @param <V> value type
	 * @param <E> item (of value) type
	 */
	private abstract static class BaseMapDataCollector<K, V, E> implements MapDataCollector<K, V> {
		private final GetValueMethod<K> getKeyMethod;
		private final GetValueMethod<E> getValueMethod;

		private Map<K, V> data;

		protected BaseMapDataCollector(GetValueMethod<K> getKeyMethod, GetValueMethod<E> getValueMethod) {
			this.getKeyMethod = getKeyMethod;
			this.getValueMethod = getValueMethod;
		}

		@Override
		public int getSize() {
			return data.size();
		}

		@Override
		public void accept(CursorValues values) {
			K key = getKeyMethod.call(values);
			if (shouldAcceptKey(key)) {
				E value = getValueMethod.call(values);
				acceptKeyAndValue(key, value);
			}
		}

		protected abstract boolean shouldAcceptKey(K key);

		protected abstract void acceptKeyAndValue(K key, E value);

		public Map<K, V> getData() {
			return data;
		}

		@Override
		public void init(int size) {
			data = new LinkedHashMap<>(size);
		}
	}

	/**
	 * Maps a key to a single value.
	 * @param <K> key type
	 * @param <V> value type
	 */
	private static class FlatMapDataCollector<K, V> extends BaseMapDataCollector<K, V, V> {

		protected FlatMapDataCollector(GetValueMethod<K> getKeyMethod, GetValueMethod<V> getValueMethod) {
			super(getKeyMethod, getValueMethod);
		}

		@Override
		protected boolean shouldAcceptKey(K key) {
			return !getData().containsKey(key);
		}

		@Override
		protected void acceptKeyAndValue(K key, V value) {
			getData().put(key, value);
		}

	}

	/**
	 * Maps a key to a list of values
	 * @param <K> key type
	 * @param <V> value type
	 */
	private static class MapToListDataCollector<K, V> extends BaseMapDataCollector<K, List<V>, V> {

		protected MapToListDataCollector(GetValueMethod<K> getKeyMethod, GetValueMethod<V> getValueMethod) {
			super(getKeyMethod, getValueMethod);
		}

		@Override
		protected boolean shouldAcceptKey(K key) {
			return true;
		}

		@Override
		protected void acceptKeyAndValue(K key, V value) {
			Map<K, List<V>> data = getData();
			List<V> values = data.get(key);
			if (values == null) {
				values = new ArrayList<>();
				data.put(key, values);
			}
			values.add(value);
		}

	}

	/**
	 * Gets a value from {@link CursorValues}.
	 * @param <T> type of value
	 */
	private interface GetValueMethod<T> {
		T call(CursorValues values);
	}

}
