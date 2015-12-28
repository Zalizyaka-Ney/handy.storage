package handy.storage;

import android.database.Cursor;
import android.text.TextUtils;

import handy.storage.api.Function;
import handy.storage.api.InspectData;
import handy.storage.api.Result;
import handy.storage.exception.OperationException;
import handy.storage.log.DatabaseLog;
import handy.storage.log.PerformanceTimer;

/**
 * A batch of operations selecting the most common aggregated values from a table.
 */
public class InspectDataOperation extends BaseOperation<InspectDataOperation> implements InspectData {

	InspectDataOperation(Table table) {
		super(table);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int count() throws OperationException {
		PerformanceTimer.startInterval("count");
		int count = getDatabaseAdapter().count(getOwner().getTableEntity(), getWhereClause());
		PerformanceTimer.endInterval();
		return count;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int countSafely() {
		try {
			return count();
		} catch (OperationException e) {
			DatabaseLog.logException(e);
			return -1;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean exists() throws OperationException {
		PerformanceTimer.startInterval("execute exists()");
		DatabaseLog.i("checking if data exists");
		StringBuilder selectBuilder = new StringBuilder("SELECT EXISTS(SELECT 1 FROM ");
		selectBuilder.append(getOwner().getTableEntity());
		String whereClause = getWhereClause();
		DatabaseLog.i("selection is " + whereClause);
		if (!TextUtils.isEmpty(whereClause)) {
			selectBuilder.append(" WHERE ");
			selectBuilder.append(whereClause);
		}
		selectBuilder.append(")");
		String select = selectBuilder.toString();
		DatabaseLog.d(select);
		Cursor cursor = getDatabaseAdapter().rawQuery(select);
		boolean result = false;
		try {
			if (cursor.moveToFirst()) {
				result = cursor.getInt(0) != 0;
			}
		} finally {
			cursor.close();
			PerformanceTimer.endInterval();
		}
		DatabaseLog.i("result is " + result);
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean existsSafely() {
		try {
			return exists();
		} catch (OperationException e) {
			DatabaseLog.logException(e);
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> T getLargestValueOf(String column, Class<T> columnClass) throws OperationException {
		return getValueOf(Function.MAX, column, columnClass);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> T getLargestValueOf(String column, Class<T> columnClass, T defaultValue) {
		return getValueOf(Function.MAX, column, columnClass, defaultValue);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> T getSmallestValueOf(String column, Class<T> columnClass) throws OperationException {
		return getValueOf(Function.MIN, column, columnClass);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> T getSmallestValueOf(String column, Class<T> columnClass, T defaultValue) {
		return getValueOf(Function.MIN, column, columnClass, defaultValue);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> T getAverageValueOf(String column, Class<T> columnClass) throws OperationException {
		return getValueOf(Function.AVG, column, columnClass);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> T getAverageValueOf(String column, Class<T> columnClass, T defaultValue) {
		return getValueOf(Function.AVG, column, columnClass, defaultValue);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> T getSumOf(String column, Class<T> columnClass) throws OperationException {
		return getValueOf(Function.TOTAL, column, columnClass);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> T getSumOf(String column, Class<T> columnClass, T defaultValue) {
		T result = getOwner().select(Result.of("SUM(" + column + ")"), columnClass).where(getWhereExpression()).executeSingleAndSafely();
		return result != null ? result : defaultValue;
	}

	private <T> T getValueOf(Function function, String column, Class<T> columnClass) throws OperationException {
		return getOwner().select(Result.of(function, column), columnClass).where(getWhereExpression()).executeSingle();
	}

	private <T> T getValueOf(Function function, String column, Class<T> columnClass, T defaultValue) {
		T result = null;
		try {
			result = getValueOf(function, column, columnClass);
		} catch (OperationException e) {
			DatabaseLog.logException(e);
		}
		return result != null ? result : defaultValue;
	}

}
