# Photonlib Developer Docs

Our maven server is located at https://maven.photonvision.org/#/. This server runs [Reposilite](https://hub.docker.com/r/dzikoysk/reposilite) in Docker, and uses Caddy for serving requests.


## Backing up using Rsync

The Clarkson Open Source Institute at Clarkson University provides a mirror of our artifacts available [online](https://mirror.clarkson.edu/photonvision). Learn more about them at [their homepage](https://mirror.clarkson.edu/home).

Artifacts from our Maven server can also be backed up locally to a folder called `photonlib-backup` using the following command, which excludes "snapshots" for space reasons:

```
rsync -avzrHy --no-perms --no-group --no-owner --ignore-errors --exclude ".~tmp~" --exclude "snapshots/org/photonvision/photontargeting*" \
--exclude "snapshots/org/photonvision/photonlib*" maven.photonvision.org::reposilite-data \
/path/to/photonlib-backup
```
