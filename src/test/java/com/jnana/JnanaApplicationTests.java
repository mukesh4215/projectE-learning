package com.jnana;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import com.jnana.model.Learner;
import com.jnana.model.Tutor;
import com.jnana.repository.LearnerRepository;
import com.jnana.repository.TutorRepository;
import com.jnana.service.GeneralService;

@ExtendWith(MockitoExtension.class)
class JnanaApplicationTests {

	@Mock
	LearnerRepository learnerRepository;

	@Mock
	TutorRepository tutorRepository;

	@InjectMocks
	GeneralService generalService;

	@Test
	void testProcessForgotPasswordLearnerFoundByMobileNumber() {
		MockHttpSession session = new MockHttpSession();
		Learner learner = new Learner();
		learner.setName("Test Learner");
		learner.setEmail("learner@test.com");
		learner.setMobile(9876543210L);

		when(learnerRepository.findByMobile(9876543210L)).thenReturn(learner);

		String result = generalService.processForgotPassword("9876543210", session);

		assertEquals("redirect:/reset-password-otp", result);
		assertNotNull(session.getAttribute("resetOtp"));
		assertEquals(9876543210L, session.getAttribute("resetMobile"));
	}

	@Test
	void testProcessForgotPasswordWithFormattedNumber() {
		MockHttpSession session = new MockHttpSession();
		Learner learner = new Learner();
		learner.setName("Test Learner");
		learner.setEmail("learner@test.com");
		learner.setMobile(9876543210L);

		when(learnerRepository.findByMobile(9876543210L)).thenReturn(learner);

		String result = generalService.processForgotPassword("+91 9876543210 ", session);

		assertEquals("redirect:/reset-password-otp", result);
		assertNotNull(session.getAttribute("resetOtp"));
		assertEquals(9876543210L, session.getAttribute("resetMobile"));
	}

	@Test
	void testProcessForgotPasswordFoundByEmail() {
		MockHttpSession session = new MockHttpSession();
		Learner learner = new Learner();
		learner.setName("Test Learner");
		learner.setEmail("learner@test.com");
		learner.setMobile(9876543210L);

		when(learnerRepository.findByEmail("learner@test.com")).thenReturn(learner);

		String result = generalService.processForgotPassword("learner@test.com", session);

		assertEquals("redirect:/reset-password-otp", result);
		assertNotNull(session.getAttribute("resetOtp"));
		assertEquals(9876543210L, session.getAttribute("resetMobile"));
	}

	@Test
	void testProcessForgotPasswordTutorFound() {
		MockHttpSession session = new MockHttpSession();
		Tutor tutor = new Tutor();
		tutor.setName("Test Tutor");
		tutor.setEmail("tutor@test.com");
		tutor.setMobile(9876543210L);

		when(learnerRepository.findByMobile(9876543210L)).thenReturn(null);
		when(tutorRepository.findByMobile(9876543210L)).thenReturn(tutor);

		String result = generalService.processForgotPassword("9876543210", session);

		assertEquals("redirect:/reset-password-otp", result);
		assertNotNull(session.getAttribute("resetOtp"));
		assertEquals(9876543210L, session.getAttribute("resetMobile"));
	}

	@Test
	void testProcessForgotPasswordFallbackFindAll() {
		MockHttpSession session = new MockHttpSession();
		Learner learner = new Learner();
		learner.setName("Test Learner");
		learner.setEmail("learner@test.com");
		learner.setMobile(9876543210L);

		when(learnerRepository.findByMobile(9876543210L)).thenReturn(null);
		when(learnerRepository.findAll()).thenReturn(List.of(learner));

		String result = generalService.processForgotPassword("9876543210", session);

		assertEquals("redirect:/reset-password-otp", result);
		assertNotNull(session.getAttribute("resetOtp"));
		assertEquals(9876543210L, session.getAttribute("resetMobile"));
	}

	@Test
	void testProcessForgotPasswordNotFound() {
		MockHttpSession session = new MockHttpSession();
		when(learnerRepository.findByMobile(9876543210L)).thenReturn(null);
		when(learnerRepository.findAll()).thenReturn(Collections.emptyList());
		when(tutorRepository.findByMobile(9876543210L)).thenReturn(null);
		when(tutorRepository.findAll()).thenReturn(Collections.emptyList());

		String result = generalService.processForgotPassword("9876543210", session);

		assertEquals("redirect:/forgot-password", result);
		assertEquals("Mobile Number / Email Not Registered!", session.getAttribute("fail"));
	}
}
