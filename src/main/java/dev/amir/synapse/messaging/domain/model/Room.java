package dev.amir.synapse.messaging.domain.model;

import dev.amir.synapse.messaging.domain.enums.RoomStatus;
import dev.amir.synapse.messaging.domain.enums.RoomType;
import dev.amir.synapse.messaging.domain.event.MemberCreatedEvent;
import dev.amir.synapse.messaging.domain.event.MemberRemovedEvent;
import dev.amir.synapse.messaging.domain.event.RoomArchivedEvent;
import dev.amir.synapse.messaging.domain.event.RoomCreatedEvent;
import dev.amir.synapse.messaging.domain.exception.RoomValidationException;
import dev.amir.synapse.messaging.domain.policy.RoomGuards;
import dev.amir.synapse.messaging.domain.value_object.MemberId;
import dev.amir.synapse.messaging.domain.value_object.RoomId;
import dev.amir.synapse.shared.domain.AggregateRoot;
import dev.amir.synapse.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Room aggregate root.
 *
 * <p>A Room is the structural unit of conversation in Synapse. It has three types:
 *
 * <ul>
 *   <li>{@link RoomType#DIRECT} — A private 1:1 conversation between exactly two users. Immutable
 *       membership. No name or avatar.
 *   <li>{@link RoomType#GROUP} — A named multi-user conversation. Membership is bounded at 2000.
 *       Requires a creator.
 *   <li>{@link RoomType#CHANNEL} — A named broadcast space. Unbounded membership. Requires a
 *       creator.
 * </ul>
 *
 * <p>All state transitions enforce domain invariants and emit domain events.
 */
public final class Room extends AggregateRoot<RoomId, DomainEvent> {
  // ── Capacity invariants ─────────────────────────────────────────────────
  private static final int NAME_MIN_LENGTH = 1;
  private static final int NAME_MAX_LENGTH = 100;
  private static final int LAST_MEMBER_COUNT = 1;
  private static final int AVATAR_URL_MAX_LENGTH = 2048;

  // ── State ───────────────────────────────────────────────────────────────
  private final RoomType roomType;
  private final Instant createdAt;

  /**
   * Members is the authoritative membership set for this room. It is mutable only through {@link
   * #addMember} and {@link #removeMember}, never through direct field access.
   */
  private final Set<MemberId> members;

  private String name;
  private String avatarUrl;
  private RoomStatus status;
  private Instant lastMessagesAt;

  // ── Constructor ─────────────────────────────────────────────────────────
  private Room(RoomSnapshot snapshot) {
    super(snapshot.id());
    this.roomType = Objects.requireNonNull(snapshot.roomType(), "Room type cannot be null");
    this.name = snapshot.name();
    this.avatarUrl = snapshot.avatarUrl();
    this.members = new HashSet<>(snapshot.initialMembers());
    this.createdAt =
        Objects.requireNonNull(snapshot.createdAt(), "Created timestamp cannot be null");
    this.lastMessagesAt = Objects.requireNonNullElse(snapshot.lastMessagesAt(), this.createdAt);
    this.status = Objects.requireNonNull(snapshot.status(), "Room status cannot be null");
  }

  // ── Factory methods ─────────────────────────────────────────────────────

  /**
   * Creates a direct message room between exactly two distinct users.
   *
   * <p>Invariants enforced:
   *
   * <ul>
   *   <li>Both participants must be non-null.
   *   <li>A user cannot start a DM with themselves.
   *   <li>Membership is sealed at exactly two users — it cannot change after creation.
   * </ul>
   */
  public static Room createDirectRoom(MemberId creatorId, MemberId recipientId) {
    Objects.requireNonNull(creatorId, "Direct message creator must have an ID");
    Objects.requireNonNull(recipientId, "Recipient ID cannot be null");

    if (creatorId.equals(recipientId)) {
      throw new RoomValidationException(
          "A direct message cannot be created between the same user.");
    }
    var members = Set.of(creatorId, recipientId);
    return create(RoomType.DIRECT, null, null, members);
  }

  /**
   * Creates a named group room. The creator becomes the first member automatically.
   *
   * <p>Invariants enforced:
   *
   * <ul>
   *   <li>Creator must be non-null — a room without an owner is an orphan.
   *   <li>Name must be non-blank and within [{@value #NAME_MIN_LENGTH}, {@value #NAME_MAX_LENGTH}]
   *       characters.
   *   <li>Avatar URL, if provided, must not exceed {@value #AVATAR_URL_MAX_LENGTH} characters.
   * </ul>
   */
  public static Room createGroupRoom(MemberId creatorId, String name, String avatarUrl) {
    Objects.requireNonNull(creatorId, "Group room creator must have an ID");
    validateName(name);
    validateAvatarUrl(avatarUrl);

    var initialMembers = Set.of(creatorId);
    return create(RoomType.GROUP, name, avatarUrl, initialMembers);
  }

  /**
   * Creates a named channel room. The creator becomes the first member automatically.
   *
   * <p>Invariants enforced: same as {@link #createGroupRoom}, with unbounded membership capacity.
   */
  public static Room createChannel(MemberId creatorId, String name, String avatarUrl) {
    Objects.requireNonNull(creatorId, "Channel creator must have an ID");
    validateName(name);
    validateAvatarUrl(avatarUrl);

    return create(RoomType.CHANNEL, name, avatarUrl, Set.of(creatorId));
  }

  /**
   * Recreates a room from persistent snapshot without emitting domain events.
   *
   * <p>Use this only from persistence adapters. Application code should use the creation factories.
   */
  public static Room reconstitute(RoomSnapshot snapshot) {
    RoomGuards.validateCreation(
        snapshot.roomType(), snapshot.name(), snapshot.avatarUrl(), snapshot.initialMembers());
    return new Room(snapshot);
  }

  // ── Member management ───────────────────────────────────────────────────

  // ── Guard methods ───────────────────────────────────────────────────────
  private static Room create(
      RoomType roomType, String name, String avatarUrl, Set<MemberId> initialMembers) {
    var now = Instant.now();
    var snapshot =
        createSnapshot(
            RoomId.generate(),
            roomType,
            name,
            avatarUrl,
            RoomStatus.ACTIVE,
            now,
            now,
            initialMembers);

    RoomGuards.validateCreation(
        snapshot.roomType(), snapshot.name(), snapshot.avatarUrl(), snapshot.initialMembers());
    var room = new Room(snapshot);

    var event =
        new RoomCreatedEvent(
            room.getId(),
            room.getRoomType(),
            room.getName(),
            room.getAvatarUrl(),
            room.getCreatedAt());
    room.registerEvent(event);

    return room;
  }

  private static void validateName(String name) {
    if (name == null || name.isBlank()) {
      throw new RoomValidationException("Room name cannot be blank.");
    }
    var stripped = name.strip();
    if (stripped.length() > NAME_MAX_LENGTH) {
      throw new RoomValidationException(
          "Room name cannot exceed %d characters. Got: %d."
              .formatted(NAME_MAX_LENGTH, stripped.length()));
    }
    if (stripped.isEmpty()) {
      throw new RoomValidationException(
          "Room name must be at least %d character(s).".formatted(NAME_MIN_LENGTH));
    }
  }

  // ── Mutations ───────────────────────────────────────────────────────────

  private static void validateAvatarUrl(String avatarUrl) {
    if (avatarUrl != null && avatarUrl.strip().length() > AVATAR_URL_MAX_LENGTH) {
      throw new RoomValidationException(
          "Avatar URL cannot exceed %d characters.".formatted(AVATAR_URL_MAX_LENGTH));
    }
  }

  /**
   * Normalizes user-supplied strings. Uses {@code strip()} instead of {@code trim()} to handle
   * Unicode whitespace correctly.
   */
  private static String normalize(String value) {
    return value != null ? value.strip() : null;
  }

  private static RoomSnapshot createSnapshot(
      RoomId id,
      RoomType roomType,
      String name,
      String avatarUrl,
      RoomStatus status,
      Instant createdAt,
      Instant lastMessagesAt,
      Set<MemberId> initialMembers) {
    return new RoomSnapshot(
        id,
        roomType,
        name,
        avatarUrl,
        status,
        createdAt,
        lastMessagesAt,
        Set.copyOf(initialMembers));
  }

  /**
   * Adds a new member to this room.
   *
   * <p>Invariants enforced:
   *
   * <ul>
   *   <li>Room must be {@link RoomStatus#ACTIVE}.
   *   <li>Direct message rooms have sealed membership — no additions permitted.
   *   <li>The user must not already be a member (no silent no-ops).
   *   <li>Group rooms cannot exceed 2000 members.
   * </ul>
   */
  public void addMember(MemberId memberId) {
    requireActive();
    Objects.requireNonNull(memberId, "Member ID cannot be null");

    if (members.contains(memberId)) {
      throw new RoomValidationException(
          "User '%s' is already a member of this room.".formatted(memberId.getValue()));
    }

    RoomGuards.validateCanAddMember(getRoomType(), memberCount());

    members.add(memberId);
    var event = new MemberCreatedEvent(getId(), memberId, Instant.now());
    registerEvent(event);
  }

  // ── Domain queries ──────────────────────────────────────────────────────

  /**
   * Removes an existing member from this room.
   *
   * <p>Invariants enforced:
   *
   * <ul>
   *   <li>Room must be {@link RoomStatus#ACTIVE}.
   *   <li>Direct message rooms have sealed membership — no removals permitted.
   *   <li>The user must be a current member.
   *   <li>The last member cannot be removed — archive the room instead. An empty room is a
   *       corrupted aggregate.
   * </ul>
   */
  public void removeMember(MemberId userId) {
    requireActive();
    Objects.requireNonNull(userId, "Member ID cannot be null");

    if (roomType == RoomType.DIRECT) {
      throw new RoomValidationException(
          "Direct message rooms have sealed membership. Members cannot be removed.");
    }
    if (!members.contains(userId)) {
      throw new RoomValidationException(
          "User '%s' is not a member of this room.".formatted(userId.getValue()));
    }

    if (members.size() == LAST_MEMBER_COUNT) {
      throw new RoomValidationException(
          "Cannot remove the last member of a room. Archive the room instead.");
    }

    members.remove(userId);
    var event = new MemberRemovedEvent(getId(), userId, Instant.now());
    registerEvent(event);
  }

  /**
   * Renames a group or channel room.
   *
   * <p>Invariants enforced:
   *
   * <ul>
   *   <li>Room must be {@link RoomStatus#ACTIVE}.
   *   <li>Direct message rooms are system-defined — they have no name to set.
   *   <li>New name must be non-blank and within the allowed length range.
   * </ul>
   */
  public void rename(String newName) {
    requireActive();
    requireNamed("Direct message rooms cannot be renamed.");
    validateName(newName);
    this.name = normalize(newName);
  }

  /**
   * Updates the avatar of a group or channel room.
   *
   * <p>Invariants enforced:
   *
   * <ul>
   *   <li>Room must be {@link RoomStatus#ACTIVE}.
   *   <li>Direct message rooms derive their avatar from participant profiles — not from the room
   *       itself.
   *   <li>URL, if provided, must not exceed {@value #AVATAR_URL_MAX_LENGTH} characters.
   * </ul>
   */
  public void updateAvatar(String newAvatarUrl) {
    requireActive();
    requireNamed("Direct message rooms cannot have an explicit avatar.");
    validateAvatarUrl(newAvatarUrl);
    this.avatarUrl = normalize(newAvatarUrl);
  }

  /**
   * Records a new message activity timestamp on this room. Used to sort inbox by recency without
   * querying the messages table.
   *
   * <p>Invariant: Only active rooms receive messages.
   */
  public void recordMessageActivity() {
    requireActive();
    this.lastMessagesAt = Instant.now();
  }

  /**
   * Archives this room. Archived rooms are read-only. No new messages, members, or renames are
   * accepted.
   *
   * <p>Invariant: Only an active room can be archived. Archiving an already-archived room is a
   * no-op error, not a silent pass.
   */
  public void archive() {
    requireActive();
    this.status = RoomStatus.ARCHIVED;
    registerEvent(new RoomArchivedEvent(getId(), Instant.now()));
  }

  public boolean hasMember(MemberId userId) {
    Objects.requireNonNull(userId, "User ID cannot be null");
    return members.contains(userId);
  }

  public int memberCount() {
    return members.size();
  }

  public boolean isActive() {
    return status == RoomStatus.ACTIVE;
  }

  public boolean isDirect() {
    return roomType == RoomType.DIRECT;
  }

  // ── Getters ─────────────────────────────────────────────────────
  public String getName() {
    return name;
  }

  public RoomStatus getStatus() {
    return status;
  }

  // ── Equality ─────────────────────────────────────────────────────────────

  public RoomType getRoomType() {
    return roomType;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getLastMessagesAt() {
    return lastMessagesAt;
  }

  /**
   * Returns an unmodifiable view of the current members. Callers must use {@link #addMember} and
   * {@link #removeMember} for mutations.
   */
  public Set<MemberId> getMembers() {
    return Collections.unmodifiableSet(members);
  }

  /**
   * Identity-based equality. Two Room instances are the same room if and only if they share the
   * same {@link RoomId}. All other fields are mutable state and must not participate in equality.
   *
   * <p>The previous implementation used getName().equals(...) which throws a NullPointerException
   * for DIRECT rooms, where name is intentionally null.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Room other)) return false;
    return Objects.equals(getId(), other.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId());
  }

  private void requireActive() {
    if (status != RoomStatus.ACTIVE) {
      throw new RoomValidationException(
          "Operation not permitted on a %s room."
              .formatted(status.name().toLowerCase(Locale.ROOT)));
    }
  }

  private void requireNamed(String message) {
    if (roomType == RoomType.DIRECT) {
      throw new RoomValidationException(message);
    }
  }
}
