# Identity Handle Architecture

The Identity context keeps provider verification, domain identity, persistence,
and discovery behind explicit inward-facing contracts.

```text
Google token
  -> GoogleOAuthAdapter (verification)
  -> OidcPort / VerifiedOidcProfile
  -> GoogleSignInHandler
       existing subject -> SaveUserPort.save
       new subject      -> HandleProvisioningService
                            -> InitialHandlePolicy
                            -> CreateUserPort.create (REQUIRES_NEW insert/flush)
                            -> PostgreSQL named constraints
                            -> cache generation increment after commit

GET /api/v1/users/search
  -> UserSearchUseCase
  -> UserSearchCachePort (Redis, fail-open, 60 seconds)
  -> UserSearchPort (JPA projection, PostgreSQL source of truth)
```

Key dependency rules:

- Domain and application code never import Google adapter DTOs, JPA entities,
  Redis types, or PostgreSQL types.
- `Handle` owns normalization, grammar, and exact reserved-name validation.
- `InitialHandlePolicy` owns display-name sanitization and deterministic
  candidate ordering; it performs no I/O.
- `CreateUserPort` is insert-only. `SaveUserPort` updates an existing aggregate.
- PostgreSQL decides uniqueness. Redis is only a disposable search cache and is
  never queried by allocation.
- Public discovery projections contain no email or provider subject.

Flyway history/backfill is intentionally excluded while the development
database is resettable; the base schema declares the authoritative named
constraints directly.
