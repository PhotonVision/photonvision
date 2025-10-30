# Linting the PhotonVision Codebase

## Frontend

### Linting

In order to lint the frontend, run `pnpm -C photon-client lint && pnpm -C photon-client format`. This should be done from the base level of the repo.

## Backend

### Installation

To lint the backend, PhotonVision uses `wpiformat` and `spotless`. Spotless is included with gradle, which means installation is not needed. To install wpiformat, run `pipx install wpiformat`.

### Linting

To lint, run `./gradlew spotlessApply` and `wpiformat`.

## Documentation

### Installation

To install `doc8`, the python tool we use to lint our documentation, run `pipx install doc8`.

### Linting

To lint the documentation, run `doc8 docs` from the root level of the docs.

## Alias

The following [alias](https://www.computerworld.com/article/1373210/how-to-use-aliases-in-linux-shell-commands.html) can be added to your shell config, which will allow you to lint the entirety of the PhotonVision project by running `pvLint`. The alias will work on Linux, macOS, Git Bash on Windows, and WSL.

```
alias pvLint='wpiformat -v && ./gradlew spotlessApply && pnpm -C photon-client lint && pnpm -C photon-client format && doc8 docs'
```
