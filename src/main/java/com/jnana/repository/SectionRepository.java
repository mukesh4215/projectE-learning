package com.jnana.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jnana.model.Course;
import com.jnana.model.Section;

public interface SectionRepository extends JpaRepository<Section, Long> {
	List<Section> findByCourse(Course course);
}
