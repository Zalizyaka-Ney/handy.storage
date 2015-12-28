package handy.storage.api;

import handy.storage.annotation.Column;
import handy.storage.annotation.CompositePrimaryKey;
import handy.storage.annotation.CompositeUnique;
import handy.storage.annotation.TableName;


/**
 * <p>
 * Interface for a model in database (a model of database table or just a model
 * to read from database). Fields, that should be saved/read, must be annotated
 * with {@link Column} annotation.
 * </p>
 * <p>
 * To set database constraints you can use {@link CompositeUnique},
 * {@link CompositePrimaryKey} annotations.
 * </p>
 * <p>
 * To set database table's name (or set the name of the table to read data from)
 * use {@link TableName} annotation.
 * </p>
 * <p>
 * Every realization supposed to be read from database must have a default
 * constructor. Private constructor is supported, you can use
 * {@link ImplicitlyUsed} annotation to suppress "unused" warning in this case.
 * Also you can set a custom way to create objects by setting custom
 * {@link ObjectCreator} via
 * {@link handy.storage.HandyStorage.Builder#setObjectCreator(Class, ObjectCreator)} (in this case
 * a default constructor is not required).
 * </p>
 */
public interface Model {

}
