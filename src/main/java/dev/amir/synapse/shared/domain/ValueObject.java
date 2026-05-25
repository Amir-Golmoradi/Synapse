package dev.amir.synapse.shared.domain;

import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;
import org.jspecify.annotations.NonNull;

/**
 *
 *
 * <h3>Base class for Value Objects in the domain model.</h3>
 *
 * <p>Value Objects are immutable by design: once they are initialized, their state cannot and
 * should not change. This immutability guarantees consistency and reliability across the system.
 *
 * <p>One of the main benefits of using Value Objects is to prevent confusion or mixing of domain
 * information. For example, in a system managing users, Value Objects can enforce that user-related
 * information (such as UserId, Email, or Address) is clearly separated and cannot be accidentally
 * interchanged.
 *
 * <p><b>Real-life scenario:</b> Imagine a system where you manage customer orders. Each user has a
 * unique UserId, an Email, and a shipping Address. Without Value Objects, it would be easy to
 * accidentally assign a shipping Address of one user to another, or use a UserId in place of an
 * Email, causing wrong deliveries or account mix-ups. By encapsulating each concept in its own
 * Value Object, the compiler and design enforce that UserId, Email, and Address are always used
 * correctly, avoiding costly mistakes in production.
 *
 * <p>Value Objects also define equality based on their content rather than identity, making
 * comparisons and collections behavior (like HashSet or HashMap) predictable and safe.
 */
public abstract class ValueObject {
  public abstract List<Object> getAtomicValues();

  @Override
  public boolean equals(Object obj) {
    return obj instanceof ValueObject other && valuesAreEqual(other);
  }

  @Override
  public int hashCode() {
    var values = StreamSupport.stream(getAtomicValues().spliterator(), false).toArray();

    return Objects.hash(values);
  }

  // Compares two Value Objects based on their atomic values.
  private boolean valuesAreEqual(@NonNull ValueObject otherValue) {
    // Iterable itself does not provide element-by-element comparison like List.
    // Therefore, we convert the Iterable to a List using a Stream.
    //
    // Spliterator allows traversing the Iterable element by element.
    // Streams provide a modern, concise, and safe alternative to manual for-loops
    // for processing and collecting elements.

    var thisValues = StreamSupport.stream(getAtomicValues().spliterator(), false).toList();

    var otherValues =
        StreamSupport.stream(otherValue.getAtomicValues().spliterator(), false).toList();

    return thisValues.equals(otherValues);
  }
}
