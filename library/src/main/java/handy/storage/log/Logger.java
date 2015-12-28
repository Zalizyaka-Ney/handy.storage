package handy.storage.log;

/**
 * Logs debug messages.
 */
public interface Logger {
	
	/**
	 * Prints a debug message.
	 *
	 * @param message message
	 */
	void d(String message);
	
	/**
	 * Prints an information message.
	 *
	 * @param message message
	 */
	void i(String message);
	
	/**
	 * Prints a warning.
	 *
	 * @param message message
	 */
	void w(String message);
	
	/**
	 * Prints an error message.
	 *
	 * @param message message
	 */
	void e(String message);
	
	/**
	 * Prints an error message.
	 *
	 * @param message message
	 * @param e exception
	 */
	void e(String message, Throwable e);

	/**
	 * Prints an exception stacktrace.
	 *
	 * @param e exception
	 */
	void logException(Throwable e);
		
}
