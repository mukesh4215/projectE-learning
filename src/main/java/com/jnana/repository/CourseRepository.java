package com.jnana.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jnana.model.Course;
import com.jnana.model.Tutor;

public interface CourseRepository extends JpaRepository<Course, Long> {
	List<Course> findByTutor(Tutor attribute);
	
	List<Course> findByPublishedTrue();

}
