## Overview

|                     |                                                                                         |
|---------------------|-----------------------------------------------------------------------------------------|
| **Type**            | `[ ]` Feature · `[ ]` Fix · `[ ]` Refactor · `[ ]` Chore · `[ ]` Docs                   |
| **Breaking Change** | `[ ]` Yes · `[ ]` No                                                                    |
| **Modules Touched** | `[ ]` identity · `[ ]` messaging · `[ ]` presence · `[ ]` calling · `[ ]` shared-kernel |

**What and why** — one or two sentences. What problem does this solve?

>

**Approach** — how did you solve it? Note any alternatives you considered and rejected.

>

---

## Architecture & Domain Integrity

- [ ] Spring Modulith boundaries verified — no illegal cross-module imports
- [ ] Domain invariants enforced in the Aggregate, not leaked into the Application layer
- [ ] Ports (in/out) correctly reflect use-case intent — no persistence types crossing the domain boundary
- [ ] New domain events published where state changes are externally observable

**Invariants added or modified** *(leave blank if none)*

>

---

## Quality Gates

#### Automated

- [ ] **Unit tests** — domain logic and use-case handlers covered with JUnit 5 & AssertJ, zero Spring context loaded
- [ ] **Integration tests** — cross-layer flows verified locally via Testcontainers (PostgreSQL / Redis)
- [ ] **Static analysis** — `mvn spotless:apply` applied; Checkstyle, PMD, SpotBugs report zero violations

#### Manual Evidence

<details>
<summary>Terminal output — <code>mvn clean verify</code></summary>

```text
// Paste output here
```

</details>

---

## Security & GitOps

- [ ] No secrets, API keys, or raw configs committed
- [ ] Multi-stage Docker build layer cache verified locally
- [ ] `helm/values.yaml` image tag updated for local cluster consumption
- [ ] ArgoCD sync verified clean on local Minikube

---

## Reviewer Notes

> Anything the reviewer should pay special attention to, or context that isn't obvious from the diff.