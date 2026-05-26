package com.dawn.identity.repository;

import com.dawn.common.core.constant.URole;
import com.dawn.identity.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Page<User> findAll(Pageable pageable);

    List<User> findByIdIn(List<Long> ids);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    @Query("""
            SELECT CASE
                WHEN COUNT(u) > 0
                THEN true ELSE false END
            FROM User AS u JOIN u.roles AS r
            WHERE r.name = :roleName
            """)
    boolean existsByRolesName(URole roleName);
}
