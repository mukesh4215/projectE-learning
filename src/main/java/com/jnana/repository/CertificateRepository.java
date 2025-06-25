package com.jnana.repository;



import org.springframework.data.jpa.repository.JpaRepository;

import com.jnana.model.Certificate;
import com.jnana.model.Course;
import com.jnana.model.Learner;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
	
	Certificate findByLearnerAndCourse(Learner attribute, Course course);

}
