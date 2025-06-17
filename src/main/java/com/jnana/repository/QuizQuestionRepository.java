package com.jnana.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jnana.model.QuizQuestion;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

}
