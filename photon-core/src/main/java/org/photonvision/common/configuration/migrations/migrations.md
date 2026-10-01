# Migration Process for the PhotonVision SQLite Database

The migration process maintains backward compatibility with older database versions by defining the steps necessary to convert the database structure and the JSON stored in the database into the format expected by the current version of PhotonVision.

## Good practices for migrations
* The first step of every migration must be the SQL statement(s) that create the table schema for the oldest supported database version. These statement(s) are provided in the `MigrationManager` constructor.
* Each subsequent step can assume that the database structure is the one provided by the previous step.
* Add each new step to the end of the existing set of steps. Never insert a step between existing steps.
* Database version numbers must be unique 32-bit integers. Do not reuse version numbers from unsupported databases.
* The numbering scheme for all new migrations is YYYYPP, where YYYY is the four-digit year, and PP is a monotonically-increasing patch number that starts at 00 for each new year and increases by 1 for each migration step added that year.
* Migration steps should be self-contained and leave the database in a valid state.
* You do not need to remove older steps. It is less risky to leave them alone.
* Once a migration has been deployed, don't edit or reorder it in any way. Doing so can lead to conflicts with databases that were already migrated.
* If you need to fix a deployed migration, add a new migration step with the fixes.
* Do not rely on POJOs for migrating the JSON object representations stored in the database. A future edit that changes the POJO could invalidate the migration step. Instead, directly edit the JSON structure using the tools provided.
* Do not add `COMMIT` statements or anything else that would cause a commit, such as enabling autocommit, inside a step. `MigrationManager` disables autocommit, commits a successful step, and rolls back a failed step.

## Adding a new migration step
The migration process is configured in `DbMigration.java`. The basic process is to create either a SQL string or a `MigrationFunction` that will carry out the migration and then add it to the end of the current migration steps defined in `DbMigration.getMigration()`. As stated above, **do not change the order or content of existing migrations**.

`MigrationManager` applies migration steps one at a time in transactions. This prevents a failure during a step from leaving the database in a partially migrated (and possibly unrecoverable) state. Because `MigrationManager` controls the transaction, do not add anything that triggers a commit during a migration step.

The `MigrationManager` constructor requires a version number and a `String` containing the SQL statement(s) that create the initial table schema. The version number and schema should describe the oldest version that is still supported. This allows `MigrationManager` to create an empty database when no database exists. Any additional steps then migrate it to the target version.

### SQL migrations
For migrations that can be done using SQL, create a new `String` in `DbMigration` with one or more SQLite statements. Every statement should end with a semicolon (`;`).

### `MigrationFunction` migrations
For more complex migrations, such as those that manipulate the JSON structure, create a method that follows the `MigrationFunction` interface signature. The method accepts a single `Connection` parameter for the database being migrated, returns nothing, and throws `MigrationException` on failure. Use the `Connection` object to create SQL statements that manipulate the database contents. The easiest way to create one is as a lambda:

```java
private static MigrationFunction migrationYYYYPP = (conn) -> {
    // migration code
};
```

NOTE: In the body of the `MigrationFunction`, do not read from and write to the database simultaneously through the same connection. If you read information and write it back, first read all of the data and close the query's `ResultSet` and `Statement` before creating the `Statement` that writes the data back.

## Migration execution
Migrations are executed by calling `MigrationManager.run(String url)` with a SQLite JDBC URL, such as `jdbc:sqlite:/path/to/db`. The method follows the process outlined in the flowchart below. Migration steps are applied in the exact order in which they are added to the `MigrationManager`. If migration fails, the failed step's transaction is rolled back. `MigrationManager` then backs up the failed database. If the backup cannot be created, it deletes the failed database before creating a new empty database so that PhotonVision can start.

``` mermaid
flowchart TB
    start((Start))
    q1{"Photon DB Exists?"}
    q2{"Default DB Exists?"}
    copy["Copy Default DB"]
    migrate["Migrate DB"]
    success{"Success?"}
    backup["Backup DB"]
    delete["Delete DB"]
    new["Create New DB"]
    stop(((End)))

    start-->q1
    q1-- yes -->migrate
    q1--no -->q2
    q2-- yes --> copy
    copy-->migrate
    migrate-->success
    success--yes-->stop
    success--no -->backup
    backup-->delete
    delete-->new
    q2--no -->new
    new-->stop
```
