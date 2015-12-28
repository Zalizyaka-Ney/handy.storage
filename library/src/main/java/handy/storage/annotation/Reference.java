package handy.storage.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import handy.storage.api.Action;

/**
 * <p>
 * Marks that this column should be serialised as reference to another table.
 * Annotated field should be declared as a table in database and have an unique
 * column (i.e. declared as {@link Unique} or {@link PrimaryKey}). This column
 * will be declared as a foreign key, so you should remember about its
 * limitations.
 * </p>
 * <p>
 * Can't be use simultaneously with {@link GsonSerializable}, {@link AliasFor},
 * {@link FunctionResult}.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Reference {

	/**
	 * Action to perform on referenced data update.
	 */
	Action onUpdateAction() default Action.DEFAULT;

	/**
	 * Action to perform on referenced data deletion.
	 */
	Action onDeleteAction() default Action.DEFAULT;

}
