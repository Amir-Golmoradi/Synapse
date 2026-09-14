package dev.amir.synapse.identity.infrastructure.adapter.out.persistence.user_search;

import dev.amir.synapse.identity.infrastructure.adapter.out.persistence.user.UserEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

@FunctionalInterface
public interface UserSearchJpaRepository extends Repository<UserEntity, UUID> {
  @Query(
      value =
          """
          SELECT id AS "userId",
                 handle AS "handle",
                 display_name AS "displayName",
                 profile_picture_url AS "profilePictureUrl"
          FROM users
          WHERE handle LIKE :prefixPattern ESCAPE '\\'
          ORDER BY handle ASC
          LIMIT :limit OFFSET :offset
          """,
      nativeQuery = true)
  List<UserSearchJpaProjection> searchByHandlePrefix(
      @Param("prefixPattern") String prefixPattern,
      @Param("limit") int limit,
      @Param("offset") long offset);
}
