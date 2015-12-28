package handy.storage.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import handy.storage.base.OnConflictStrategy;

/**
 * Declares a set of columns that should be the private key in the table. The
 * annotated table mustn't have any column annotated with {@link PrimaryKey}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CompositePrimaryKey {

	/**
	 * Array of table's columns which values will be a primary key in this
	 * table.
	 */
	String[] columns();

	/**
	 * What action should be performed on conflict during insertion into the
	 * database table. {@link OnConflictStrategy#DEFAULT} is used by default.
	 */
	OnConflictStrategy onConflictStrategy() default OnConflictStrategy.DEFAULT;

}
