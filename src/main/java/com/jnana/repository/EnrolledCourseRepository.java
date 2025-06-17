package com.jnana.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jnana.model.Course;
import com.jnana.model.EnrolledCourse;
import com.jnana.model.EnrolledSection;

public interface EnrolledCourseRepository extends JpaRepository<EnrolledCourse, Long> {

	EnrolledCourse findByEnrolledSections(EnrolledSection section);
	
	List<EnrolledCourse> findByCourseIn(List<Course> courses);


}
