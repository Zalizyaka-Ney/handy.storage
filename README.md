Handy Storage is a framework simplifying work with SQLite databases for Android developers. It's based on the java reflection API.

### Import. ###

Add to your build.gradle the following:
```
dependencies {
    compile 'com.github.zalizyaka-ney:handy.storage:1.0.0'
}
```

### Base usage. ###

First, you need to declare models for data to be stored in a database. Every model class must have a constructor without parameters. For example:


```
@TableName(Person.TABLE_NAME)
public class Person implements Model {

	public static final String TABLE_NAME = "person";
	public static final String NAME = "name";
	public static final String SURNAME = "surname";
	public static final String AGE = "age";

	@Column(NAME)
	private String name;

	@Column(SURNAME)
	private String surname;

	@Column(AGE)
	private int age;

	// for the framework
	private Person() {
	}

	public Person(String name, String surname, int age) {
		this.name = name;
		this.surname = surname;
		this.age = age;
	}
}

```


Then create a Database object and register all model classes:


```
Database database = HandyStorage.defaultInstance().newDatabase(context, DATABASE_NAME, DATABASE_VERSION)
	.addTable(Person.class)
	// register more tables
	.build();
```
There can be any number of **HandyStorage** and **Database** objects simultaneously. Hovewer, there can not be two **Database** instances working with the same SQLite database simultaneously - if you want to create another **Database** object for work with the database, you must call **close()** on the previous one first.
	
The code above don't do any heavy operation and can be called in onCreate() of your Application class. The database initialization is postponed until you call **getTable()** method.

```
WritableTable<Person> table = database.getTable(Person.class);
```


Having a table object you can do your database routine.


```
// Store objects:
table.insert(new Person("Nick", "Highman", 19));
table.insert(collectionOfPersons);

// transactions
database.performTransaction(new Transaction() {
	@Override
	public void performQueries(Database database) throws OperationException {
		table.insert(new Person("Ann", "Madison", 20));
		table.insert(new Person("John", "Doe", 18));
	}
});


// Read data from the database:
List<Person> persons = table.selectAll();
List<Person> teenagers = table.select().where(Person.AGE).between(12, 19).execute();

// Delete data:
table.delete().where(Person.AGE).lessThan(19).execute();

//Update data:
table.update().where(Person.NAME).equalsTo("Ann").and(Person.SURNAME).equalsTo("Madison").setValue(Person.AGE, 21).execute();
```
See more code samples [here](https://github.com/Zalizyaka-Ney/handy.storage/wiki/More-code-samples).

Note that all operations with database can be executed only in a background thread, an attempt to use the framework in UI thread will lead to a runtime exception.

### Declaration of models for database tables. ###
All data model classes must implement the **Model** interface. All fields in those classes annotated with **Column** annotation will be declared as columns in the database table. Fields declared in superclasses will be also included there (this can be turned off). Use annotations **AutoIncrement, ForeignKey, NotNull, PrimaryKey, Unique, CompositePrimaryKey, CompositeUnique** to declare column and table constrains. Names of columns and tables can be set via annotations **Column** and **TableName** correspondently.

You also can use a **Reference** annotation to automatize the work with foreign keys (see [the corresponding wiki page](https://github.com/Zalizyaka-Ney/handy.storage/wiki/References-to-anothe-tables)).

If your data model contains a rowid (i.e. *INTEGER PRIMARY KEY AUTOINCREMENT*) column - consider extending **UniqueObject** class, it already has such a field declared.

### Data types. ###

The framework out of box can work with fields of such types: **byte, short, int, long, float, double, boolean (and their object equivalents), byte[], String, java.util.Date, java.util.Calendar, android.net.Uri** and **enums**. The framework also supports a serialization of objects of other types into strings via **gson** library - to use it, you need to add a **@GsonSeriazable** annotation to a column declaration. 
If you want to work with another type - just create a [custom type adapter](https://github.com/Zalizyaka-Ney/handy.storage/wiki/Custom-type-adapters) for it.

### Wiki pages. ###

[More code samples](https://github.com/Zalizyaka-Ney/handy.storage/wiki/More-code-samples)

[Settings](https://github.com/Zalizyaka-Ney/handy.storage/wiki/Settings)

[Custom type adapters](https://github.com/Zalizyaka-Ney/handy.storage/wiki/Custom-type-adapters)

[References to another tables](https://github.com/Zalizyaka-Ney/handy.storage/wiki/References-to-anothe-tables)

[Building new table objects (including joint tables)](https://github.com/Zalizyaka-Ney/handy.storage/wiki/Building-new-table-objects-%28including-joint-tables%29)

[Using Handy Storage with content providers](https://github.com/Zalizyaka-Ney/handy.storage/wiki/Using-Handy-Storage-with-content-providers)

[Database updates](https://github.com/Zalizyaka-Ney/handy.storage/wiki/Database-updates)

[What can I do if the framework doesn't have a functionality that I need?](https://github.com/Zalizyaka-Ney/handy.storage/wiki/What-can-I-do-if-the-framework-doesn't-have-a-functionality-that-I-need%3F)

[Proguard rules](https://github.com/Zalizyaka-Ney/handy.storage/wiki/Proguard-rules)
