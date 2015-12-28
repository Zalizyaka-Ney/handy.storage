package handy.storage;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.List;

import handy.storage.api.Model;
import handy.storage.api.ObjectCreator;

/**
 * Converts models to {@link ContentValues} for inserting into a database or a content provider and parses models from cursor.
 *
 * @param <T> model class
 */
public class ModelAdapter<T extends Model> {

	private final ContentValuesParser<T> contentValuesParser;
	private final CursorReader cursorReader;
	private final ObjectCreator<T> objectCreator;

	ModelAdapter(ContentValuesParser<T> contentValuesParser, CursorReader cursorReader, ObjectCreator<T> objectCreator) {
		this.contentValuesParser = contentValuesParser;
		this.cursorReader = cursorReader;
		this.objectCreator = objectCreator;
	}

	/**
	 * Converts a model to a {@link ContentValues}.
	 */
	public ContentValues modelToContentValues(T model) {
		return contentValuesParser.parseContentValues(model);
	}

	/**
	 * Extract a list of models from the cursor. Closes the cursor.
	 *
	 * @throws handy.storage.exception.IllegalUsageException if cursor doesn't contains all columns declared in the model class
	 */
	public List<T> cursorToModels(Cursor cursor) {
		try {
			SelectOperation.ModelListDataCollector<T> dataConsumer = new SelectOperation.ModelListDataCollector<>(objectCreator);
			cursorReader.readData(cursor, null, null, dataConsumer);
			return dataConsumer.getData();
		} finally {
			cursor.close();
		}
	}

}
