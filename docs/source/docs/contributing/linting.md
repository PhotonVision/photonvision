# Linting the PhotonVision Codebase

## Versions

:::{note}
If you work on other projects that use different versions of the same linters as PhotonVision, you may find it beneficial to use a [venv](https://docs.python.org/3/library/venv.html) instead of installing the linters globally. This will allow you to have different versions of the same linter installed for different projects.
:::

The correct versions for each linter can be found under the linting workflow located [here](https://github.com/PhotonVision/photonvision/tree/main/.github/workflows). For *doc8*, the version can be found in `docs/requirements.txt`. If you've linted, and are still unable to pass CI, please check the versions of your linters.

## Frontend

### Linting the frontend

In order to lint the frontend, run `pnpm -C photon-client lint && pnpm -C photon-client format`. This should be done from the base level of the repo.

## Backend

### wpiformat installation

To lint the backend, PhotonVision uses *wpiformat* and *spotless*. Spotless is included with gradle, which means installation is not needed. To install wpiformat, run `pipx install wpiformat`. To install a specific version, run `pipx install wpiformat==<version>`.

### Linting the backend

To lint, run `./gradlew spotlessApply` and `wpiformat`.

## Documentation

### doc8 installation

To install *doc8*, the python tool we use to lint our documentation, run `pipx install doc8`. To install a specific version, run `pipx install doc8==<version>`.

### Linting the documentation

To lint the documentation, run `doc8 docs` from the root level of the docs.

## Alias

The following [alias](https://www.computerworld.com/article/1373210/how-to-use-aliases-in-linux-shell-commands.html) can be added to your shell config, which will allow you to lint the entirety of the PhotonVision project by running `pvLint`. The alias will work on Linux, macOS, Git Bash on Windows, and WSL.

```sh
alias pvLint='wpiformat -v && ./gradlew spotlessApply && pnpm -C photon-client lint && pnpm -C photon-client format && doc8 docs'
```
