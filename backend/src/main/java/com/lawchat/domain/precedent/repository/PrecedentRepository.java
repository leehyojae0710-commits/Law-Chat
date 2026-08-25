package com.lawchat.domain.precedent.repository;

import com.lawchat.domain.precedent.entity.Precedent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrecedentRepository extends JpaRepository<Precedent, Long> {

    Optional<Precedent> findByCaseNumber(String caseNumber);

    boolean existsByCaseNumber(String caseNumber);
}
