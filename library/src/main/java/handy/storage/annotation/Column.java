package handy.storage.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Marks the field to be a column in the database table.
 * </p>
 * 
 * <p>
 * If the attribute <code>value</code> is set, it will be used as the column's
 * name, otherwise the field's name will be used. The column's name must be
 * unique in the table.
 * </p>
 * 
 * <p>
 * Use this annotation to declare which fields should be saved to the database
 * or read from it. The fields that are not annotated with it will be not saved
 * and read. If the annotated field is <code>static</code>, the annotation will
 * be ignored, all the other modifiers (including <code>final</code>,
 * <code>transient</code>) don't affect it.
 * </p>
 * 
 * <p>
 * In a database table's column declaration you can also use {@link PrimaryKey},
 * {@link AutoIncrement}, {@link ForeignKey}, {@link GsonSerializable},
 * {@link Reference}, {@link NotNull}, {@link Unique} annotations. For models,
 * that are only read from the database, you can also use {@link AliasFor} and
 * {@link FunctionResult}.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {

	String value() default "";

}
