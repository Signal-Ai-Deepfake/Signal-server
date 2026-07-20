package com.signal.domain.deepfakedetection.repository;

import com.signal.domain.deepfakedetection.entity.DeepfakeDetection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeepfakeDetectionRepository extends JpaRepository<DeepfakeDetection, Long> {
}
