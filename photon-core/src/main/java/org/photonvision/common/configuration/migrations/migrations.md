# Migration Process for PhotonVision SQLite database


## Migration process diagram
``` mermaid
---
config:
    layout:dagre
---
flowchart TB
    start((Start))
    q1{"DB Exists?"}
    q2{"Conf DB Exists?"}
    copy["Copy Conf DB"]
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
