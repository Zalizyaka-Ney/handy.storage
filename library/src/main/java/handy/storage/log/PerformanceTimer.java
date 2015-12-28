package handy.storage.log;

import java.util.ArrayList;
import java.util.List;

/**
 * Is used to measure time spent on any actions.
 */
public final class PerformanceTimer {

	private static boolean enabled = false;

	static boolean isEnable() {
		return enabled;
	}

	static void setEnabled(boolean enabled) {
		PerformanceTimer.enabled = enabled;
	}

	private PerformanceTimer() {
	}

	private static final ThreadLocal<List<TimeInterval>> EVENTS = new ThreadLocal<List<TimeInterval>>() {
		@Override
		protected List<TimeInterval> initialValue() {
			return new ArrayList<>();
		}
	};
	
	/**
	 * Marks the start of time measuring interval for an event.
	 * @param event	caption of the event
	 */
	public static void startInterval(String event) {
		if (!enabled) {
			return;
		}
		DatabaseLog.d("started " + event);
		List<TimeInterval> threadEvents = EVENTS.get();
		threadEvents.add(new TimeInterval(event));
	}
	
	/**
	 * Ends last started time measuring interval. Can be called only after a call to
	 * {@link #startInterval(String)}.
	 */
	public static void endInterval(String details) {
		if (!enabled) {
			return;
		}
		long now = System.currentTimeMillis();
		List<TimeInterval> threadEvents = EVENTS.get();
		if (threadEvents.isEmpty()) {
			DatabaseLog.e("endInterval() called without startInterval()");
			return;
		}
		TimeInterval interval = threadEvents.remove(threadEvents.size() - 1);
		DatabaseLog.d("ended " + interval.event + ", " + (details == null ? "" : details + ", ") + (now - interval.timestamp) + " ms");
	}
	
	/**
	 * Ends last started time measuring interval. Can be called only after a
	 * call to {@link #startInterval(String)}.
	 */
	public static void endInterval() {
		endInterval(null);
	}

	/**
	 * Represents a time interval for some event.
	 */
	private static final class TimeInterval {

		final long timestamp = System.currentTimeMillis();
		final String event;

		TimeInterval(String caption) {
			this.event = caption;
		}
	}
}
