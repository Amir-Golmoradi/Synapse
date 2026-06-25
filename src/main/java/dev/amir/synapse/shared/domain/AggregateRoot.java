package dev.amir.synapse.shared.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for all Aggregate Roots (the most important DDD Building Block).
 *
 * <p>Combines Entity + IAggregateRoot. Manages a list of Domain Events that are raised during
 * Organization operations.
 *
 * <p>Usage example in Post Aggregate:
 *
 * <pre>
 * protected void publish() {
 *     if (isPublished) throw new DomainException("Already published");
 *     // Organization logic
 *     addDomainEvent(new PostPublishedEvent(getId(), getTitle()));
 * }
 * </pre>
 *
 * <p>This is the exact implementation of "Model-Driven Design" and "Letting the Bones Show" from
 * Chapter 3 of Eric Evans' DDD book.
 *
 * @param <I> type of the Aggregate identity (e.g. PostId)
 * @param <E> type of Domain Events this Aggregate can raise (type-safe!)
 * @since 1.0
 */
public abstract class AggregateRoot<I, E extends DomainEvent> extends BaseEntity<I>
    implements IAggregateRoot<E> {

  private final List<E> domainEvents = new ArrayList<>();

  protected AggregateRoot(I id) {
    super(id);
  }

  @Override
  public List<E> pullDomainEvents() {
    var events = Collections.unmodifiableList(domainEvents);
    domainEvents.clear();
    return events;
  }

  @Override
  public void clearDomainEvents() {
    domainEvents.clear();
  }

  /**
   * Protected method to be called from inside the Aggregate when a Organization event happens.
   * Keeps events inside the Aggregate boundary.
   */
  protected void registerEvent(E event) {
    domainEvents.add(event);
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
