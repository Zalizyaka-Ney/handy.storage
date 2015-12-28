package handy.storage.annotation;

import com.google.gson.Gson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks that this column should be serialised as <code>json</code> string in
 * database (using {@link Gson} type adapter). Can be used only for columns that
 * have no other type adapter and are not annotated with {@link Reference}. You
 * can set custom {@link Gson} object to use in serialisation using
 * {@link handy.storage.HandyStorage.Builder#setCustomGson(Gson)} method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface GsonSerializable {
}
