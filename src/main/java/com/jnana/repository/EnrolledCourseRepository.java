package com.jnana.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jnana.model.EnrolledCourse;

public interface EnrolledCourseRepository extends JpaRepository<EnrolledCourse, Long> {
	
}
