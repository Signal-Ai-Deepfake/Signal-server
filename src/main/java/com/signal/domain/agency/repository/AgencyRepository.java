package com.signal.domain.agency.repository;

import com.signal.domain.agency.entity.Agency;
import com.signal.domain.agency.entity.AgencySituationType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AgencyRepository extends JpaRepository<Agency, Long> {

    @Query("SELECT DISTINCT a FROM Agency a JOIN a.supportedSituationTypes s WHERE s = :situationType")
    List<Agency> findBySituationType(@Param("situationType") AgencySituationType situationType);
}
