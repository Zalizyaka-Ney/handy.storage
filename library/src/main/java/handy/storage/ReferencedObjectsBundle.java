package handy.storage;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds values of referenced models.
 */
class ReferencedObjectsBundle {

	/**
	 * Inner key for the map.
	 */
	private static class Key {
		private final Class<?> modelClass;
		private final Object primaryKeyValue;

		Key(Class<?> modelClass, Object primaryKeyValue) {
			this.modelClass = modelClass;
			this.primaryKeyValue = primaryKeyValue;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + modelClass.hashCode();
			result = prime * result + ((primaryKeyValue == null) ? 0 : primaryKeyValue.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			ReferencedObjectsBundle.Key other = (ReferencedObjectsBundle.Key) obj;
			if (modelClass == null) {
				if (other.modelClass != null) {
					return false;
				}
			} else if (!modelClass.equals(other.modelClass)) {
				return false;
			}
			if (primaryKeyValue == null) {
				if (other.primaryKeyValue != null) {
					return false;
				}
			} else if (!primaryKeyValue.equals(other.primaryKeyValue)) {
				return false;
			}
			return true;
		}

	}

	private Map<ReferencedObjectsBundle.Key, Object> bundle = new HashMap<>();

	void put(Class<?> modelClass, Object primaryKeyValue, Object model) {
		bundle.put(new Key(modelClass, primaryKeyValue), model);
	}

	Object get(Class<?> modelClass, Object primaryKeyValue) {
		return bundle.get(new Key(modelClass, primaryKeyValue));
	}

}