# Contributing to Synapse

Synapse uses trunk-based development. Repository workflow rules keep changes focused and reviewable while the existing automated quality gates protect application behavior.

## Git workflow

`main` is the only permanent branch and the source for all development work.

Create a working branch with the `amg-<short-description>` convention:

```bash
git switch main
git pull --ff-only origin main
git switch -c amg-add-room-invitations
```

Examples include `amg-fix-call-timeout`, `amg-update-readme`, and `amg-video-message`. Renovate and other explicitly configured automation may use their own machine branch pattern, such as `chore/update-*`.

Keep each branch focused on one purpose. Avoid combining unrelated application, infrastructure, documentation, or dependency work.

### Synchronizing a working branch

Merge the latest trunk into the working branch without rewriting its history:

```bash
git fetch origin
git merge origin/main
git push
```

Do not use `git rebase origin/main` as the normal synchronization method. Do not force-push merely to synchronize a branch. If an exceptional rewrite of a personal branch is necessary, coordinate with anyone using the branch and use `--force-with-lease`.

### Commits

Use a short, direct, imperative subject:

```text
Add stale-cache fallback to resolver
Fix call recovery timeout
Update WebSocket documentation
Remove obsolete staging workflow
```

Do not use Conventional Commit prefixes or gitmoji for human commits. Avoid vague subjects such as `WIP`, `changes`, or `update stuff`.

Renovate and other configured automation may retain stable machine-generated commit formats required for their operation. Historical commits are not rewritten.

Every commit should represent a coherent change, avoid unrelated files, and leave the project in a reviewable state.

### Pull requests and merging

Open pull requests against `main`. Pull requests should explain the change, approach, material risks, and database impact where relevant. Draft pull requests are appropriate for incomplete work.

GitHub Actions is the authoritative automated verification record for the current commit. Do not paste routine terminal logs into the pull request description.

Pull requests are squash merged. GitHub merge commits and rebase merges are disabled. The squash commit title must follow the same short, imperative convention as human commits.

Small, focused changes may be pushed directly to `main`. Direct pushes must still keep `main` buildable and trigger the same main-push CI. Force pushes to `main` and deletion of `main` are prohibited.

Synapse does not require DCO sign-off, signed commits, a commit template, CODEOWNERS approval, or a fixed approval count merely to satisfy this workflow.

### Releases and tags

The application is currently unstable, so normal development does not create version tags, release branches, release trains, or automated changelogs. Historical tags remain part of repository history.

The release workflow is separate from a release branch: pushes to `main` may verify and publish the application using the existing deployment automation.

## Validation

Every pull request and push to `main` runs the repository CI, including workflow linting, Maven verification, Docker build validation, and Docker Compose configuration validation.

Useful local checks are:

```bash
./mvnw spotless:apply
./mvnw clean verify
docker compose --env-file .env.example config --quiet
git diff --check
git status --short
```

The optional repository pre-commit hook formats Java and runs Maven verification. Install it only when desired:

```bash
./mvnw -Pinstall-git-hooks initialize
```

Do not disable or delete tests merely to make a build pass. Use `@Disabled` only with a clear technical reason, a tracked issue, and a plan to restore the test.

### Test expectations

Behavioral changes should cover the primary path, important validation or authorization failures, domain invariants, and meaningful edge cases. A bug fix should include a regression test that fails before the fix and passes afterward.

Pay particular attention to authentication, authorization, token rotation and revocation, concurrency, transactions, external adapters, persistence mapping, locking, idempotency, and security filters.

## Architecture

Synapse follows Domain-Driven Design, hexagonal architecture, CQRS, bounded contexts, and dependency inversion. The intended dependency direction is:

```text
Infrastructure → Application → Domain
```

The Domain layer must not depend on Spring, JPA, Hibernate, HTTP, controllers, persistence implementations, or serialization frameworks. Do not place framework annotations such as `@Entity`, `@Service`, `@Component`, `@RestController`, or `@Repository` in the Domain layer.

A bounded context must not depend on another context's internal domain or infrastructure implementation. Cross-context interaction uses an explicit public application API. REST endpoints return explicit request and response DTOs rather than JPA entities, aggregates, or persistence models.

Keep changes within the bounded context that owns the behavior. Verify Spring Modulith boundaries when imports or module APIs change.

## Database migrations

Implement every schema change as a new Flyway migration.

- Never edit, rename, delete, or reuse the version of an applied migration.
- Do not rely on Hibernate schema generation for application schema changes.
- Make ordering deterministic and define required constraints and indexes.
- Consider existing data before any destructive operation.
- Verify migrations from a clean database and document their impact in the pull request.

## Security and repository hygiene

Never commit credentials, tokens, private keys, certificates, real `.env` files, service-account files, or production configuration. Sensitive values come from environment variables or an approved secret manager.

Review staged changes before committing:

```bash
git diff --cached
```

If a secret is committed, treat it as compromised, revoke or rotate it, remove it, and clean history only when the incident requires it. Ordinary workflow changes must not rewrite repository history.

Do not commit generated build output, IDE state, local-only configuration, debugging artifacts, deprecated copies, or temporary files. Restore material from older repositories through an explicit reviewed allowlist rather than copying whole trees.

Logs must provide useful context without exposing credentials, authorization headers, complete JWT values, secrets, or sensitive personal information.

## Configuration and environments

Synapse selects Spring profiles externally. Never hardcode `spring.profiles.active` in committed source.

```text
application.yml          shared, complete base configuration
application-dev.yml      local/development overrides
application-stage.yml    staging-environment overrides
application-prod.yml     production overrides
application-test.yml     automated-test overrides
```

Runtime environments are independent of Git branches. The deployment environment selects `dev`, `stage`, `prod`, or `test` with `SPRING_PROFILES_ACTIVE`. The same source and immutable image can run with different externally supplied profiles and secrets.

Local Compose defaults to `dev` through `.env.example`. A different profile can be selected explicitly:

```bash
SPRING_PROFILES_ACTIVE=stage ./mvnw spring-boot:run
```

Profile files contain overrides rather than duplicated full configuration. Configuration and environment-variable changes must update `.env.example` and the relevant documentation.

## Dependencies and documentation

Before adding a dependency, check whether the JDK, Spring Boot, or an existing dependency already provides the capability. Review maintenance status, security history, license, transitive cost, and actual use. Explain `pom.xml` changes in the pull request.

Update documentation whenever public or operational behavior changes, including endpoints, environment variables, authentication, ports, database setup, deployment, Docker, or build requirements. Keep architectural decisions and code behavior consistent.

## Definition of done

A change is complete when it is focused, buildable, tested appropriately, free of unrelated files and secrets, consistent with architecture and migration rules, documented where necessary, and verified by CI. Resolve merge conflicts and review risks before merging.
