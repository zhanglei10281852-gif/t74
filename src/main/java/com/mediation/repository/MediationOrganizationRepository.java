package com.mediation.repository;

import com.mediation.entity.MediationOrganization;
import com.mediation.entity.MediationOrganization.AreaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediationOrganizationRepository extends JpaRepository<MediationOrganization, Long> {

    List<MediationOrganization> findByAreaType(AreaType areaType);
}
