package handy.storage;

import android.text.TextUtils;

import handy.storage.api.Delete;
import handy.storage.api.Model;
import handy.storage.api.Select;
import handy.storage.base.Order;
import handy.storage.exception.OperationException;
import handy.storage.log.DatabaseLog;
import handy.storage.log.PerformanceTimer;

/**
 * Deletes records in a table.
 * 
 * @param <T>
 *            model class
 */
public class DeleteOperation<T extends Model> extends BaseOperation<DeleteOperation<T>> implements Delete<T> {
	
	private boolean limitedMode = false;
	private Select<T> selectToDelete;

	DeleteOperation(WritableTable<T> table) {
		super(table);
		selectToDelete = table.select();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int execute() throws OperationException {
		PerformanceTimer.startInterval("delete");
		int removedCount = limitedMode ? executeLimited() : executeNormal();
		PerformanceTimer.endInterval();
		return removedCount;
	}

	private int executeNormal() throws OperationException {
		return getDatabaseAdapter().remove(getOwner().getTableEntity(), getWhereClause());
	}
	
	@SuppressWarnings("unchecked")
	private int executeLimited() throws OperationException {
		String where = getWhereClause();		
		if (!TextUtils.isEmpty(where)) {
			selectToDelete.where(getOwner().expressions().raw(where));
		}
		return ((WritableTable<T>) getOwner()).deleteAllFrom(selectToDelete);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Delete<T> limit(int limit) {
		limitedMode = true;
		selectToDelete.limit(limit);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Delete<T> orderBy(String column) {
		limitedMode = true;
		selectToDelete.orderBy(column);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Delete<T> orderBy(String column, Order order) {
		limitedMode = true;
		selectToDelete.orderBy(column, order);
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int executeSafely() {
		try {
			return execute();
		} catch (OperationException e) {
			DatabaseLog.logException(e);
			return -1;
		}
	}

}
