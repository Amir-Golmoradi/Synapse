# Contributing to Synapse

Thank you for contributing to Synapse.

This project follows strict rules for branch management, commits, pull requests, architecture, testing, security, database migrations, and repository cleanliness.

These rules exist to:

* Maintain a clean and reliable Git history
* Prevent incomplete or unrelated changes from entering protected branches
* Prevent direct pushes to permanent branches
* Simplify code review, debugging, and rollback
* Preserve architectural boundaries
* Prevent temporary, generated, deprecated, or sensitive files from entering the repository
* Keep the project ready for team development and production deployment

Changes that do not follow this document must not be merged.

---

## 1. Branch Structure

Synapse has three permanent branches:

| Branch    | Purpose                                                           |
|-----------|-------------------------------------------------------------------|
| `develop` | Integration branch for completed features and development changes |
| `staging` | Release-candidate branch for final testing and validation         |
| `main`    | Stable, production-ready branch                                   |

The standard delivery flow is:

```text
<normal-type>/* ──PR──> develop ──PR──> staging ──PR──> main
```

Normal development branches must never be merged directly into `staging` or `main`.
Here, `<normal-type>` means `feature`, `fix`, `refactor`, `test`, `docs`, `chore`, or `ci`.

---

## 2. Non-Negotiable Branch Rules

### 2.1 Direct Pushes Are Forbidden

Direct pushes to permanent branches are strictly forbidden:

```text
develop
staging
main
```

All changes must enter these branches through Pull Requests.

Forbidden:

```bash
git push origin develop
git push origin staging
git push origin main
```

Required flow:

```text
<normal-type>/* → Pull Request → develop
develop → Pull Request → staging
staging → Pull Request → main
```

### 2.2 Force Pushes Are Forbidden

Force pushes to permanent branches are forbidden:

```bash
git push --force origin develop
git push --force origin staging
git push --force origin main
```

A force push to a personal development branch is allowed only when necessary and only with:

```bash
git push --force-with-lease
```

Never use:

```bash
git push --force
```

`--force-with-lease` must be preferred because it protects remote work that was not present locally.

### 2.3 Branch Deletion Requires an Explicit Decision

A branch must not be deleted automatically merely because its Pull Request was merged.

Branch deletion is allowed only when the repository owner or the responsible developer explicitly decides that the branch is no longer needed.

Do not assume that merged development branches should always be deleted.

---

## 3. Creating a New Branch

Normal development branches must always be created from the latest `develop`.

```bash
git switch develop
git pull --ff-only origin develop
git switch -c feature/messaging-room-invitations
```

Creating a normal development branch from `main` or `staging` is forbidden.

### Branch Naming Convention

Use the following format:

```text
<type>/<short-kebab-case-description>
```

Allowed prefixes:

| Prefix      | Purpose                                             |
| ----------- | --------------------------------------------------- |
| `feature/`  | New functionality                                   |
| `fix/`      | Bug fix                                             |
| `refactor/` | Internal redesign without intended behavior changes |
| `test/`     | Test additions or corrections                       |
| `docs/`     | Documentation changes                               |
| `chore/`    | Maintenance, configuration, or repository work      |
| `ci/`       | CI/CD and GitHub Actions changes                    |
| `hotfix/`   | Emergency production fix                            |

Valid examples:

```text
feature/messaging-room-invitations
feature/identity-passwordless-login
fix/refresh-token-race-condition
refactor/room-persistence-mapper
test/google-token-validator
docs/api-authentication-guide
chore/update-maven-dependencies
ci/add-container-security-scan
```

Forbidden examples:

```text
new-feature
my-branch
amir-test
fix
update
final
latest
branch-1
test2
```

Branch names must clearly communicate their purpose without requiring someone to inspect the code.

---

## 4. One Branch, One Purpose

Every branch must have one clearly defined purpose.

A Pull Request must not combine unrelated work such as:

* A new feature
* A large refactor
* CI/CD changes
* Docker changes
* README rewrites
* Database schema changes
* Unrelated architecture changes

Bad example:

```text
Add room invitations, update Docker, refactor Identity, and rewrite README
```

This work must be divided into separate branches and Pull Requests.

A reviewer should be able to understand the purpose of a branch from its name and the purpose of a Pull Request from its title.

---

## 5. Commit Rules

Synapse uses Conventional Commits.

### Commit Format

```text
<type>(<scope>): <description>
```

Examples:

```text
feat(messaging): add room invitation workflow
fix(identity): prevent concurrent refresh token reuse
refactor(shared): simplify aggregate event handling
test(identity): cover expired Google token validation
docs(api): document refresh token endpoint
chore(build): update Maven plugins
ci(pipeline): add container image scanning
```

### Allowed Commit Types

```text
feat
fix
refactor
test
docs
chore
build
ci
perf
style
revert
```

### Commit Subject Rules

The commit subject must:

* Be written in English
* Be concise and specific
* Use the imperative mood
* Include a meaningful scope
* Explain what the change does
* Not end with a period
* Not contain vague wording

Good:

```text
feat(identity): add internal user lookup API
```

Forbidden:

```text
update
fix bug
changes
final changes
working version
some fixes
WIP
test commit
done
new code
```

### Atomic Commits

Every commit must:

* Represent one logical change
* Compile successfully
* Keep existing tests passing
* Avoid unrelated files
* Be independently understandable
* Be safe to review or revert

Do not commit:

* Half-finished implementations
* Commented-out experimental code
* Temporary debugging statements
* Placeholder methods without an explicit reason
* Broken tests
* Generated build output
* Local-only configuration

---

## 6. Pull Request Rules

### 6.1 Pull Request Targets

For normal feature, fix, refactor, test, documentation, chore, and CI changes:

```text
base: develop
compare: <normal-type>/*
```

For release-candidate promotion:

```text
base: staging
compare: develop
```

For production promotion:

```text
base: main
compare: staging
```

The following flows are forbidden:

```text
<normal-type>/* → main
<normal-type>/* → staging
develop → main
main → develop
```

The only exception is the documented emergency hotfix process.

### 6.2 Pull Request Titles

Pull Request titles must follow Conventional Commits:

```text
feat(messaging): add room invitation workflow
```

Forbidden titles:

```text
New PR
Update project
Changes
Final version
Fixes
Merge this
Latest changes
Work completed
```

The Pull Request title may become the final squash commit message, so it must be clean and meaningful.

### 6.3 Pull Request Description

Every Pull Request must contain at least the following sections:

```markdown
## Summary

Briefly explain the purpose of the change.

## Changes

- First important change
- Second important change
- Third important change

## Verification

- `./mvnw clean verify`
- Relevant tests were executed
- Build completed successfully

## Risks

Describe known risks, or write `None`.

## Database Changes

Describe migrations or schema changes, or write `None`.
```

A Pull Request with an incomplete, vague, or empty description must not be merged.

### 6.4 Pull Request Size

Pull Requests should remain small and reviewable.

Recommended maximum:

```text
400 lines of meaningful change
```

Generated files, lock files, and necessary database migrations may be exceptions.

A Pull Request larger than approximately 800 changed lines must either:

* Be split into smaller Pull Requests, or
* Include a clear explanation for why splitting is not practical

Large changes must not be used as an excuse to mix unrelated work.

### 6.5 Draft Pull Requests

Incomplete Pull Requests must be created as Drafts.

A Draft may be marked ready for review only when:

* The implementation is complete
* The build passes
* All relevant tests pass
* The description is complete
* Temporary files are removed
* Debug code is removed
* Known risks are documented
* The branch is synchronized with its target when necessary

### 6.6 Merge Strategy

For features, fixes, tests, and refactors entering `develop`, use:

```text
Squash and merge
```

The final commit title should match the Conventional Commit-style Pull Request title.

For promotions between permanent branches:

```text
develop → staging
staging → main
```

Use the repository-approved merge strategy.

Promotion Pull Requests must not contain additional unrelated changes.

---

## 7. Mandatory Checks Before Merge

Before a Pull Request is merged, run:

```bash
./mvnw clean verify
git diff --check
git status --short
```

Expected build result:

```text
BUILD SUCCESS
```

Before pushing the final branch state, the working tree should be clean:

```text
nothing to commit, working tree clean
```

A Pull Request must not be merged when:

* The build fails
* Any test fails
* Checkstyle fails
* Spotless fails
* PMD fails
* SpotBugs fails
* Compilation fails
* Conflicts exist
* Temporary files exist
* Unrelated changes exist
* Secrets or credentials are present
* Database migrations are missing
* Required documentation is outdated
* Architecture boundaries are violated

---

## 8. Testing Rules

Every behavioral change must include appropriate tests.

### New Features

A new feature should include tests for:

* The primary success path
* Important validation failures
* Authorization failures when applicable
* Domain rule violations
* Important edge cases

### Bug Fixes

Every bug fix must include a regression test that:

1. Fails before the fix
2. Passes after the fix

### Security-Critical and Reliability-Critical Areas

Tests are mandatory for changes involving:

* Authentication
* Authorization
* JWT creation and verification
* Refresh-token rotation
* Token revocation
* Concurrency
* Database transactions
* Domain invariants
* Input validation
* External API adapters
* Persistence mappers
* Security filters
* Locking behavior
* Idempotency

Disabling or deleting tests merely to make the build pass is forbidden.

Do not add:

```java
@Disabled
```

unless there is:

* A clear technical explanation
* A linked issue or task
* A plan for re-enabling the test

---

## 9. Architecture Rules

Synapse follows:

* Domain-Driven Design
* Hexagonal Architecture
* CQRS
* Bounded Contexts
* Dependency Inversion
* Explicit application boundaries

### Dependency Direction

The intended dependency direction is:

```text
Infrastructure → Application → Domain
```

The Domain layer must not depend on Application or Infrastructure.

### Domain Layer Restrictions

The Domain layer must not directly depend on:

* Spring Framework
* JPA
* Hibernate
* HTTP
* REST DTOs
* Database implementations
* Controllers
* Infrastructure repositories
* Serialization frameworks

Annotations such as the following are forbidden inside the Domain layer:

```java
@Entity
@Service
@Component
@RestController
@Repository
```

Domain objects must express business behavior without framework dependencies.

### Communication Between Bounded Contexts

A bounded context must not depend directly on another bounded context's internal implementation.

Forbidden:

```text
messaging → identity.infrastructure
messaging → identity.domain.model
messaging → identity.persistence
```

Cross-context communication must use an explicit public application API:

```text
messaging → identity.application.api
```

Example:

```java
UserLookupUseCase
```

Internal domain entities must not be exposed as cross-context contracts.

### Entities and API DTOs

The following objects must not be returned directly from REST endpoints:

* JPA entities
* Domain aggregates
* Persistence models
* Internal infrastructure models

Controllers must use explicit request and response DTOs.

---

## 10. Database Migration Rules

Every schema change must be implemented through a new database migration.

Rules:

* Never edit a migration that has already been applied
* Never rename an applied migration
* Never reuse a migration version
* Never change the schema without a migration
* Never silently delete production data
* Never rely only on Hibernate automatic schema generation

Valid examples:

```text
V3__create_rooms_table.sql
V4__add_room_status_index.sql
```

Forbidden actions:

```text
Editing V1 after it has been applied
Deleting V2
Reusing version V3
Changing entities without adding a migration
```

Each migration should:

* Work from a clean database
* Have a deterministic order
* Define required constraints
* Add appropriate indexes
* Consider existing production data
* Avoid destructive operations unless explicitly reviewed
* Use clear and descriptive names

Migration changes must be documented in the Pull Request.

---

## 11. Security Rules

Never commit:

```text
.env
.env.*
private keys
JWT signing keys
API keys
Google client secrets
database passwords
access tokens
refresh tokens
certificates
credentials
production configuration
service-account files
```

Sensitive values must come from environment variables or an approved secret-management system.

Example:

```yaml
google:
  client-id: ${GOOGLE_CLIENT_ID}
```

Before committing, review staged changes:

```bash
git diff --cached
```

A secret remains compromised even after the file is removed from a later commit because it may still exist in Git history.

If a secret is committed:

1. Treat it as compromised
2. Revoke or rotate it immediately
3. Remove it from the repository
4. Clean the Git history when necessary
5. Document the incident appropriately

---

## 12. Repository Cleanliness Rules

The following must not enter Git:

```text
.git/
.idea/
.vscode/
target/
build/
out/
node_modules/
*.class
*.jar
*.log
*.tmp
*.swp
.DS_Store
.env
coverage/
local configuration
IDE metadata
database dumps
temporary exports
```

Before staging files:

```bash
git status --short
```

Every unknown file must be inspected before being staged.

Avoid broad staging commands:

```bash
git add .
git add -A
```

Prefer explicit paths:

```bash
git add src/main/java/dev/amir/synapse/messaging
git add src/test/java/dev/amir/synapse/messaging
```

Broad staging is allowed only when the developer has already reviewed the complete working tree and understands every file being added, modified, or deleted.

---

## 13. Restoring Files from an Older Repository

Blindly copying files from an older repository is strictly forbidden.

Never run:

```bash
cp -a ../old-project/. .
```

This may copy:

* An old `.git` directory
* Broken Git history
* IDE settings
* Build output
* Secrets
* Deprecated files
* Obsolete workflows
* Incompatible configuration
* Temporary development files

Old project files must be restored through an explicit allowlist.

Each category should use a separate branch and Pull Request when practical:

```text
chore/restore-tests
ci/restore-github-workflows
chore/restore-docker-infrastructure
docs/restore-project-documentation
```

For every restored category:

1. List the candidate files
2. Review their purpose
3. Check for secrets and local configuration
4. Check whether they are still required
5. Copy only approved files
6. Review the Git diff
7. Run the complete build
8. Create an isolated Pull Request

Do not copy old files merely because they existed in the previous project.

---

## 14. Dependency Rules

Every new dependency must have a clear technical justification.

Before adding a dependency, verify:

* Whether the JDK already provides the required capability
* Whether Spring Boot already provides it
* Whether an equivalent dependency already exists
* Whether the project is actively maintained
* Whether it has known security vulnerabilities
* Whether its license is acceptable
* Whether its transitive dependencies are reasonable
* Whether the dependency is actually used

Unused, duplicate, abandoned, or experimental dependencies must not be introduced without explicit justification.

Every `pom.xml` change must be explained in the Pull Request.

---

## 15. Logging Rules

Logs must:

* Provide useful operational context
* Use an appropriate log level
* Avoid exposing sensitive data
* Be understandable during incident investigation
* Avoid unnecessary noise

Never log:

* Passwords
* Access tokens
* Refresh tokens
* Complete JWT values
* Authorization headers
* Secrets
* Private keys
* Sensitive personal information

Forbidden:

```java
log.info("Token: {}", token);
```

Prefer safe identifiers and non-sensitive metadata.

---

## 16. Documentation Rules

Any change that modifies public or operational behavior must update the relevant documentation.

Examples include:

* New endpoints
* New environment variables
* Port changes
* Authentication-flow changes
* Database setup changes
* Docker Compose changes
* Build command changes
* Deployment changes
* New infrastructure requirements
* New configuration properties

Outdated, incomplete, or misleading documentation must not remain after a feature is merged.

Code and documentation should describe the same system.

---

## 17. Emergency Hotfix Process

A hotfix is allowed only for a serious production issue.

Create the branch from `main`:

```bash
git switch main
git pull --ff-only origin main
git switch -c hotfix/refresh-token-reuse
```

Primary hotfix flow:

```text
hotfix/* → main
```

After the fix enters `main`, the same change must be propagated back through Pull Requests so that permanent branches do not diverge.

Recommended synchronization flow:

```text
main → staging
staging → develop
```

A hotfix must not include:

* New features
* Broad refactoring
* Cosmetic cleanup
* Unrelated dependency upgrades
* Documentation rewrites unrelated to the incident

A hotfix must remain as small and focused as possible.

---

## 18. Definition of Done

A change is complete only when:

* The branch has one clear purpose
* The implementation is complete
* The project compiles
* The build passes
* Relevant tests pass
* New behavior has appropriate tests
* Checkstyle passes
* Spotless passes
* PMD passes
* SpotBugs passes
* Database migrations are valid
* Security implications were reviewed
* Documentation was updated
* Pull Request details are complete
* No conflicts exist
* No unrelated files exist
* No secrets were introduced
* Architecture boundaries are respected
* The change followed the correct branch flow

“Works on my machine” is not a valid definition of done.

---

## 19. Final Pull Request Checklist

Before requesting a merge:

```markdown
- [ ] The branch was created from the latest `develop`
- [ ] The Pull Request has one clear purpose
- [ ] The Pull Request title follows Conventional Commits
- [ ] `./mvnw clean verify` passes
- [ ] Required tests were added or updated
- [ ] No test was disabled to make the build pass
- [ ] `git diff --check` reports no errors
- [ ] No temporary or generated file is tracked
- [ ] No secret or credential is present
- [ ] Database migrations are correct
- [ ] Architecture boundaries are respected
- [ ] Relevant documentation is updated
- [ ] The Pull Request targets the correct branch
- [ ] No merge conflict exists
- [ ] No unrelated change is included
- [ ] Risks are documented
- [ ] Database changes are documented
```

---

## 20. Final Principle

When you are uncertain whether a file, dependency, migration, configuration change, or architectural decision belongs in the repository:

```text
Do not commit it yet.
```

Inspect it first.

Every change should be:

* Intentional
* Small
* Focused
* Reviewable
* Tested
* Secure
* Documented
* Reversible

Repository quality, maintainability, and reliability are more important than merging quickly.
