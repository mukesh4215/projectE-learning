package com.jnana.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jnana.model.Tutor;

public interface TutorRepository extends JpaRepository<Tutor, Long> {
	boolean existsByMobile(Long mobile);

	boolean existsByEmail(String email);
	
	Tutor findByEmail(String email);

	@Query("SELECT t FROM Tutor t WHERE t.mobile = :mobile")
	Tutor findByMobile(@Param("mobile") Long mobile);
}

