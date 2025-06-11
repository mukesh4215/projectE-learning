package com.jnana.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
public class EnrolledSection {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@ManyToOne
	private Section section;
	private boolean sectionCompleted;
	private boolean sectionQuizCompleted;
}
