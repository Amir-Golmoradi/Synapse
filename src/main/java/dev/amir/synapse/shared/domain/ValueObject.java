package dev.amir.synapse.shared.domain;

/**
 * Marker interface identifying a type as a Domain-Driven Design Value Object.
 *
 * <p>Deliberately empty — immutability and structural equality/hashCode are guaranteed by
 * implementing this interface via a {@code record}, not by inheritance.
 */
public interface ValueObject {}
