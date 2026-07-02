package dev.amir.synapse.messaging.domain.model;

import dev.amir.synapse.messaging.domain.enums.RoomRole;
import dev.amir.synapse.messaging.domain.enums.RoomStatus;
import dev.amir.synapse.messaging.domain.enums.RoomType;
import dev.amir.synapse.messaging.domain.event.MemberCreatedEvent;
import dev.amir.synapse.messaging.domain.event.MemberRemovedEvent;
import dev.amir.synapse.messaging.domain.event.MemberRoleChangedEvent;
import dev.amir.synapse.messaging.domain.event.MembersAddedEvent;
import dev.amir.synapse.messaging.domain.event.RoomArchivedEvent;
import dev.amir.synapse.messaging.domain.event.RoomCreatedEvent;
import dev.amir.synapse.messaging.domain.exception.RoomValidationException;
import dev.amir.synapse.messaging.domain.policy.RoomGuards;
import dev.amir.synapse.messaging.domain.value_object.MemberId;
import dev.amir.synapse.messaging.domain.value_object.RoomId;
import dev.amir.synapse.messaging.domain.value_object.RoomMember;
import dev.amir.synapse.shared.domain.AggregateRoot;
import dev.amir.synapse.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
  private final Long version;

  /**
   * Returns an unmodifiable snapshot of the current members. Callers must use {@link #addMember}
   * and {@link #removeMember} for mutations.
   */
  public Map<MemberId, RoomMember> getMembers() {
    return Map.copyOf(members);
  }

  /**
   * Members is the authoritative membership set for this room. It is mutable only through {@link
   * #addMember} and {@link #removeMember}, never through direct field access.
   */
  private final Map<MemberId, RoomMember> members;

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
    this.members = new HashMap<>();
    for (RoomMember m : snapshot.initialMembers()) {
      this.members.put(m.getMemberId(), m);
    }
    this.createdAt =
        Objects.requireNonNull(snapshot.createdAt(), "Created timestamp cannot be null");
    this.lastMessagesAt = Objects.requireNonNullElse(snapshot.lastMessagesAt(), this.createdAt);
    this.status = Objects.requireNonNull(snapshot.status(), "Room status cannot be null");
    this.version = snapshot.version();
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
    // Rule: a 1:1 has no owner — both participants are equal MEMBERs.
    var roles = Map.of(creatorId, RoomRole.MEMBER, recipientId, RoomRole.MEMBER);
    return create(RoomType.DIRECT, null, null, roles);
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

    // Rule 3: the creator becomes OWNER.
    return create(RoomType.GROUP, name, avatarUrl, Map.of(creatorId, RoomRole.OWNER));
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

    return create(RoomType.CHANNEL, name, avatarUrl, Map.of(creatorId, RoomRole.OWNER));
  }

  /**
   * Recreates a room from persistent snapshot without emitting domain events.
   *
   * <p>Use this only from persistence adapters. Application code should use the creation factories.
   */
  public static Room reconstitute(RoomSnapshot snapshot) {
    RoomGuards.validateCreation(
        snapshot.roomType(),
        snapshot.name(),
        snapshot.avatarUrl(),
        memberIds(snapshot.initialMembers()));
    return new Room(snapshot);
  }

  // ── Member management ───────────────────────────────────────────────────

  // ── Guard methods ───────────────────────────────────────────────────────
  private static Room create(
      RoomType roomType, String name, String avatarUrl, Map<MemberId, RoomRole> roleAssignments) {
    var now = Instant.now();
    var normalizedName = normalize(name);

    Set<RoomMember> initialMembers =
        roleAssignments.entrySet().stream()
            .map(e -> RoomMember.create(e.getKey(), e.getValue(), now))
            .collect(Collectors.toSet());

    var snapshot =
        createSnapshot(
            RoomId.generate(), roomType, normalizedName, avatarUrl, now, now, initialMembers);

    RoomGuards.validateCreation(
        snapshot.roomType(), snapshot.name(), snapshot.avatarUrl(), memberIds(initialMembers));
    var room = new Room(snapshot);

    var event =
        new RoomCreatedEvent(room.getId(), room.getRoomType(), room.getName(), room.getAvatarUrl());
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

  private static Set<MemberId> memberIds(Set<RoomMember> members) {
    return members.stream().map(RoomMember::getMemberId).collect(Collectors.toSet());
  }

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
      Instant createdAt,
      Instant lastMessagesAt,
      Set<RoomMember> initialMembers) {
    return new RoomSnapshot(
        id,
        roomType,
        name,
        avatarUrl,
        RoomStatus.ACTIVE,
        createdAt,
        lastMessagesAt,
        null,
        initialMembers);
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

    if (members.containsKey(memberId)) {
      throw new RoomValidationException(
          "User '%s' is already a member of this room.".formatted(memberId.getValue()));
    }

    RoomGuards.validateCanAddMember(getRoomType(), memberCount());

    var newMember = RoomMember.create(memberId, RoomRole.MEMBER, Instant.now());
    members.put(memberId, newMember);
    registerEvent(new MemberCreatedEvent(getId(), memberId, newMember.getJoinedAt()));
  }

  /**
   * Adds multiple members atomically.
   *
   * <p>If any requested member is invalid or already belongs to this room, no members are added and
   * no domain event is emitted.
   */
  public void addMembers(Set<MemberId> memberIds) {
    requireActive();
    Objects.requireNonNull(memberIds, "Member IDs cannot be null");

    if (memberIds.isEmpty()) {
      return;
    }

    memberIds.forEach(memberId -> Objects.requireNonNull(memberId, "Member ID cannot be null"));
    var newMemberIds = Set.copyOf(memberIds);

    var existingMember =
        newMemberIds.stream().filter(members::containsKey).findFirst().orElse(null);
    if (existingMember != null) {
      throw new RoomValidationException(
          "User '%s' is already a member of this room.".formatted(existingMember.getValue()));
    }

    RoomGuards.validateCanAddMembers(getRoomType(), memberCount(), newMemberIds.size());

    var joinedAt = Instant.now();
    newMemberIds.forEach(
        memberId -> members.put(memberId, RoomMember.create(memberId, RoomRole.MEMBER, joinedAt)));
    registerEvent(new MembersAddedEvent(getId(), newMemberIds, joinedAt));
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
    if (!members.containsKey(userId)) {
      throw new RoomValidationException(
          "User '%s' is not a member of this room.".formatted(userId.getValue()));
    }

    if (members.size() == LAST_MEMBER_COUNT) {
      throw new RoomValidationException(
          "Cannot remove the last member of a room. Archive the room instead.");
    }

    members.remove(userId);
    var event = new MemberRemovedEvent(getId(), userId);
    registerEvent(event);
  }

  public void changeMemberRole(MemberId actorId, MemberId targetId, RoomRole newRole) {
    requireActive();
    Objects.requireNonNull(actorId, "Actor ID cannot be null");
    Objects.requireNonNull(targetId, "Target ID cannot be null");
    Objects.requireNonNull(newRole, "New role cannot be null");

    if (roomType == RoomType.DIRECT) {
      throw new RoomValidationException("Direct message rooms do not have member roles.");
    }

    var actor = members.get(actorId);
    var target = members.get(targetId);
    if (actor == null) {
      throw new RoomValidationException("Actor is not a member of this room.");
    }
    if (target == null) {
      throw new RoomValidationException("Target user is not a member of this room.");
    }

    // Rules 4–6 all begin with "Owner can ..." — only the Owner may change roles.
    if (!actor.isOwner()) {
      throw new RoomValidationException("Only the room owner can change member roles.");
    }
    if (actorId.equals(targetId)) {
      throw new RoomValidationException("The owner cannot change their own role.");
    }

    var currentRole = target.getRole();
    if (currentRole == newRole) {
      throw new RoomValidationException("Member already has role '%s'.".formatted(newRole));
    }

    switch (newRole) {
      case ADMIN -> { // Rule 4: Member -> Admin
        if (currentRole != RoomRole.MEMBER) {
          throw new RoomValidationException("Only a MEMBER can be promoted to ADMIN.");
        }
        members.put(targetId, target.withRole(RoomRole.ADMIN));
      }
      case MEMBER -> { // Rule 6: Admin -> Member
        if (currentRole != RoomRole.ADMIN) {
          throw new RoomValidationException("Only an ADMIN can be demoted to MEMBER.");
        }
        members.put(targetId, target.withRole(RoomRole.MEMBER));
      }
      case OWNER -> { // Rule 5: Admin -> Owner, and the old Owner becomes Admin
        if (currentRole != RoomRole.ADMIN) {
          throw new RoomValidationException("Only an ADMIN can be promoted to OWNER.");
        }
        members.put(targetId, target.withRole(RoomRole.OWNER));
        members.put(
            actorId, actor.withRole(RoomRole.ADMIN)); // preserves the single-owner invariant
      }
    }

    registerEvent(new MemberRoleChangedEvent(getId(), targetId, currentRole, newRole));
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
   * Changes the avatar of a group or channel room.
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
  public void changeAvatar(String newAvatarUrl) {
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
    registerEvent(new RoomArchivedEvent(getId()));
  }

  public boolean hasMember(MemberId userId) {
    Objects.requireNonNull(userId, "User ID cannot be null");
    return members.containsKey(userId);
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

  public Long getVersion() {
    return version;
  }

  public Instant getLastMessagesAt() {
    return lastMessagesAt;
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
