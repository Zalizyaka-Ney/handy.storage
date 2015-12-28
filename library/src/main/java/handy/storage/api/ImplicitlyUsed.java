package handy.storage.api;

/**
 * Marks annotated constructor/field/method as used implicitly via reflection.
 * The framework doesn't rely on this annotation in any way, but it is
 * recommended to use it to suppress warning on private default constructors.
 */
public @interface ImplicitlyUsed {
}
