package handy.storage.annotation;

import handy.storage.base.OnConflictStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Columns set that should be unique in the table. If you need to declare
 * multiple composite unique restriction for a model, use
 * {@link CompositeUniques} annotation.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CompositeUnique {

	/**
	 * Array of column names from this table.
	 */
	String[] columns();

	/**
	 * What action should be performed on conflict during insertion into the
	 * database table. {@link OnConflictStrategy#DEFAULT} is used by default.
	 */
	OnConflictStrategy onConflictStrategy() default OnConflictStrategy.DEFAULT;

}
