package handy.storage.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Marks the column to be an alias for some SQLite entity (for example,
 * <code>"MAX(yourColumn, 2)"</code>).
 * Can be used for renaming columns and for clarifying to witch table belongs
 * the column (in this case set value to <code>"yourTable.yourColumn"</code> ).
 * </p>
 * 
 * <p>
 * Use this annotation in a declaration of models, that are only read from the
 * database, it can't be used in a declaration of database table's model.
 * </p>
 * 
 * <p>
 * Can't be used simultaneously with {@link FunctionResult} annotation.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AliasFor {

	String value();

}
