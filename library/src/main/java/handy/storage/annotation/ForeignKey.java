package handy.storage.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import handy.storage.api.Action;
import handy.storage.api.Model;

/**
 * Marks that this column is a foreign key.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ForeignKey {

	/**
	 * Class of the database table's model, the referenced column belongs to.
	 */
	Class<? extends Model> modelClass();

	/**
	 * Referenced column name.
	 */
	String column();

	/**
	 * Action to perform on referenced data update.
	 */
	Action onUpdateAction() default Action.DEFAULT;

	/**
	 * Action to perform on referenced data deletion.
	 */
	Action onDeleteAction() default Action.DEFAULT;

}
