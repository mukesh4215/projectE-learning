package com.jnana.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.reactive.resource.NoResourceFoundException;

@ControllerAdvice
public class MyExceptionHandler {
	@ExceptionHandler(NoResourceFoundException.class)
	public String handle(NoResourceFoundException e) {
		return "404.html";
	}
}
