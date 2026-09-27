# Repository Guidelines

## Project Structure & Module Organization
- `src/` contains the Vue 3 + TypeScript frontend. Key areas:
  - `src/pages/` feature pages such as `barn/`, `site-selection/`, `site-new-build/`, `bi/`, and `system/`.
  - `src/components/` shared UI, including `Layout/` for the app shell.
  - `src/api/` request wrappers and feature APIs.
  - `src/router/` route definitions and menu entry points.
- `ruoyi-barn/` contains the Spring Boot backend. Controllers, services, mappers, and entities follow the `com/barn/...` package tree.
- `ruoyi-barn/src/main/resources/db/` stores SQL scripts for new tables and schema additions.
- `public/` holds static assets; `dist/` is build output and should not be edited manually.

## Build, Test, and Development Commands
- `pnpm dev` or `npm run dev`: start the frontend at `http://localhost:5000`.
- `pnpm build` or `npm run build`: type-check and build the frontend bundle.
- `pnpm check` or `npm run check`: run Vue/TypeScript checking only.
- `pnpm lint` or `npm run lint`: lint `.ts` and `.vue` files.
- Backend build: `mvn -f ruoyi-barn/pom.xml -DskipTests compile`.

## Coding Style & Naming Conventions
- Use 2-space indentation in Vue, TypeScript, JavaScript, and SQL files.
- Prefer descriptive, feature-based names: `SiteSelection.vue`, `SiteNewBuildController.java`, `site_new_build.sql`.
- Keep Vue SFCs organized as `<script setup>`, `<template>`, then `<style scoped>`.
- Use `camelCase` for JS/TS variables and `PascalCase` for Vue components.
- Use `snake_case` for SQL table and column names.

## Testing Guidelines
- There is no dedicated frontend test suite in this repository; rely on `pnpm check`, `pnpm lint`, and manual verification.
- For backend changes, prefer a compile check with Maven before handing off.
- When adding new flows, verify the corresponding page, API call, and database table together.

## Commit & Pull Request Guidelines
- Commit history uses concise Conventional Commit style, for example: `feat: 烤房全周期信息化管理平台完整实现`.
- Keep commit messages short, imperative, and scoped to one task.
- PRs should describe what changed, where it changed, and how it was verified.
- Include screenshots or short notes for UI changes, and mention any new SQL scripts or backend endpoints.

## Security & Configuration Tips
- Do not hardcode secrets or remote database credentials in new code.
- Treat `ruoyi-barn/src/main/resources/application.yml` and SQL scripts as configuration-sensitive files.
- Prefer adding new tables and endpoints over modifying existing production flows unless explicitly requested.
