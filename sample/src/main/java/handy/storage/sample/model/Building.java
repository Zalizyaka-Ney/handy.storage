package handy.storage.sample.model;

import handy.storage.annotation.Column;
import handy.storage.annotation.TableName;
import handy.storage.api.ImplicitlyUsed;
import handy.storage.api.UniqueObject;

@TableName(Building.TABLE_NAME)
public class Building extends UniqueObject {

	public static final String TABLE_NAME = "building";
	public static final String NAME = "name";

	@Column(NAME)
	private String name;

	@ImplicitlyUsed
	public Building() {
	}

	public Building(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Building [name=" + name + "]";
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
