package com.jnana.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.jnana.mydto.UserDto;
import com.jnana.service.GeneralService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class GeneralController {

	@Autowired
	GeneralService generalService;

	@GetMapping("/")
	public String loadHome() {
		return "home.html";
	}

	@GetMapping("/register")
	public String loadRegister(UserDto userDto, Model model) {
		return generalService.loadRegister(userDto, model);
	}

	@PostMapping("/register")
	public String register(@ModelAttribute @Valid UserDto userDto, BindingResult result, HttpSession session) {
		return generalService.register(userDto, result, session);
	}

	@GetMapping("/otp")
	public String loadOtp() {
		return "otp.html";
	}

	@PostMapping("/submit-otp")
	public String submitOtp(@RequestParam int otp, HttpSession session) {
		return generalService.confirmOtp(otp, session);
	}

	@GetMapping("/resend-otp")
	public String resendOtp(HttpSession session) {
		return generalService.resendOtp(session);
	}

	@GetMapping("/login")
	public String loadLogin() {
		return "login.html";
	}

	@PostMapping("/login")
	public String login(@RequestParam String email, @RequestParam String password, HttpSession session) {
		return generalService.login(email, password, session);
	}

	@GetMapping("/logout")
	public String logout(HttpSession session) {
		return generalService.logout(session);
	}

	// ==================== FORGOT PASSWORD ENDPOINTS ====================

	@GetMapping("/forgot-password")
	public String loadForgotPassword() {
		return "forgot-password.html";
	}

	@PostMapping("/forgot-password")
	public String processForgotPassword(@RequestParam long mobile, HttpSession session) {
		return generalService.processForgotPassword(mobile, session);
	}

	@GetMapping("/reset-password-otp")
	public String loadResetPasswordOtp() {
		return "reset-password-otp.html";
	}

	@PostMapping("/submit-reset-otp")
	public String submitResetOtp(@RequestParam int otp, HttpSession session) {
		return generalService.confirmResetOtp(otp, session);
	}

	@GetMapping("/resend-reset-otp")
	public String resendResetOtp(HttpSession session) {
		return generalService.resendResetOtp(session);
	}

	@GetMapping("/reset-password")
	public String loadResetPassword(HttpSession session) {
		return generalService.loadResetPassword(session);
	}

	@PostMapping("/reset-password")
	public String processResetPassword(@RequestParam String password, @RequestParam String confirmPassword, HttpSession session) {
		return generalService.processResetPassword(password, confirmPassword, session);
	}

}