# Configuration & Spring Profiles — A Practical Guide

Welcome! This guide explains how configuration works in Synapse and how to use
Spring profiles correctly. You do **not** need to be a Spring expert to follow
it. If you read it top to bottom once, you'll understand how the app gets its
settings, how to run it for a specific environment, and how to add new
configuration without breaking anything.

If you only remember one sentence from this whole document, make it this one:

> **`application.yml` holds everything; the `application-<profile>.yml` files
> only change the few things that differ for that environment; and which
> profile is active is decided _outside_ the code.**

Everything below is just that idea, explained slowly with examples.

---

## 1. What is a "profile" and why do we use one?

Synapse runs in more than one place:

- On **your laptop**, while you develop.
- In a **staging** environment, where we test release candidates.
- In **production**, where real users are.

These places need slightly different behavior. For example, on your laptop it's
helpful to see every SQL query printed to the console. In production, that would
be noisy and could leak information, so we turn it off.

A **Spring profile** is just a named set of overrides for one environment.
Synapse has four:

| Profile | Used for | Typical differences |
|---------|----------|---------------------|
| `dev`   | Your laptop and the development environment | Verbose logging, SQL printed, API docs on |
| `stage` | Release-candidate testing | Production-like, API docs on |
| `prod`  | Production | Quiet logging, SQL off, API docs off |
| `test`  | Automated tests | Stub for now; will grow when integration tests arrive |

You don't have to memorize the exact differences — you can always open the
files and read them. They're short on purpose.

---

## 2. The mental model: a base + thin overlays

This is the part newcomers most often get wrong, so let's go slow.

Spring loads configuration in **two layers**:

1. First it loads `application.yml` — the **base**. This file is complete on its
   own. If no profile were active at all, the app could still start using only
   this file.
2. Then, if a profile is active (say `dev`), it loads `application-dev.yml` **on
   top** and overrides only the keys that file mentions.

So the configuration the app actually runs with is:

```text
application.yml   (everything)
      +
application-dev.yml   (a few overrides)
      =
the settings used at runtime
```

### A concrete example

`application.yml` says:

```yaml
spring:
  jpa:
    open-in-view: false
```

`application-dev.yml` says:

```yaml
spring:
  jpa:
    show-sql: true
```

When you run with the `dev` profile, the app ends up with **both**
`open-in-view: false` (from the base) **and** `show-sql: true` (from dev). The
dev file did not need to repeat `open-in-view` — it was inherited.

### Why we do it this way

Because it keeps each profile file tiny and honest. When you open
`application-prod.yml`, you see _only_ what's special about production. You don't
have to diff four giant near-identical files to spot the one line that matters.

> **Rule:** A profile file must contain **only overrides**, never a full copy of
> the base. If you find yourself pasting the whole config into a profile file,
> stop — that's the mistake this model exists to prevent.

---

## 3. The files in this repo

All configuration lives in `src/main/resources/`:

```text
src/main/resources/
├── application.yml          ← shared base, complete and bootable
├── application-dev.yml      ← development overrides
├── application-stage.yml    ← staging overrides
├── application-prod.yml     ← production overrides
└── application-test.yml     ← test overrides (a stub for now)
```

Here is exactly what each profile file currently contains, so you know there's
no hidden magic.

**`application-dev.yml`** — make local development comfortable:

```yaml
spring:
  jpa:
    show-sql: true              # print SQL to the console
    properties:
      hibernate:
        format_sql: true        # ...nicely formatted
logging:
  level:
    dev.amir.synapse: DEBUG     # chatty logs for our own code
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG
```

**`application-stage.yml`** — production-like, but still observable:

```yaml
spring:
  jpa:
    show-sql: false
logging:
  level:
    dev.amir.synapse: INFO
    org.springframework.web: INFO
```

**`application-prod.yml`** — quiet and locked down:

```yaml
spring:
  jpa:
    show-sql: false
springdoc:
  api-docs:
    enabled: false              # no public API docs in production
  swagger-ui:
    enabled: false
logging:
  level:
    root: WARN
    dev.amir.synapse: INFO
```

**`application-test.yml`** — a placeholder until we add integration tests:

```yaml
spring:
  jpa:
    show-sql: false
logging:
  level:
    dev.amir.synapse: DEBUG
```

> If you want to change how an environment behaves, you edit its profile file —
> not the base, unless the change should apply **everywhere**.

---

## 4. How a profile gets activated (and why never in code)

Here's a rule that might feel surprising at first:

> **We never write `spring.profiles.active` in any committed file.**

The following is **forbidden** anywhere in the repository:

```yaml
# DO NOT do this
spring:
  profiles:
    active: dev
```

### Why not? It seems easier.

It seems easier, but it quietly breaks our Git workflow. Synapse promotes code
through three permanent branches:

```text
develop  →  staging  →  main
```

If we hardcoded `active: dev` in `develop` and `active: prod` in `main`, that one
line would be **different on every branch forever**. Every time we promote
`develop → staging → main`, Git would see a conflict on that line. Our project
requires a clean, linear history, so this would cause friction on every single
release. The whole point of promotion is that the **same code** moves forward
unchanged — and a per-branch profile value sabotages that.

### So how is it chosen?

Through an **environment variable** named `SPRING_PROFILES_ACTIVE`, set
**outside** the code — by Docker Compose locally, and by the deployment system
when running in a real environment. Spring reads this variable automatically; we
don't write any code to consume it.

This way:

- The code is identical on every branch.
- The same built image can run as `dev`, `stage`, or `prod` just by changing one
  environment variable.

---

## 5. How environments map to profiles

| Branch    | Environment | `SPRING_PROFILES_ACTIVE` | Where it's set |
|-----------|-------------|--------------------------|----------------|
| `develop` | Development | `dev`                    | `compose.yaml` locally; deployment config when deployed |
| `staging` | Staging     | `stage`                  | deployment config |
| `main`    | Production  | `prod`                   | deployment config |

The same container image is promoted through all three. Only the injected
`SPRING_PROFILES_ACTIVE` (and the secrets) differ. We never build a separate
image per environment.

> The "deployment config" above refers to our GitOps/Helm setup
> (`values-dev.yaml`, `values-stage.yaml`, `values-prod.yaml`). If you only work
> locally, you can ignore that part — Compose is all you need.

---

## 6. Running Synapse locally

For everyday local work you don't have to do anything special. The local default
is `dev`, set in `.env.example`:

```text
SPRING_PROFILES_ACTIVE=dev
```

### Start the supporting services (database + cache)

```fish
docker compose up -d synapse-database synapse-cache --wait
```

### Run the app

```fish
./mvnw spring-boot:run
```

### Run under a different profile

To temporarily run as if you were in staging or production, set the variable for
that one command. In **Fish** (this project's shell):

```fish
SPRING_PROFILES_ACTIVE=stage ./mvnw spring-boot:run
```

In **bash/zsh**, the same idea:

```bash
SPRING_PROFILES_ACTIVE=stage ./mvnw spring-boot:run
```

### Confirm which profile is active

When the app starts, look near the top of the log for a line like:

```text
The following 1 profile is active: "dev"
```

If you don't see your expected profile there, the variable didn't reach the app —
see the troubleshooting section below.

---

## 7. How to add a new configuration property

This is the most common configuration task, so here's a step-by-step recipe.
Suppose you're adding a setting called `synapse.feature.max-upload-mb`.

**Step 1 — Decide: is it the same everywhere, or does it differ per environment?**

- **Same everywhere** → it goes in `application.yml` (the base). Most settings
  are like this.
- **Differs per environment** → put a sensible default in `application.yml`, then
  override it in the specific profile file(s) that need a different value.

**Step 2 — Add it to the base** (`application.yml`):

```yaml
synapse:
  feature:
    max-upload-mb: 10
```

**Step 3 — If the value is a secret or environment-specific, use a placeholder**
instead of a literal, and supply the real value via an environment variable:

```yaml
synapse:
  feature:
    max-upload-mb: ${MAX_UPLOAD_MB:10}    # 10 is the fallback default
```

**Step 4 — If you introduced a new environment variable, document it** by adding
it to `.env.example` with a safe local default:

```text
MAX_UPLOAD_MB=10
```

**Step 5 — Update the docs.** Per our contribution rules, adding a config
property or environment variable means updating the relevant documentation
(this file and/or `CONTRIBUTING.md`). Code and docs must describe the same
system.

**Step 6 — Verify:**

```fish
./mvnw clean verify
```

---

## 8. Secrets: the one thing you must never get wrong

Real client secrets, JWT signing keys, and database passwords must **never** be
committed — not in `application.yml`, not in a profile file, not anywhere.

- In configuration, refer to them with placeholders: `${JWT_SECRET}`,
  `${GOOGLE_CLIENT_SECRET}`, etc.
- Locally, you provide real values in a `.env` file, which is **git-ignored** and
  must stay that way.
- `.env.example` exists to show _which_ variables are needed, using **fake**
  local-only values. Never put a real secret in `.env.example`.

If you ever accidentally commit a secret, treat it as compromised: rotate it
immediately and tell a maintainer. Removing it in a later commit is **not**
enough — it still lives in Git history. (See `CONTRIBUTING.md` → Security Rules.)

---

## 9. Troubleshooting & common mistakes

**"My profile isn't being picked up."**
The log says a different profile than you expected. Check that
`SPRING_PROFILES_ACTIVE` is actually set in the environment you're running in. If
you're using Compose, confirm it's present in the service's `environment:` block.
If you ran a one-off command, make sure you put the variable _before_ the command
on the same line.

**"I copied the whole config into `application-prod.yml` and now changes don't
take effect."**
Profile files are overlays, not replacements. Delete everything from the profile
file except the keys that genuinely differ from the base. Duplicated keys are
confusing and easy to get out of sync.

**"The app starts but can't reach the database."**
Configuration is probably fine — make sure the database container is up:
`docker compose up -d synapse-database --wait`. Then check the datasource
variables in your `.env`.

**"I added a setting in a profile file but it's `null` at runtime."**
A profile file only applies when that profile is active. If you put a _required_
setting only in `application-dev.yml`, it will be missing under `prod`. Required
settings belong in `application.yml` (the base); profile files are for
_overrides_ of things the base already defines.

**"Should I hardcode the active profile just for my branch to test something?"**
No. Set `SPRING_PROFILES_ACTIVE` for your run instead (Section 6). Never commit a
hardcoded active profile.

---

## 10. Quick reference

```text
Base config (always loaded):     src/main/resources/application.yml
Per-environment overrides:       src/main/resources/application-<profile>.yml
Profiles:                        dev | stage | prod | test
Activation:                      SPRING_PROFILES_ACTIVE  (env var, never in code)
Local default:                   dev  (from .env.example)

Run locally:                     ./mvnw spring-boot:run
Run as another profile:          SPRING_PROFILES_ACTIVE=stage ./mvnw spring-boot:run
Confirm active profile:          look for 'The following 1 profile is active' in logs
Verify build:                    ./mvnw clean verify
```

---

## 11. Where to go next

- **Why** we made this design decision (the architecture record):
  `docs/adr/0001-spring-profiles-via-external-activation.md`
- **The rules** for configuration and contributions:
  `CONTRIBUTING.md` → *Configuration and Environment Profiles* and *Security Rules*

If anything here is unclear, that's a documentation bug — open an issue or ask in
review. A guide that a newcomer can't follow isn't finished, and we'd genuinely
like to fix it.