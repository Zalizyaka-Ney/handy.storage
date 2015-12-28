package handy.storage.update;

import java.util.Collections;
import java.util.List;

/**
 * Produces some most common {@link OnDatabaseUpdatePolicy} instances.
 */
public final class OnDatabaseUpdatePolicyFactory {

	private OnDatabaseUpdatePolicyFactory() {}

	/**
	 * Creates an {@link OnDatabaseUpdatePolicy} instance which creates all
	 * tables that are registered but don't exist at the moment of update (no
	 * records will be deleted from the database).
	 */
	public static OnDatabaseUpdatePolicy createNewTablesPolicy() {
		return new OnDatabaseUpdatePolicy() {

			@Override
			public List<OnDatabaseUpdateAction> getOnUpdateActions(int oldVersion, int newVersion) {
				return Collections.singletonList(OnDatabaseUpdateActionFactory.createNewTablesAction());
			}
		};
	}

	/**
	 * Creates an {@link OnDatabaseUpdatePolicy} instance which deletes all
	 * tables and than creates all registered tables on database update (all
	 * records in the database will be cleared on a update).
	 */
	public static OnDatabaseUpdatePolicy recreateTablesPolicy() {
		return new OnDatabaseUpdatePolicy() {

			@Override
			public List<OnDatabaseUpdateAction> getOnUpdateActions(int oldVersion, int newVersion) {
				return Collections.singletonList(OnDatabaseUpdateActionFactory.recreateTablesAction());
			}
		};
	}

	/**
	 * Creates an {@link OnDatabaseUpdatePolicy} instance which doesn't do
	 * anything on all database updates.
	 */
	public static OnDatabaseUpdatePolicy emptyPolicy() {
		return new OnDatabaseUpdatePolicy() {

			@Override
			public List<OnDatabaseUpdateAction> getOnUpdateActions(int oldVersion, int newVersion) {
				return Collections.singletonList(OnDatabaseUpdateActionFactory.emptyAction());
			}
		};
	}

}
