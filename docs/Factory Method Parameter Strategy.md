# ADR 004: Factory Method Parameter Strategies (Value Objects vs. Primitives)

## Status

Accepted

## Context

When implementing structural factory methods (such as DDD reconstitution factories used to hydrate Domain Entities from database persistence layers or network payloads), we face a recurring design decision regarding method signature design:

1. **Option A (Value Object Parameters):** Forcing upstream callers to pre-wrap data into domain Value Objects (Email, FullName) before invoking the factory.
2. **Option B (Primitive/String Parameters):** Accepting raw primitives (String, UUID, int) and delegating Value Object instantiation to the internal body of the factory method.

```java
// Option A: Value Objects as Parameters
public static User reconstitute(Email email, String googleId, FullName fullName, String profilePictureUrl) {}

// Option B: Primitives as Parameters
public static User reconstitute(String email, String googleId, String firstName, String lastName, String profilePictureUrl) {}

```

### Key Triggers & Friction Points

* **Persistence Mappings:** Most Object-Relational Mappers (ORMs) or data mappers extract table rows as primitive types.
* **Validation Blast Radius:** Instantiating a Value Object often triggers structural or format validation (e.g., throwing an InvalidEmailException). Where this validation happens changes how we handle errors in our data pipelines.
* **Primitive Obsession:** Allowing raw strings to proliferate deep into the application layers risks mixing up positional arguments (e.g., passing a lastName into a firstName parameter).

---

## Decision

We will use a **hybrid, context-driven approach** that strictly segregates the strategy based on the architectural boundary layer:

### 1. Database Reconstitution & Infrastructure Layers $\rightarrow$ **Use Option B (Primitives)**

When hydrating an Entity from an internal infrastructure source (Repositories, DB Mappers, Event Consumers), the factory method **MUST accept primitive types**.

* **Why:** The infrastructure layer's responsibility is flat mapping. Pushing Value Object creation *inside* the factory keeps the mapping code clean and atomic. If data in the database is malformed, the validation fails exactly at the point of domain entry (inside the factory).

### 2. Domain-to-Domain & Application Services $\rightarrow$ **Use Option A (Value Objects)**

When an Entity is being derived, transformed, or constructed across internal domain services or use-cases, the factory **MUST accept domain Value Objects**.

* **Why:** This enforces type-safety across compile time and completely eradicates positional argument bugs. If a domain service already possesses an Email object, forcing it to call .getValue() just to pass it to a factory that calls Email.of() creates unnecessary boilerplate and redundant validations.

---

## Consequences

### Positive (Benefits)

* **Decoupled Infrastructure:** Data mappers remain lightweight because they pass raw database types straight into the reconstitution factory.
* **Bulletproof Domain APIs:** Internal domain communication is strictly type-safe. It is compiler-impossible to accidentally swap an Email parameter with a GoogleId parameter.
* **Localized Validation:** Failures during infrastructure loading are trapped exactly during the of() or reconstitute() phase, preventing semi-constructed, invalid domain models from creeping into memory.

### Negative (Drawbacks)

* **Dual-Factory Maintenance:** Entities may occasionally require two distinct factory methods (of(...) for raw input and reconstitute(...) for typed input), slightly increasing class surface area.
* **Parameter Drift:** If a Value Object changes internally (e.g., FullName expands to include a middle name), a primitive-based factory signature must change, whereas a VO-based signature remains unaffected.

---

## Compliance & Examples

### Invariant Rule

> Never wrap primitives into Value Objects *solely* to pass them into a reconstitution factory if those primitives were just pulled out of a flat data row. Let the factory do the lifting.

#### Good Practice (Data Hydration Pipeline)

```java
// Infrastructure/Repository layer passes raw DB values cleanly
public User mapRow(ResultSet rs) {
    return User.reconstitute(
        rs.getString("email"),
        rs.getString("google_id"),
        rs.getString("first_name"),
        rs.getString("last_name"),
        rs.getString("profile_url")
    );
}

```

#### Good Practice (Domain Core Interaction)

```java
// Domain Service already handles typed objects; passes them directly
public User upgradeToGoogleUser(User existingUser, GoogleProfileClaim claim) {
    return User.createNewImplicit(
        existingUser.getEmail(), // Already typed as Email
        claim.getGoogleId(),
        existingUser.getFullName() // Already typed as FullName
    );
}

```