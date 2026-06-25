# 1. Spring profiles selected via external activation

Date: 2026-06-26

## Status

Accepted

## Context

Synapse must run in three environments — development, staging, and
production — that differ in logging verbosity, SQL echo, and whether API
documentation is exposed. The repository uses three permanent branches
(`develop`, `staging`, `main`) promoted strictly through pull requests, with
required linear history and squash merges.

The intuitive approach is to set the active Spring profile in `application.yml`
and give each branch a different value: `dev` on `develop`, `stage` on
`staging`, `prod` on `main`.

This conflicts with the branching model. If `spring.profiles.active` differs
per branch, that line diverges permanently. Every `develop → staging → main`
promotion would carry a conflicting change on that line, and with required
linear history the value cannot flow cleanly downstream. The promotion model
assumes the same code and the same artifact move through environments
unchanged; a per-branch profile value breaks that assumption.

We also build a single container image intended to be promoted through all
environments. Baking an environment identity into the image would force
separate builds per environment.

## Decision

The active profile is never set in committed source. `application.yml` contains
no `spring.profiles.active`. The profile is supplied externally through the
`SPRING_PROFILES_ACTIVE` environment variable at deployment time.

Configuration is layered:

- `application.yml` is the shared, complete, bootable base.
- `application-dev.yml`, `application-stage.yml`, `application-prod.yml`, and
  `application-test.yml` contain only overrides.

All profile files are identical across every branch. The same immutable image
is promoted through all environments; only the injected environment variables
differ. Environment-to-profile mapping is expressed in deployment
configuration (Compose locally, Helm `values-{env}.yaml` when deployed), not in
the application source.

## Consequences

### Positive

- Promotion pull requests carry no profile-related diff, so there is nothing to
  conflict on. Linear history is preserved.
- One immutable image is built once and promoted, matching the release pipeline.
- The base config alone is bootable, so no profile is required for a context to
  start (relevant for future `@SpringBootTest`).
- Environment configuration is auditable: profile selection and secret sources
  live in deployment config, not scattered across branches.

### Negative

- Profile selection now depends on the deployment environment being configured
  correctly. A missing or wrong `SPRING_PROFILES_ACTIVE` is a deployment-time
  fault rather than a compile-time one.
- Contributors must understand the override-layering model; the meaning of a
  given runtime config is the base plus the active overlay, not a single file.

### Mitigations

- A local default of `dev` is provided via `.env.example` and `compose.yaml`.
- The model and environment mapping are documented in `CONTRIBUTING.md`
  (Configuration and Environment Profiles).