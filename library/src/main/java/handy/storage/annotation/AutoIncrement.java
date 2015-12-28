package handy.storage.annotation;

import handy.storage.DatabaseBuilder;
import handy.storage.WritableTable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * The annotated field also must be annotated with {@link PrimaryKey} and its
 * type must be <code>long</code> or <code>java.lang.Long</code>.
 * </p>
 * 
 * <p>
 * If at the moment of insertion into the database this field's value is not set
 * (the value is null or it is less than or equal to 0), the database replace it
 * with an automatically generated unique positive number. This number will be
 * set to this field in runtime if <code>setIdsOnInsert</code> isn't disabled
 * (see {@link DatabaseBuilder#setIdOnInsertByDefault(boolean)},
 * {@link WritableTable#setIdOnInsert(boolean)}, this functionality is enabled
 * by default).
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AutoIncrement {

}
