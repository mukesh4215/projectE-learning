package com.jnana.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.jnana.model.Learner;
import com.jnana.model.Tutor;
import com.jnana.mydto.AccountType;
import com.jnana.mydto.UserDto;
import com.jnana.repository.LearnerRepository;
import com.jnana.repository.TutorRepository;

import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Service
public class GeneralService {
	@Autowired
	LearnerRepository learnerRepository;

	@Autowired
	PasswordEncoder encoder;

	@Autowired
	TutorRepository tutorRepository;

	@Autowired
	JavaMailSender mailSender;

	@Autowired
	TemplateEngine templateEngine;

	@Value("${OTP_TIME:300}")
	long otpTime;

	@Value("${spring.mail.username:noreply@jnana.com}")
	private String email;

	public String loadRegister(UserDto userDto, Model model) {
		model.addAttribute("userDto", userDto);
		return "register.html";
	}

	public String register(UserDto userDto, BindingResult result, HttpSession session) {
		if (!userDto.getConfirmPassword().equals(userDto.getPassword()))
			result.rejectValue("confirmPassword", "error.confirmPassword",
					"* Password and Confirm Password not matching");

		if (learnerRepository.existsByMobile(userDto.getMobile())
				|| tutorRepository.existsByMobile(userDto.getMobile()))
			result.rejectValue("mobile", "error.mobile", "* Mobile Number Already in Use");

		if (learnerRepository.existsByEmail(userDto.getEmail()) || tutorRepository.existsByEmail(userDto.getEmail()))
			result.rejectValue("email", "error.email", "* Email Address Already in Use");

		if (!result.hasErrors()) {
			int otp = new Random().nextInt(100000, 1000000);
			session.setAttribute("otp", otp);
			session.setAttribute("userDto", userDto);
			sendEmail(otp, userDto);
			session.setAttribute("time", LocalDateTime.now());
			session.setAttribute("pass", "OTP Sent Successfully. Check your email (or console log).");
			return "redirect:/otp";
		}
		return "register.html";
	}

	public String confirmOtp(int otp, HttpSession session) {
		LocalDateTime createdTime = (LocalDateTime) session.getAttribute("time");
		Integer sessionOtpObj = (Integer) session.getAttribute("otp");
		UserDto userDto = (UserDto) session.getAttribute("userDto");

		if (createdTime == null || sessionOtpObj == null || userDto == null) {
			session.setAttribute("fail", "Session Expired. Please Register Again.");
			return "redirect:/register";
		}

		long seconds = Duration.between(createdTime, LocalDateTime.now()).getSeconds();
		if (seconds > otpTime) {
			session.setAttribute("fail", "OTP Expired! Click Resend OTP to get a new code.");
			return "redirect:/otp";
		}

		if (sessionOtpObj.intValue() == otp) {
			if (userDto.getType() == AccountType.TUTOR) {
				Tutor tutor = new Tutor();
				tutor.setEmail(userDto.getEmail());
				tutor.setMobile(userDto.getMobile());
				tutor.setName(userDto.getName());
				tutor.setPassword(encoder.encode(userDto.getPassword()));

				tutorRepository.save(tutor);
			} else {
				Learner learner = new Learner();
				learner.setEmail(userDto.getEmail());
				learner.setMobile(userDto.getMobile());
				learner.setName(userDto.getName());
				learner.setPassword(encoder.encode(userDto.getPassword()));

				learnerRepository.save(learner);
			}

			session.removeAttribute("otp");
			session.removeAttribute("userDto");
			session.removeAttribute("time");
			session.setAttribute("pass", "Account Created Successfully! Please login.");
			return "redirect:/login";
		} else {
			session.setAttribute("fail", "Invalid OTP. Please try again.");
			return "redirect:/otp";
		}
	}

	void sendEmail(int otp, UserDto userDto) {
		System.out.println("==================================================");
		System.out.println("GENERATED OTP for " + userDto.getEmail() + " (" + userDto.getName() + "): " + otp);
		System.out.println("==================================================");

		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message);
			helper.setTo(userDto.getEmail());
			helper.setFrom(email != null && !email.isEmpty() ? email : "noreply@jnana.com", "Elearning Platform");
			helper.setSubject("Verify OTP - Jnana");

			Context context = new Context();
			context.setVariable("otp", otp);
			context.setVariable("name", userDto.getName());
			context.setVariable("role", userDto.getType() != null ? userDto.getType().name() : "USER");

			String body = templateEngine.process("email-template.html", context);

			helper.setText(body, true);

			mailSender.send(message);
			System.out.println("OTP Email successfully sent to: " + userDto.getEmail());
		} catch (Exception e) {
			System.err.println("Failed to Send OTP Email to " + userDto.getEmail() + " : " + e.getMessage());
		}
	}

	public void removeMessage() {
		RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
		ServletRequestAttributes attributes = (ServletRequestAttributes) requestAttributes;
		HttpServletRequest request = attributes.getRequest();
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.removeAttribute("pass");
			session.removeAttribute("fail");
		}
	}

	public String resendOtp(HttpSession session) {
		UserDto userDto = (UserDto) session.getAttribute("userDto");
		if (userDto == null) {
			session.setAttribute("fail", "Session Expired. Please Register Again.");
			return "redirect:/register";
		}

		int otp = new Random().nextInt(100000, 1000000);
		session.setAttribute("otp", otp);
		session.setAttribute("time", LocalDateTime.now());
		sendEmail(otp, userDto);

		session.setAttribute("pass", "OTP Re-Sent Successfully.");
		return "redirect:/otp";
	}

	// ==================== FORGOT PASSWORD MODULE ====================

	public String processForgotPassword(String mobileInput, HttpSession session) {
		if (mobileInput == null || mobileInput.trim().isEmpty()) {
			session.setAttribute("fail", "Please enter a valid Mobile Number or Email.");
			return "redirect:/forgot-password";
		}

		String trimmed = mobileInput.trim();
		Learner learner = null;
		Tutor tutor = null;

		if (trimmed.contains("@")) {
			learner = learnerRepository.findByEmail(trimmed);
			tutor = tutorRepository.findByEmail(trimmed);
		} else {
			String digitsOnly = trimmed.replaceAll("[^0-9]", "");
			if (!digitsOnly.isEmpty()) {
				if (digitsOnly.length() > 10) {
					digitsOnly = digitsOnly.substring(digitsOnly.length() - 10);
				}
				try {
					Long mobileNumber = Long.parseLong(digitsOnly);
					learner = learnerRepository.findByMobile(mobileNumber);
					tutor = tutorRepository.findByMobile(mobileNumber);
				} catch (NumberFormatException e) {
					// Invalid number format
				}
			}
		}

		if (learner == null && tutor == null) {
			session.setAttribute("fail", "Mobile Number / Email Not Registered!");
			return "redirect:/forgot-password";
		}

		String userEmail = learner != null ? learner.getEmail() : tutor.getEmail();
		String userName = learner != null ? learner.getName() : tutor.getName();
		AccountType accountType = learner != null ? AccountType.LEARNER : AccountType.TUTOR;
		Long registeredMobile = learner != null ? learner.getMobile() : tutor.getMobile();

		int resetOtp = new Random().nextInt(100000, 1000000);
		session.setAttribute("resetOtp", resetOtp);
		session.setAttribute("resetMobile", registeredMobile);
		session.setAttribute("resetAccountType", accountType.name());
		session.setAttribute("resetTime", LocalDateTime.now());

		System.out.println("==================================================");
		System.out.println("FORGOT PASSWORD OTP for " + userEmail + " (" + userName + "): " + resetOtp);
		System.out.println("==================================================");

		UserDto tempDto = new UserDto();
		tempDto.setEmail(userEmail);
		tempDto.setName(userName);
		tempDto.setType(accountType);
		sendEmail(resetOtp, tempDto);

		session.setAttribute("pass", "OTP sent successfully to your registered email (" + userEmail + ").");
		return "redirect:/reset-password-otp";
	}

	public String confirmResetOtp(int otp, HttpSession session) {
		LocalDateTime createdTime = (LocalDateTime) session.getAttribute("resetTime");
		Integer sessionOtpObj = (Integer) session.getAttribute("resetOtp");
		Long mobile = (Long) session.getAttribute("resetMobile");

		if (createdTime == null || sessionOtpObj == null || mobile == null) {
			session.setAttribute("fail", "Session Expired. Please try again.");
			return "redirect:/forgot-password";
		}

		long seconds = Duration.between(createdTime, LocalDateTime.now()).getSeconds();
		if (seconds > otpTime) {
			session.setAttribute("fail", "OTP Expired! Click Resend OTP.");
			return "redirect:/reset-password-otp";
		}

		if (sessionOtpObj.intValue() == otp) {
			session.setAttribute("resetVerified", true);
			session.setAttribute("pass", "OTP Verified Successfully! Enter your new password.");
			return "redirect:/reset-password";
		} else {
			session.setAttribute("fail", "Invalid OTP! Please try again.");
			return "redirect:/reset-password-otp";
		}
	}

	public String resendResetOtp(HttpSession session) {
		Long mobile = (Long) session.getAttribute("resetMobile");
		String accountTypeStr = (String) session.getAttribute("resetAccountType");

		if (mobile == null || accountTypeStr == null) {
			session.setAttribute("fail", "Session Expired. Please try again.");
			return "redirect:/forgot-password";
		}

		int newOtp = new Random().nextInt(100000, 1000000);
		session.setAttribute("resetOtp", newOtp);
		session.setAttribute("resetTime", LocalDateTime.now());

		System.out.println("==================================================");
		System.out.println("RESENT FORGOT PASSWORD OTP for Mobile " + mobile + ": " + newOtp);
		System.out.println("==================================================");

		String userEmail = null;
		String userName = null;
		AccountType accountType = "LEARNER".equals(accountTypeStr) ? AccountType.LEARNER : AccountType.TUTOR;

		if (accountType == AccountType.LEARNER) {
			Learner l = learnerRepository.findByMobile(mobile);
			if (l != null) { userEmail = l.getEmail(); userName = l.getName(); }
		} else {
			Tutor t = tutorRepository.findByMobile(mobile);
			if (t != null) { userEmail = t.getEmail(); userName = t.getName(); }
		}

		if (userEmail != null) {
			UserDto tempDto = new UserDto();
			tempDto.setEmail(userEmail);
			tempDto.setName(userName);
			tempDto.setType(accountType);
			sendEmail(newOtp, tempDto);
		}

		session.setAttribute("pass", "New OTP has been sent successfully.");
		return "redirect:/reset-password-otp";
	}

	public String loadResetPassword(HttpSession session) {
		Boolean verified = (Boolean) session.getAttribute("resetVerified");
		if (verified == null || !verified) {
			session.setAttribute("fail", "Please verify OTP first.");
			return "redirect:/forgot-password";
		}
		return "reset-password.html";
	}

	public String processResetPassword(String password, String confirmPassword, HttpSession session) {
		Boolean verified = (Boolean) session.getAttribute("resetVerified");
		Long mobile = (Long) session.getAttribute("resetMobile");
		String accountTypeStr = (String) session.getAttribute("resetAccountType");

		if (verified == null || !verified || mobile == null || accountTypeStr == null) {
			session.setAttribute("fail", "Session Expired. Please try again.");
			return "redirect:/forgot-password";
		}

		if (!password.equals(confirmPassword)) {
			session.setAttribute("fail", "Password and Confirm Password do not match!");
			return "redirect:/reset-password";
		}

		String passwordPattern = "^.*(?=.{8,})(?=..*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$";
		if (!password.matches(passwordPattern)) {
			session.setAttribute("fail", "Password must contain uppercase, lowercase, number, special character and minimum 8 characters.");
			return "redirect:/reset-password";
		}

		String encodedPassword = encoder.encode(password);

		if ("LEARNER".equals(accountTypeStr)) {
			Learner learner = learnerRepository.findByMobile(mobile);
			if (learner != null) {
				learner.setPassword(encodedPassword);
				learnerRepository.save(learner);
			}
		} else {
			Tutor tutor = tutorRepository.findByMobile(mobile);
			if (tutor != null) {
				tutor.setPassword(encodedPassword);
				tutorRepository.save(tutor);
			}
		}

		// Clear reset attributes from session
		session.removeAttribute("resetOtp");
		session.removeAttribute("resetMobile");
		session.removeAttribute("resetAccountType");
		session.removeAttribute("resetTime");
		session.removeAttribute("resetVerified");

		session.setAttribute("pass", "Password Reset Successful! Please login with your new password.");
		return "redirect:/login";
	}

	// =================================================================

	public String login(String email, String password, HttpSession session) {
		Learner learner = learnerRepository.findByEmail(email);
		Tutor tutor = tutorRepository.findByEmail(email);

		if (learner == null && tutor == null) {
			session.setAttribute("fail", "Invalid Email");
			return "redirect:/login";
		} else {
			if (tutor != null) {
				if (encoder.matches(password, tutor.getPassword())) {
					session.setAttribute("pass", "Login Success as Tutor");
					session.setAttribute("tutor", tutor);
					return "redirect:/tutor/home";
				} else {
					session.setAttribute("fail", "Invalid Password");
					return "redirect:/login";
				}
			} else {
				if (encoder.matches(password, learner.getPassword())) {
					session.setAttribute("pass", "Login Success as Learner");
					session.setAttribute("learner", learner);
					return "redirect:/learner/home";
				} else {
					session.setAttribute("fail", "Invalid Password");
					return "redirect:/login";
				}
			}
		}
	}

	public String logout(HttpSession session) {
		session.removeAttribute("learner");
		session.removeAttribute("tutor");
		session.setAttribute("fail", "Logout Success");
		return "redirect:/";
	}

}