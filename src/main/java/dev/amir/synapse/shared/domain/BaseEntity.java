package dev.amir.synapse.shared.domain;

import java.util.Objects;

@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class BaseEntity<TId> {
  private final TId id;

  protected BaseEntity(TId id) {
    this.id = Objects.requireNonNull(id, "");
  }

  public TId getId() {
    return id;
  }

  @Override
  public boolean equals(Object other) {
    return this == other || (other instanceof BaseEntity<?> entity && id.equals(entity.id));
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
