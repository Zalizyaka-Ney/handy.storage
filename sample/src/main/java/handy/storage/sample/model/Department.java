package handy.storage.sample.model;

import handy.storage.annotation.Column;
import handy.storage.annotation.Reference;
import handy.storage.annotation.TableName;
import handy.storage.api.CursorValues;
import handy.storage.api.ObjectCreator;
import handy.storage.api.UniqueObject;

@TableName(Department.TABLE_NAME)
public class Department extends UniqueObject {

	public static final String TABLE_NAME = "department";
	public static final String NAME = "name";
	public static final String BUILDING = "building";

	@Column(BUILDING)
	@Reference
	private Building building;

	@Column(NAME)
	private String name;

	@SuppressWarnings("unused")
	private Department() {
	}

	public Department(Building building, String name) {
		this.building = building;
		this.name = name;
	}

	@Override
	public String toString() {
		return "Department [building=" + building + ", name=" + name + "]";
	}

	public Building getBuilding() {
		return building;
	}

	public void setBuilding(Building building) {
		this.building = building;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public static class Creator implements ObjectCreator<Department> {

		@Override
		public Department createObject(CursorValues values) {
			Department department = new Department((Building) values.getValue(BUILDING), (String) values.getValue(NAME));
			Long id = values.getValue(ID);
			department.setId(id);
			return department;
		}

	}

}
