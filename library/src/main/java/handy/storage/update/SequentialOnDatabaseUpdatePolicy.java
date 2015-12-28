package handy.storage.update;

import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * <p>
 * Simple realization of {@link OnDatabaseUpdatePolicy}.
 * </p>
 * <p>
 * Register {@link OnDatabaseUpdateAction}'s you want to execute on update to a
 * certain database version via method
 * {@link #addOnUpdateAction(int, OnDatabaseUpdateAction)}.
 * Then on update from version <code>oldVersion</code> to
 * <code>newVersion</code> the method
 * {@link SequentialOnDatabaseUpdatePolicy#getOnUpdateActions(int, int)} will return the
 * list containing actions registered for versions <code>oldVersion + 1</code>,
 * <code>oldVersion + 2</code>, ... , <code>newVersion - 1</code>,
 * <code>newVersion</code>.
 * </p>
 */
public class SequentialOnDatabaseUpdatePolicy implements OnDatabaseUpdatePolicy {

	private SparseArray<OnDatabaseUpdateAction> actions = new SparseArray<>();

	/**
	 * Registers an action to execute on database update to a certain version.
	 * Replace a previous registered action for this version.
	 * 
	 * @param version
	 *            version of the database
	 * @param action
	 *            action to execute
	 */
	public void addOnUpdateAction(int version, OnDatabaseUpdateAction action) {
		actions.put(version, action);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<OnDatabaseUpdateAction> getOnUpdateActions(int oldVersion, int newVersion) {
		List<OnDatabaseUpdateAction> result = new ArrayList<>();
		TreeSet<Integer> versions = new TreeSet<>();
		for (int i = 0; i < actions.size(); i++) {
			versions.add(actions.keyAt(i));
		}
		for (int version : versions) {
			if (version > oldVersion && version <= newVersion) {
				result.add(actions.get(version));
			}
		}
		return result;
	}

}
