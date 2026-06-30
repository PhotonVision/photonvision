# AGENTS.md

## Repo overview

Monorepo with three package systems:
- **Gradle** multi-project (Java 25, C++) -- root `./gradlew`
- **pnpm** workspaces -- `photon-client/` (Vue 3 + Vuetify 3 SPA) and `website/` (Vite SSG marketing site)
- **Python** -- `photon-lib/py/` (photonlibpy wheel, pytest + mypy)

Key Gradle subprojects (defined in `settings.gradle`):
- `photon-server` -- fat JAR entrypoint (`org.photonvision.Main.main()`, port **5800**)
- `photon-core` -- vision pipelines, hardware manager, config
- `photon-targeting` -- native C++ AprilTag + JNI
- `photon-lib` -- robot-side vendor library (Java, C++, Python)
- `photon-docs` -- JavaDoc + Doxygen

## Commands (run from repo root)

| What | Command |
|------|---------|
| Full app (backend + frontend) | `./gradlew run` |
| Build all Java/C++ | `./gradlew build` |
| Run Java tests | `./gradlew test` |
| Java format | `./gradlew spotlessApply` |
| Cross-compile toolchain | `./gradlew installArm64Toolchain` |
| Deploy to coprocessor | `./gradlew deploy -PArchOverride=linuxarm64` |
| Frontend dev (hot reload) | `pnpm dev` (in `photon-client/`) |
| Frontend type-check | `pnpm type-check` (in `photon-client/`) |
| Frontend lint/format | `pnpm lint` / `pnpm format` (in `photon-client/`) |
| E2E tests (Playwright) | `pnpm test` (auto-starts `./gradlew run`) |
| Python tests | `pytest` (in `photon-lib/py/`) |

Use `pnpm` not `npm` for JS packages.

## Frontend conventions

- **Formatter**: Prettier — `semi: true`, `singleQuote: false`, `tabWidth: 2`, `printWidth: 120`, `trailingComma: "none"`
- **Linter**: ESLint 9 flat config — enforces same style as Prettier
- **Router**: hash-based (`/#/dashboard`, `/#/cameras`, `/#/settings`, `/#/cameraConfigs`, `/#/docs`)
- **API base**: `http://{host}:5800/api` (Axios baseURL set automatically in `src/main.ts`)
- **Path alias**: `@` → `src/`
- **State**: Pinia stores in `src/stores/`, key store is `StateStore`
- **WebSocket**: `ws://{host}:5800/websocket_data` for real-time data

## Ports

- App UI + API: **5800**
- Vite dev server (photon-client): default Vite port (usually 5173)

## Build artifacts

- `photon-server/build/libs/photonvision.jar` (fat JAR via Shadow plugin)
- `photon-client/dist/` (built SPA, served by Gradle during `./gradlew run`)
- `photon-lib/py/dist/` (Python wheel)

## Testing quirks

- Playwright tests expect `./gradlew run` on port 5800 (auto-configured in `playwright.config.ts`)
- Java tests use JUnit 5 (Jupiter) with JaCoCo coverage
- Python tests use pytest + mypy type checking
- CI runs typecheck-client (`vue-tsc --noEmit`) before Playwright

## CI workflows

- `build.yml` — main CI: typecheck, Playwright, Gradle build/test, cross-compile, IPK, disk images, smoketests
- `lint-format.yml` — wpiformat, Spotless, ESLint + Prettier
- `python.yml` — Python wheel build, pytest, mypy, PyPI publish
- `website.yml` — marketing site build + deploy
- `photon-api-docs.yml` — JavaDoc + Doxygen publish

## Repository quick reference

- Non-obvious dirs: `photon-serde/` (YAML→Java/C++/Python codegen), `photon-sc-app/` (IPK packaging), `test-resources/` (test images/configs)
- Version: WPILib 2027.0.0-alpha-6, JDK 25, Node 24, Python 3.14, Gradle 9.4.0
- License: GPL-3.0
