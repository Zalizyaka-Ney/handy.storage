package handy.storage.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Wrapper for multiple {@link CompositeUnique} annotations. Don't use it
 * simultaneously with a separate {@link CompositeUnique}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CompositeUniques {

	CompositeUnique[] value();

}
