# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`plugin-moments` is a Halo 2.0 CMS plugin that provides lightweight microblogging ("moments") functionality supporting text, images, video, and audio. It consists of a Java/Spring WebFlux backend and a Vue 3 TypeScript frontend.

**Prerequisites:** Java 21, Node 24, pnpm 10+, Docker (for dev server).

## Common Commands

### Build
```bash
./gradlew build
```
Compiles both frontend (Vue/Rsbuild) and backend (Java). The frontend is built first and output to `src/main/resources/console/`.

### Development Server
```bash
./gradlew haloServer
```
Starts a Halo Docker container with the plugin auto-loaded. Requires Docker. This is the recommended dev workflow.

### Install Frontend Dependencies
```bash
./gradlew pnpmInstall
```

### Tests
```bash
./gradlew test
```

### Regenerate API Client
After changing backend Endpoints, DTOs, or Extension fields:
```bash
./gradlew generateApiClient
```
Generated files go to `console/src/api/generated/` — do not hand-edit.

### Frontend Lint / Format / Type Check
```bash
cd console
pnpm lint        # ESLint with auto-fix
pnpm prettier    # Prettier formatting
pnpm type-check  # Vue TypeScript check
```

### Frontend Dev Build (watch mode)
```bash
cd console
pnpm dev
```

## Architecture

### Backend (Java / Spring WebFlux)

The backend follows the **Halo 2.0 Plugin API** and uses **reactive programming** (Project Reactor `Mono`/`Flux`) throughout.

**Core Domain Model:** `Moment` extends `AbstractExtension` and is annotated with `@GVK(group = "moment.halo.run", version = "v1alpha1", kind = "Moment")`. Extensions are stored and managed by Halo's extension system. The `Moment` spec contains `content` (raw/html/medium), `releaseTime`, `visible` (PUBLIC|PRIVATE), `owner`, `tags`, and `approved`.

**Lifecycle:** `MomentReconciler` handles the reconciler pattern — adding finalizers, setting `observedVersion`, auto-approving legacy data, and creating comment subscriptions. It is registered with `workerCount(5)` and an `onAddMatcher` for startup sync.

**Plugin Entry:** `MomentsPlugin` extends `BasePlugin`. Its `start()` method registers the `Moment` scheme with database indexes on `spec.tags`, `spec.owner`, `spec.releaseTime`, `spec.visible`, and `spec.approved`.

**API Layers:** There are three distinct API surfaces, each implemented as a `CustomEndpoint` using `SpringdocRouteBuilder`:
- **Console API** (`MomentEndpoint`): admin operations at `console.api.moment.halo.run/v1alpha1`.
- **User Center API** (`UcMomentEndpoint`): end-user self-service at `uc.api.moment.halo.run/v1alpha1`. Creating/updating moments from UC sets `approved=false` (pending review) unless the user has the approval role.
- **Public API** (`MomentQueryEndpoint`, `MomentRouter`): theme-facing and public queries at `api.moment.halo.run/v1alpha1`, plus theme template routes `/moments` and `/moments/{name}`.

**Finder API** (`MomentFinder`): provides Thymeleaf template variables for themes (`momentFinder.listAll()`, `momentFinder.list(page, size)`, etc.).

**Integrations:**
- **RSS**: `MomentRssProvider` implements `RssRouteItem` from the feed plugin (optional dependency `PluginFeed >= 1.4.0`).
- **Search**: `MomentHaloDocumentsProvider` indexes moments into Halo's search system with type `moment.moment.halo.run`.
- **Comments**: `CommentNotificationReasonPublisher` and the reconciler integrate with Halo's comment/notification system.

**Permissions** are defined in `src/main/resources/extensions/roleTemplate.yaml` with roles for view, manage, publish, approve, and delete.

### Frontend (Vue 3 / TypeScript)

The frontend lives in `console/` and is bundled with **Rsbuild** using `@halo-dev/ui-plugin-bundler-kit`.

**Plugin Registration:** The entry point (`src/index.ts`) uses `definePlugin()` from `@halo-dev/ui-shared` to register routes (admin Console and User Center) and extension points (e.g., `comment:subject-ref:create`).

**API Client:** TypeScript API types and clients are auto-generated from the backend OpenAPI spec. Run `./gradlew generateApiClient` to regenerate after backend changes. Generated code lives in `console/src/api/generated/` — do not hand-edit.

**State & Data:** Uses **Pinia** for local state and **Vue Query** (`@tanstack/vue-query`) for server state.

**Styling:** **UnoCSS** (`uno.config.ts`) with `presetWind3` and `transformerCompileClass`. Icons come from `unplugin-icons` with Iconify sets (Mingcute, Lucide, Tabler, etc.). SCSS is supported via `@rsbuild/plugin-sass`.

**Path Alias:** `@/` maps to `console/src/`.

## Code Style

The project uses `.editorconfig` extensively:
- Java: 4-space indent, 100-character line length, single imports (no star imports).
- Frontend (JS/TS/Vue/CSS/SCSS): 2-space indent, 100-character line length, LF line endings, trailing whitespace trimmed.
- JSON/YAML: 2-space indent.
- Gradle/Groovy: 4-space indent.

Prettier config is embedded in `console/package.json` (printWidth 100, singleQuote false, trailingComma es5, arrowParens always).

## OpenAPI / API Docs

The Gradle build generates grouped OpenAPI specs into `api-docs/openapi/v3_0/` and TypeScript clients into `console/src/api/generated/`. The grouping rules cover:
- `/apis/moment.halo.run/v1alpha1/**` (public)
- `/apis/console.api.moment.halo.run/v1alpha1/**` (console)
- `/apis/uc.api.moment.halo.run/v1alpha1/**` (user center)
