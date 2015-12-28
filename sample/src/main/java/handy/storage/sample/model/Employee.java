package handy.storage.sample.model;

import java.util.Date;

import handy.storage.annotation.Column;
import handy.storage.annotation.Reference;
import handy.storage.annotation.TableName;
import handy.storage.api.UniqueObject;

@TableName(Employee.TABLE_NAME)
public class Employee extends UniqueObject {

	public static final String TABLE_NAME = "employee";
	public static final String DEPARTMENT = "department";
	public static final String NAME = "name";
	public static final String GENDER = "gender";
	public static final String BIRTH_DATE = "birth_date";
	public static final String HIRE_DATE = "hire_date";

	@Column(DEPARTMENT)
	@Reference
	private Department department;

	@Column(NAME)
	private String name;

	@Column(GENDER)
	private Gender gender;

	@Column(BIRTH_DATE)
	private Date birthDate;

	@Column(HIRE_DATE)
	private Date hireDate;

	public Employee() {
	}

	public Employee(Department department, String name, Gender gender, Date birthDate, Date hireDate) {
		this.department = department;
		this.name = name;
		this.gender = gender;
		this.birthDate = birthDate;
		this.hireDate = hireDate;
	}

	@Override
	public String toString() {
		return "Employee ["
			+ "department=" + department + ", "
			+ "name=" + name + ", "
			+ "gender=" + gender + ", "
			+ "birthDate=" + birthDate + ", "
			+ "hireDate=" + hireDate
			+ "]";
	}

	public Department getDepartment() {
		return department;
	}

	public void setDepartment(Department department) {
		this.department = department;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public Date getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(Date birthDate) {
		this.birthDate = birthDate;
	}

	public Date getHireDate() {
		return hireDate;
	}

	public void setHireDate(Date hireDate) {
		this.hireDate = hireDate;
	}

	@Override
	public int hashCode() {
		return (int) getId();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Employee other = (Employee) obj;
		return other.getId() == getId();
	}

}
