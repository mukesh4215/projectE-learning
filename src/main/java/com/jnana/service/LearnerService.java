package com.jnana.service;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import com.jnana.model.Course;
import com.jnana.model.EnrolledCourse;
import com.jnana.model.EnrolledSection;
import com.jnana.model.Learner;
import com.jnana.model.QuizQuestion;
import com.jnana.model.Section;
import com.jnana.repository.CourseRepository;
import com.jnana.repository.EnrolledCourseRepository;
import com.jnana.repository.EnrolledSectionRepository;
import com.jnana.repository.LearnerRepository;
import com.jnana.repository.SectionRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import jakarta.servlet.http.HttpSession;

@Service
public class LearnerService {
	@Autowired
	CourseRepository courseRepository;
	
	@Autowired
	EnrolledCourseRepository enrolledCourseRepository;
	
	@Autowired
	LearnerRepository learnerRepository;
	
	@Autowired
	SectionRepository sectionRepository;
	
	@Autowired
	EnrolledSectionRepository enrolledSectionRepository;

	@Value("${razor-pay.api.key}")
	String key;
	@Value("${razor-pay.api.secret}")
	String secret;

	public String loadHome(HttpSession session) {
		if (session.getAttribute("learner") != null) {
			return "leaner-home.html";
		} else {
			session.setAttribute("fail", "Invalid Session, Login First");
			return "redirect:/login";
		}
	}

	public String viewCourses(HttpSession session, Model model) {
		if (session.getAttribute("learner") != null) {
			List<Course> courses = courseRepository.findByPublishedTrue();
			if (courses.isEmpty()) {
				session.setAttribute("fail", "No Courses Available");
				return "redirect:/learner/home";
			} else {
				model.addAttribute("courses", courses);
				return "available-courses.html";
			}
		} else {
			session.setAttribute("fail", "Invalid Session, Login First");
			return "redirect:/login";
		}
	}

	public String enrollCourse(HttpSession session, Long id, Model model) {
		if (session.getAttribute("learner") != null) {
			Learner learner = (Learner) session.getAttribute("learner");
			Course course = courseRepository.findById(id).get();

			if (course.isPaid()) {
				double amount = 199;
				try {
					RazorpayClient client = new RazorpayClient(key, secret);

					JSONObject object = new JSONObject();
					object.put("amount", amount * 100);
					object.put("currency", "INR");

					Order order = client.orders.create(object);
					String orderId = order.get("id");

					model.addAttribute("orderId", orderId);
					model.addAttribute("amount", amount * 100);
					model.addAttribute("currency", "INR");
					model.addAttribute("leaner", learner);
					model.addAttribute("key", key);

					return "payment.html";

				} catch (RazorpayException e) {
					e.printStackTrace();
					session.setAttribute("fail", "Something Went Wrong");
					return "redirect:/learner/home";
				}

			} else {

				List<Section> sections = sectionRepository.findByCourse(course);
				List<EnrolledSection> enrolledSections = new ArrayList<EnrolledSection>();
				for (Section section : sections) {
					EnrolledSection enrolledSection = new EnrolledSection();
					enrolledSection.setSection(section);
					enrolledSections.add(enrolledSection);
				}

				EnrolledCourse enrolledCourse = new EnrolledCourse();
				enrolledCourse.setCourse(course);
				enrolledCourse.setEnrolledSections(enrolledSections);

				learner.getEnrolledCourses().add(enrolledCourse);

				learnerRepository.save(learner);

				session.setAttribute("pass", "Courses Enrolled Success, Thanks " + learner.getName());
				session.setAttribute("learner", learnerRepository.findById(learner.getId()).get());
				return "redirect:/learner/home";
			}

		} else {
			session.setAttribute("fail", "Invalid Session, Login First");
			return "redirect:/login";
		}
	}

	public String viewEnrolledCourses(HttpSession session, Model model) {
		if (session.getAttribute("learner") != null) {
			Learner learner = (Learner) session.getAttribute("learner");

			List<EnrolledCourse> enrolledCourses = learner.getEnrolledCourses();
			if (enrolledCourses.isEmpty()) {
				session.setAttribute("fail", "Not Enrolled for Any of the Courses");
				return "redirect:/learner/home";
			} else {
				model.addAttribute("enrolledCourses", enrolledCourses);
				return "view-enrolled-courses.html";
			}
		} else {
			session.setAttribute("fail", "Invalid Session, Login First");
			return "redirect:/login";
		}
	}

	public String viewEnrolledSections(HttpSession session, Long id, Model model) {
		if (session.getAttribute("learner") != null) {
			EnrolledCourse enrolledCourse = enrolledCourseRepository.findById(id).get();
			List<EnrolledSection> enrolledSections = enrolledCourse.getEnrolledSections();

			model.addAttribute("enrolledSections", enrolledSections);
			return "view-enrolled-sections.html";
		} else {
			session.setAttribute("fail", "Invalid Session, Login First");
			return "redirect:/login";
		}
	}
	
	public String viewVideo(HttpSession session, Long id, Model model) {
		if (session.getAttribute("learner") != null) {

			EnrolledSection section = enrolledSectionRepository.findById(id).get();
			section.setSectionCompleted(true);

			enrolledSectionRepository.save(section);

			String videoUrl = section.getSection().getVideoUrl();
			model.addAttribute("link", videoUrl);
			EnrolledCourse course = enrolledCourseRepository.findByEnrolledSections(section);
			model.addAttribute("id", course.getId());
			return "play-video.html";
		} else {
			session.setAttribute("fail", "Invalid Session, Login First");
			return "redirect:/login";
		}
	}

	public String loadSectionQuiz(Long id, HttpSession session, Model model) {
		if (session.getAttribute("learner") != null) {

			EnrolledSection section = enrolledSectionRepository.findById(id).get();

			if (!section.isSectionCompleted()) {
				EnrolledCourse course = enrolledCourseRepository.findByEnrolledSections(section);
				session.setAttribute("fail", "First Complete the Section to Take Quiz");
				return "redirect:/learner/view-enrolled-sections/" + course.getId();
			}
			List<QuizQuestion> questions = section.getSection().getQuizQuestions();
			model.addAttribute("questions", questions);
			model.addAttribute("id", id);

			return "section-quiz.html";
		} else {
			session.setAttribute("fail", "Invalid Session, Login First");
			return "redirect:/login";
		}
	}

}
