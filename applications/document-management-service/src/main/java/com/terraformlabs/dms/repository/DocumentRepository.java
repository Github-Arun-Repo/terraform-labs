package com.terraformlabs.dms.repository;

import com.terraformlabs.dms.entity.Document;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    Optional<Document> findByIdAndUserId(Long id, Long userId);
}
