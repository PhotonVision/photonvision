# Migration Process for PhotonVision SQLite database

The migration process maintains backward compatibility with older database versions by defining the process necesary to convert the database structure and JSON stored in the database into the format expected by the current version of PhotonVision.

## Good practices for migrations
* The first step of any migration must be the SQL statement(s) that create the table schema for the oldest supported database version.
* Each subsequent step can assume that the database structure is the one provided by the previous step.
* Add each new step to the end of the existing set of steps, never insert a step betwen steps.
* Database version numbers must be unique 32-bit integers. Do not reuse version numbers of un-supported databases.
* The numbering scheme for all new migrations is YYVV, where YY is the two-digit year, and VV is a monotonically-increasing version number that starts at 00 for each new year and increase by 1 for each new migration step added that year.
* Migration steps should be self-contained and leave the database in a valid state.
* You don't need to remove older steps ever and it is less risky to leave them alone.
* Once a migration has been deployed, don't edit or reorder it in any way. Doing so can lead to conflicts with databases that were already migrated.
* If you need to fix a deployed migration, add a new migration step with the fixes.
* Do not rely on POJOs for migrating the JSON object representations stored in the database. A future edit that changes the POJO could invalidate the migration step. Instead, directly edit the JSON structure using the tools provided.
* Don't add COMMIT statements or anything else that would cause commits (like setting autocommit) inside a step. The migration system takes care of this.

## Adding a new migration step
The migration process is configured in `DBMigration.java`. The basic process is to create either a SQL string or a `MigrationFunction` that will carry out the migration and then add it to the end of the current migration steps defined in `DBMigration.getMigration()`. As stated above, **do not change the order or content of existing migrations**.

`MigrationManager` takes care of applying the migration steps one at a time and atomically. This prevents failure during a step from leaving the database in a partially-migrated (and possibly unrecoverable) state. Because of this, do not add anything that will trigger a commit during a migration step.

### SQL migrations
For migrations that can be done using SQL, create a new `String` in `DBMigrations` with one or more SQLite statements. Every statement should end with a semicolon (`;`).

### `MigrationFunction` migrations
For more complex migrations, for example ones that require manipulating the JSON structure, create a method that follows the `MigrationFunction` interface signature. The method takes a single `Connection` parameter, which is the connection to the database being migrated, returns nothing, and throws `MigrationException` on failure. This method should use the `Connection` object to create SQL statements that manipulate the contents of the database.

NOTE: SQLite databases cannot read and write simultaneously with a single connection. If you are reading information and then writing it back to the database, make sure that you read all of the data first and close the `ResultSet` returned by the query before creating the `Statement` that writes the data back to the database.

## Migration execution
Migrations are executed by calling `MigrationManager.run()`, which follows the process outlined in the flowchart below. Migration steps are applied in the exact order that they are added when setting up the `MigrationManager` object. If it encounters an error during the migration process it will rollback the transaction that caused the error, make a backup copy of the database, and then create an empty database so that PhotonVision can start.

``` mermaid
flowchart TB
    start((Start))
    q1{"Photon DB Exists?"}
    q2{"Config DB Exists?"}
    copy["Copy Config DB"]
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
