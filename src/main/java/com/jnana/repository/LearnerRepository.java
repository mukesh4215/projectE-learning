package com.jnana.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jnana.model.EnrolledCourse;
import com.jnana.model.Learner;

public interface LearnerRepository extends JpaRepository<Learner, Long> {
	boolean existsByMobile(Long mobile);

	boolean existsByEmail(String email);
	
	Learner findByEmail(String email);
	
	@Query("SELECT l FROM Learner l WHERE l.mobile = :mobile")
	Learner findByMobile(@Param("mobile") Long mobile);
	
	List<Learner> findByEnrolledCoursesIn(List<EnrolledCourse> enrolledCourses);
}

