package dev.amir.synapse.messaging.infrastructure.adapter.out.persistence.model;

import dev.amir.synapse.messaging.domain.enums.RoomStatus;
import dev.amir.synapse.messaging.domain.enums.RoomType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * JPA persistence entity for the {@code Room} aggregate.
 *
 * <p>This class lives exclusively in the infrastructure layer. It is not the domain model. The
 * domain {@code Room} aggregate is mapped to/from this entity by {@link RoomPersistenceMapper}.
 *
 * <p>Design decisions:
 *
 * <ul>
 *   <li>ID is supplied by the domain — no {@code @GeneratedValue}. The domain owns identity.
 *   <li>{@code @Version} enables optimistic locking for concurrent member mutations.
 *   <li>Enums stored as STRING — ordinal storage breaks on enum reordering.
 *   <li>{@code TIMESTAMPTZ} for all timestamps — PostgreSQL timezone-aware type.
 *   <li>{@code room_type} and {@code created_at} are {@code updatable = false} — immutable after
 *       creation.
 *   <li>Members stored via {@code @ElementCollection} of {@link RoomMemberEmbeddable} in a separate
 *       {@code room_members} table. {@code LAZY} fetch — loading members on every room read is
 *       wasteful.
 * </ul>
 */
@Entity
@Table(
    name = "rooms",
    indexes = {
      @Index(name = "idx_rooms_status", columnList = "status"),
      @Index(name = "idx_rooms_type", columnList = "room_type"),
      @Index(name = "idx_rooms_last_messages_at", columnList = "last_messages_at")
    })
public class RoomJpaEntity {

  @Id
  @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(name = "room_type", nullable = false, updatable = false, length = 20)
  private RoomType roomType;

  // Nullable: DIRECT rooms have no name
  @Column(name = "name", length = 100)
  private String name;

  // Nullable: DIRECT rooms have no avatar; GROUP/CHANNEL avatar is optional
  @Column(name = "avatar_url", length = 2048)
  private String avatarUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private RoomStatus status;

  @Column(
      name = "created_at",
      nullable = false,
      updatable = false,
      columnDefinition = "TIMESTAMPTZ")
  private Instant createdAt;

  @Column(name = "last_messages_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
  private Instant lastMessagesAt;

  /**
   * Members stored in a dedicated {@code room_members} table, one row per member, each carrying the
   * user id, role, and join timestamp.
   *
   * <p>LAZY fetch is intentional — the member list is only needed for membership checks and
   * mutations, not for every room query (e.g., inbox listing).
   */
  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "room_members",
      joinColumns = @JoinColumn(name = "room_id", referencedColumnName = "id"),
      indexes = {@Index(name = "idx_room_members_user_id", columnList = "user_id")})
  private Set<RoomMemberEmbeddable> members = new HashSet<>();

  /**
   * Optimistic locking version.
   *
   * <p>Critical for {@code addMember} and {@code removeMember} — two concurrent requests mutating
   * the member set without versioning would silently corrupt it. With {@code @Version}, the second
   * writer gets an {@code OptimisticLockException}, which the application layer retries.
   */
  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  // ── Constructors ─────────────────────────────────────────────────────────

  /** Required by JPA. Never call directly. */
  protected RoomJpaEntity() {}

  private RoomJpaEntity(
      UUID id,
      RoomType roomType,
      String name,
      String avatarUrl,
      RoomStatus status,
      Instant createdAt,
      Instant lastMessagesAt,
      Long version,
      Set<RoomMemberEmbeddable> members) {
    this.id = id;
    this.roomType = roomType;
    this.name = name;
    this.avatarUrl = avatarUrl;
    this.status = Objects.requireNonNull(status, "Room status cannot be null");
    this.createdAt = Objects.requireNonNull(createdAt, "Created timestamp cannot be null");
    this.lastMessagesAt =
        Objects.requireNonNull(lastMessagesAt, "Last message timestamp cannot be null");
    this.version = version;
    this.members = new HashSet<>(members);
  }

  public static RoomJpaEntity create(
      UUID id,
      RoomType roomType,
      String name,
      String avatarUrl,
      Set<RoomMemberEmbeddable> members) {
    var now = Instant.now();
    return new RoomJpaEntity(
        id, roomType, name, avatarUrl, RoomStatus.ACTIVE, now, now, null, members);
  }

  public static RoomJpaEntity fromDomainState(
      UUID id,
      RoomType roomType,
      String name,
      String avatarUrl,
      RoomStatus status,
      Instant createdAt,
      Instant lastMessagesAt,
      Long version,
      Set<RoomMemberEmbeddable> members) {
    return new RoomJpaEntity(
        id, roomType, name, avatarUrl, status, createdAt, lastMessagesAt, version, members);
  }

  // ── Getters ──────────────────────────────────────────────────────────────

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Long getVersion() {
    return version;
  }

  public RoomStatus getStatus() {
    return status;
  }

  public RoomType getRoomType() {
    return roomType;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Set<RoomMemberEmbeddable> getMembers() {
    return members;
  }

  public Instant getLastMessagesAt() {
    return lastMessagesAt;
  }
}
