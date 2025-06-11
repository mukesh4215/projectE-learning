package com.jnana.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Learner {
	@Id
	@GeneratedValue(generator = "learner_id")
	@SequenceGenerator(name = "learner_id", initialValue = 1, allocationSize = 2)
	private Long id;
	private String name;
	private String email;
	private String password;
	private Long mobile;
	
	@OneToMany(cascade = CascadeType.ALL,fetch = FetchType.EAGER)
	List<EnrolledCourse> enrolledCourses = new ArrayList<EnrolledCourse>();

	
	public boolean checkCourse(Long id) {
		for(EnrolledCourse enrolledCourse:this.enrolledCourses) {
			if(enrolledCourse.getCourse().getId()==id) {
				return true;
			}
		}
		return false;
	}
	
}
