package dev.amir.synapse.shared.domain;

import java.time.Instant;

/**
 * Marker interface for all Domain Events in the domain model.
 *
 * <p>Domain Events represent something important that happened in the domain and other parts of the
 * system (or other bounded contexts) may be interested in. Every domain event must implement this
 * interface so that AggregateRoot can collect them in a type-safe way and the event publisher can
 * handle them uniformly.
 *
 * <p>This is a core part of Model-Driven Design (Chapter 3 of Eric Evans' DDD book) and makes
 * events explicit in the ubiquitous language.
 *
 * @see AggregateRoot
 * @since 1.0
 */
@FunctionalInterface
public interface DomainEvent {
  Instant occurredOn();
}
