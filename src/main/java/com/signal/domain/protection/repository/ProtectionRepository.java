package com.signal.domain.protection.repository;

import com.signal.domain.protection.entity.Protection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProtectionRepository extends JpaRepository<Protection, Long> {
}
