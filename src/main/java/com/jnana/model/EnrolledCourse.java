package com.jnana.model;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;

@Entity
@Data
public class EnrolledCourse {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	private Course course;

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	private List<EnrolledSection> enrolledSections;
	
	private boolean courseCompleted;
	private boolean finalQuizCompleted;
	private LocalDate completionDate;
}
