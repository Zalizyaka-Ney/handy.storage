package handy.storage;

import handy.storage.api.Model;
import handy.storage.base.QueryParams;

/**
 * Builds a {@link ReadableTable} instance that always aggregates a selection on some columns (i.e. uses <code>'group by'</code> functionality).
 *
 * @param <T> type of model (in the original table)
 */
public class AggregatedTableBuilder<T extends Model> {

	private final ReadableTable<T> table;
	private final QueryParams queryParams = new QueryParams();

	AggregatedTableBuilder(ReadableTable<T> table) {
		this.table = table;
	}

	/**
	 * Adds columns to group by.
	 *
	 * @param columns column names
	 * @return this object
	 */
	public AggregatedTableBuilder<T> groupBy(String... columns) {
		queryParams.groupBy(columns);
		return this;
	}

	/**
	 * Creates a new aggregated readable table parameterized with the same model class as the original table.
	 */
	public ReadableTable<T> asReadableTable() {
		return asReadableTable(table.getModelClass());
	}

	/**
	 * Creates a new aggregated readable table.
	 *
	 * @param modelClass class of model
	 * @param <M>        type of model
	 */
	public <M extends Model> ReadableTable<M> asReadableTable(Class<M> modelClass) {
		return table.getDatabaseCore().getTablesFactory().createProjection(
			modelClass,
			table.getTableInfo(),
			QueryParams.cloneObjectFactory(queryParams)
		);
	}

}
