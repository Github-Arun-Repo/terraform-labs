package com.terraformlabs.ums.repository;

import com.terraformlabs.ums.entity.AppUser;
import com.terraformlabs.ums.entity.RefreshToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserAndRevokedFalse(AppUser user);
}
