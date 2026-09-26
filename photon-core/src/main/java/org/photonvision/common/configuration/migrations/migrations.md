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

## Migration process diagram
Migrations are executed by calling the `MigrationManager.run()` method, which follows the process outlined in the flowchart below.

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
