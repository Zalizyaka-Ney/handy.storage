package handy.storage.log;

import android.util.Log;
import android.util.SparseBooleanArray;

/**
 * Implementation of {@link Logger} printing messages to the android logs (using {@link Log}'s
 * methods).
 */
public class AndroidLogger implements Logger {

	private static final String DEFAULT_TAG = "DATABASE";
	private static final int[] ORDERED_LOG_LEVELS = {
			Log.ASSERT, Log.VERBOSE, Log.DEBUG, Log.INFO, Log.WARN, Log.ERROR
	};

	private final String tag;
	private final SparseBooleanArray logLevelStates = new SparseBooleanArray(ORDERED_LOG_LEVELS.length);

	/**
	 * Creates new instance.
	 * 
	 * @param minLogLevel
	 *            one of {@link Log#ASSERT}, {@link Log#DEBUG},
	 *            {@link Log#ERROR}, {@link Log#INFO}, {@link Log#VERBOSE},
	 *            {@link Log#WARN}
	 */
	public AndroidLogger(int minLogLevel) {
		this(DEFAULT_TAG, minLogLevel);
	}

	/**
	 * Creates new instance.
	 * 
	 * @param tag
	 *            tag for log messages
	 * @param minLogLevel
	 *            one of {@link Log#ASSERT}, {@link Log#DEBUG},
	 *            {@link Log#ERROR}, {@link Log#INFO}, {@link Log#VERBOSE},
	 *            {@link Log#WARN}
	 */
	public AndroidLogger(String tag, int minLogLevel) {
		this.tag = tag;
		initLogLevelStates(minLogLevel);
	}

	private void initLogLevelStates(int minLogLevel) {
		boolean logLevelAllowed = false;
		for (int logLevel : ORDERED_LOG_LEVELS) {
			if (logLevel == minLogLevel) {
				logLevelAllowed = true;
			}
			logLevelStates.put(logLevel, logLevelAllowed);
		}
	}

	private boolean isLogLevelAllowed(int logLevel) {
		return logLevelStates.get(logLevel);
	}

	@Override
	public void d(String message) {
		if (isLogLevelAllowed(Log.DEBUG)) {
			Log.d(tag, message);
		}
	}

	@Override
	public void i(String message) {
		if (isLogLevelAllowed(Log.INFO)) {
			Log.i(tag, message);
		}
	}

	@Override
	public void w(String message) {
		if (isLogLevelAllowed(Log.WARN)) {
			Log.w(tag, message);
		}
	}

	@Override
	public void e(String message) {
		if (isLogLevelAllowed(Log.ERROR)) {
			Log.e(tag, message);
		}
	}

	@Override
	public void e(String message, Throwable e) {
		if (isLogLevelAllowed(Log.ERROR)) {
			Log.e(tag, message, e);
		}
	}

	@Override
	public void logException(Throwable e) {
		if (isLogLevelAllowed(Log.ERROR)) {
			Log.e(tag, "exception in the database framework", e);
		}
	}

}
