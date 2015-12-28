package handy.storage.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import handy.storage.api.Function;

/**
 * <p>
 * Declares this column as a result of some SQLite function.
 * </p>
 *
 * <p>
 * Use this annotation in a declaration of models, that are only read from the
 * database, it can't be used in a declaration of database table's model.
 * </p>
 * 
 * <p>
 * Can't be used simultaneously with {@link AliasFor} annotation.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FunctionResult {

	/**
	 * SQLite function.
	 */
	Function type();

	/**
	 * Arguments of the function. See details about these arguments and their
	 * limitations in {@link Function} documentation.
	 */
	String[] arguments() default { };

}
