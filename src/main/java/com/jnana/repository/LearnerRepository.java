package com.jnana.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jnana.model.EnrolledCourse;
import com.jnana.model.Learner;

public interface LearnerRepository extends JpaRepository<Learner, Long> {
	boolean existsByMobile(long mobile);

	boolean existsByEmail(String email);
	
	Learner findByEmail(String email);
	
	List<Learner> findByEnrolledCoursesIn(List<EnrolledCourse> enrolledCourses);
}
