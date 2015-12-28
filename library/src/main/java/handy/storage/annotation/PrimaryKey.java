package handy.storage.annotation;

import handy.storage.base.OnConflictStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks field to be a primary key in the database.
 * 
 * @see AutoIncrement
 * @see CompositePrimaryKey
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PrimaryKey {

	/**
	 * What action should be performed on conflict during insertion into the
	 * database table. {@link OnConflictStrategy#DEFAULT} is used by default.
	 */
	OnConflictStrategy value() default OnConflictStrategy.DEFAULT;

}
