package handy.storage.api;

import handy.storage.Database;
import handy.storage.exception.OperationException;

/**
 * Encapsulates actions that should be executed in one transaction . Use
 * {@link Database#performTransaction(Transaction)} to execute it.
 */
public interface Transaction {

	/**
	 * Database operations to execute in this transaction. The transaction is
	 * considered as successful if no exception was thrown in this method.
	 *
	 * @param database Database instance
	 * @throws OperationException if any error happen during this transaction
	 */
	void performQueries(Database database) throws OperationException;

}
