package handy.storage;

import handy.storage.api.CursorValues;

/**
 * Accumulates data in a collection.
 */
interface DataCollector {

	void accept(CursorValues values);

	void init(int size);

	int getSize();

}
