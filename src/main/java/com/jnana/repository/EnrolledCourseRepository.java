package com.jnana.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jnana.model.EnrolledCourse;
import com.jnana.model.EnrolledSection;

public interface EnrolledCourseRepository extends JpaRepository<EnrolledCourse, Long> {
	EnrolledCourse findByEnrolledSections(EnrolledSection section);
}
