package com.redsocial.api.repository;

import com.redsocial.api.domain.Donation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DonationRepository extends JpaRepository<Donation, Long> {

    void deleteByProject_Id(Long projectId);

    long countByProject_Id(Long projectId);
}