package handy.storage.annotation;

import handy.storage.base.OnConflictStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks that this column should be declared with UNIQUE modifier.
 * 
 * @see CompositeUnique
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Unique {

	/**
	 * What action should be performed on conflict during insertion into the
	 * database table. {@link OnConflictStrategy#DEFAULT} is used by default.
	 */
	OnConflictStrategy value() default OnConflictStrategy.DEFAULT;

}
