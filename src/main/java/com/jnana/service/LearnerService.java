package com.jnana.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional; // Import Optional

import org.json.JSONObject;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import com.cloudinary.Cloudinary;
import com.jnana.model.Certificate;
import com.jnana.model.Course;
import com.jnana.model.EnrolledCourse;
import com.jnana.model.EnrolledSection;
import com.jnana.model.Learner;
import com.jnana.model.QuizQuestion;
import com.jnana.model.Section;
import com.jnana.repository.CertificateRepository;
import com.jnana.repository.CourseRepository;
import com.jnana.repository.EnrolledCourseRepository;
import com.jnana.repository.EnrolledSectionRepository;
import com.jnana.repository.LearnerRepository;
import com.jnana.repository.QuizQuestionRepository;
import com.jnana.repository.SectionRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

import jakarta.servlet.http.HttpSession;

@Service
public class LearnerService {

    private final PasswordEncoder encoder;

    private final Cloudinary cloudinary;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    LearnerRepository learnerRepository;

    @Autowired
    CertificateRepository certificateRepository;

    @Autowired
    ChatClient chatClient;

    @Autowired
    QuizQuestionRepository questionRepository;

    @Autowired
    EnrolledSectionRepository enrolledSectionRepository;

    @Autowired
    SectionRepository sectionRepository;

    @Autowired
    EnrolledCourseRepository enrolledCourseRepository;

    @Value("${razor-pay.api.key}")
    String key;
    @Value("${razor-pay.api.secret}")
    String secret;

    LearnerService(Cloudinary cloudinary, PasswordEncoder encoder) {
        this.cloudinary = cloudinary;
        this.encoder = encoder;
    }

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
            Course course = courseRepository.findById(id).get(); // Consider handling Optional here too

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
                    model.addAttribute("course", course);
                    model.addAttribute("amount", amount * 100);
                    model.addAttribute("currency", "INR");
                    model.addAttribute("leaner", learner);
                    model.addAttribute("key", key);
                    model.addAttribute("path", "/learner/enroll-paidcourse/" + course.getId());

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
                session.setAttribute("learner", learnerRepository.findById(learner.getId()).get()); // Consider handling Optional here
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

    // CORRECTED METHOD
    public String viewEnrolledSections(HttpSession session, Long id, Model model) {
        if (session.getAttribute("learner") != null) {
            // Use Optional to safely retrieve the EnrolledCourse
            Optional<EnrolledCourse> optionalEnrolledCourse = enrolledCourseRepository.findById(id);

            if (optionalEnrolledCourse.isPresent()) {
                EnrolledCourse enrolledCourse = optionalEnrolledCourse.get();
                List<EnrolledSection> enrolledSections = enrolledCourse.getEnrolledSections();

                model.addAttribute("enrolledSections", enrolledSections);
                return "view-enrolled-sections.html";
            } else {
                // If the EnrolledCourse is not found, handle it gracefully
                session.setAttribute("fail", "Enrolled Course not found.");
                return "redirect:/learner/enrolled-courses"; // Redirect to enrolled courses list or home
            }
        } else {
            session.setAttribute("fail", "Invalid Session, Login First");
            return "redirect:/login";
        }
    }

    public String viewVideo(HttpSession session, Long id, Model model) {
        if (session.getAttribute("learner") != null) {

            // Consider handling Optional for enrolledSectionRepository.findById(id) as well
            Optional<EnrolledSection> optionalSection = enrolledSectionRepository.findById(id);
            if (!optionalSection.isPresent()) {
                session.setAttribute("fail", "Section not found.");
                return "redirect:/learner/home"; // Or redirect to a more appropriate page
            }
            EnrolledSection section = optionalSection.get();
            section.setSectionCompleted(true);

            enrolledSectionRepository.save(section);

            String videoUrl = section.getSection().getVideoUrl();
            model.addAttribute("link", videoUrl);
            EnrolledCourse course = enrolledCourseRepository.findByEnrolledSections(section); // This findByEnrolledSections might also return Optional depending on its definition
            model.addAttribute("id", course.getId());
            return "play-video.html";
        } else {
            session.setAttribute("fail", "Invalid Session, Login First");
            return "redirect:/login";
        }
    }

    public String loadSectionQuiz(Long id, HttpSession session, Model model) {
        if (session.getAttribute("learner") != null) {

            // Consider handling Optional for enrolledSectionRepository.findById(id) as well
            Optional<EnrolledSection> optionalSection = enrolledSectionRepository.findById(id);
            if (!optionalSection.isPresent()) {
                session.setAttribute("fail", "Section for quiz not found.");
                return "redirect:/learner/home";
            }
            EnrolledSection section = optionalSection.get();

            if (!section.isSectionCompleted()) {
                EnrolledCourse course = enrolledCourseRepository.findByEnrolledSections(section); // Again, consider Optional
                session.setAttribute("fail", "First Complete the Section to Take Quiz");
                return "redirect:/learner/view-enrolled-sections/" + course.getId();
            }
            List<QuizQuestion> questions = section.getSection().getQuizQuestions();
            model.addAttribute("questions", questions);
            model.addAttribute("section", section);
            model.addAttribute("id", id);

            return "section-quiz.html";
        } else {
            session.setAttribute("fail", "Invalid Session, Login First");
            return "redirect:/login";
        }
    }

    public String submitQuiz(Long id, HttpSession session, Map<String, String> quiz) {
        if (session.getAttribute("learner") != null) {
            // Consider handling Optional for enrolledSectionRepository.findById(id)
            Optional<EnrolledSection> optionalSection = enrolledSectionRepository.findById(id);
            if (!optionalSection.isPresent()) {
                session.setAttribute("fail", "Section for quiz submission not found.");
                return "redirect:/learner/home";
            }
            EnrolledSection section = optionalSection.get();

            String prompt = "";
            for (String questionId : quiz.keySet()) {
                // Consider handling Optional for questionRepository.findById(Long.parseLong(questionId))
                Optional<QuizQuestion> optionalQuestion = questionRepository.findById(Long.parseLong(questionId));
                if (optionalQuestion.isPresent()) {
                    String question = optionalQuestion.get().getQuestion();
                    String answer = quiz.get(questionId);
                    prompt += ". question: " + question + ". answer: " + answer;
                }
            }
            prompt += "Evaluate the following quiz strictly. For each question, consider the given answer against the correct answer. Calculate and return ONLY a numerical score from 0-100 based on the accuracy of responses. Return just the number without any additional text.\n\n";
            String answer = chatClient.prompt(prompt).call().content();
            int score = Integer.parseInt(answer);
            if (score >= 75) {
                section.setSectionQuizCompleted(true);
                enrolledSectionRepository.save(section);
                session.setAttribute("pass", "Quiz Cleared Success, Congratulations!");
            } else {
                session.setAttribute("fail", "You failed to clear the quiz, as you got " + score + " marks. Try again");
            }
            EnrolledCourse course = enrolledCourseRepository.findByEnrolledSections(section); // Consider Optional here
            session.setAttribute("learner", learnerRepository.findById(((Learner) session.getAttribute("learner")).getId()).get()); // Consider Optional here
            return "redirect:/learner/view-enrolled-sections/" + course.getId();
        } else {
            session.setAttribute("fail", "Invalid Session, Login First");
            return "redirect:/login";
        }
    }

    public String enrollPaidCourse(HttpSession session, Long id, Model model) {
        if (session.getAttribute("learner") != null) {
            Learner learner = (Learner) session.getAttribute("learner");
            Course course = courseRepository.findById(id).get(); // Consider handling Optional here
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
            session.setAttribute("learner", learnerRepository.findById(learner.getId()).get()); // Consider handling Optional here
            return "redirect:/learner/home";

        } else {
            session.setAttribute("fail", "Invalid Session, Login First");
            return "redirect:/login";
        }
    }

    public String loadCourseQuiz(Long id, HttpSession session, Model model) {
        if (session.getAttribute("learner") != null) {
            // Consider handling Optional for enrolledCourseRepository.findById(id)
            Optional<EnrolledCourse> optionalEnrolledCourse = enrolledCourseRepository.findById(id);
            if (!optionalEnrolledCourse.isPresent()) {
                session.setAttribute("fail", "Enrolled Course for quiz not found.");
                return "redirect:/learner/enrolled-courses";
            }
            EnrolledCourse enrolledCourse = optionalEnrolledCourse.get();

            boolean completed = true;

            for (EnrolledSection section : enrolledCourse.getEnrolledSections()) {
                if (!section.isSectionQuizCompleted()) {
                    completed = false;
                    break;
                }
            }
            if (completed) {
                enrolledCourse.setCourseCompleted(true);
                enrolledCourseRepository.save(enrolledCourse);
            }

            if (!enrolledCourse.isCourseCompleted()) {
                session.setAttribute("fail", "First Complete all Sections to Take Quiz");
                return "redirect:/learner/view-enrolled-sections/" + enrolledCourse.getId();
            } else {
                List<QuizQuestion> questions = enrolledCourse.getCourse().getQuizQuestions();
                if (questions.isEmpty()) {
                    session.setAttribute("fail", "No Quiz Available for this Course");
                    return "redirect:/learner/view-enrolled-courses";
                }
                model.addAttribute("questions", questions);
                model.addAttribute("id", id);
                model.addAttribute("course", enrolledCourse.getCourse());
                return "course-quiz.html";
            }
        } else {
            session.setAttribute("fail", "Invalid Session, Login First");
            return "redirect:/login";
        }
    }

    public String submitCourseQuiz(Long id, HttpSession session, Map<String, String> quiz) {
        if (session.getAttribute("learner") != null) {
            // Consider handling Optional for enrolledCourseRepository.findById(id)
            Optional<EnrolledCourse> optionalEnrolledCourse = enrolledCourseRepository.findById(id);
            if (!optionalEnrolledCourse.isPresent()) {
                session.setAttribute("fail", "Enrolled Course for quiz submission not found.");
                return "redirect:/learner/enrolled-courses";
            }
            EnrolledCourse enrolledCourse = optionalEnrolledCourse.get();

            String prompt = "";
            for (String questionId : quiz.keySet()) {
                // Consider handling Optional for questionRepository.findById(Long.parseLong(questionId))
                Optional<QuizQuestion> optionalQuestion = questionRepository.findById(Long.parseLong(questionId));
                if (optionalQuestion.isPresent()) {
                    String question = optionalQuestion.get().getQuestion();
                    String answer = quiz.get(questionId);
                    prompt += ". question: " + question + ". answer: " + answer;
                }
            }
            prompt += "Evaluate the following quiz strictly. For each question, consider the given answer against the correct answer. Calculate and return ONLY a numerical score from 0-100 based on the accuracy of responses. Return just the number without any additional text.\n\n";
            String answer = chatClient.prompt(prompt).call().content();
            int score = Integer.parseInt(answer);
            if (score >= 80) {
                enrolledCourse.setFinalQuizCompleted(true);
                enrolledCourse.setCompletionDate(LocalDate.now());
                enrolledCourseRepository.save(enrolledCourse);
                session.setAttribute("pass", "Quiz Cleared Success, Congratulations!");
                Certificate certificate = new Certificate();
                certificate.setLearner((Learner) session.getAttribute("learner"));
                certificate.setCourse(enrolledCourse.getCourse());
                certificateRepository.save(certificate);
            } else {
                session.setAttribute("fail", "You failed to clear the quiz, as you got " + score + " marks. Try again");
            }
            session.setAttribute("learner", learnerRepository.findById(((Learner) session.getAttribute("learner")).getId()).get()); // Consider Optional here
            return "redirect:/learner/enrolled-courses";
        } else {
            session.setAttribute("fail", "Invalid Session, Login First");
            return "redirect:/login";
        }
    }

    public String viewCertificate(Long id, HttpSession session, Model model) {
        if (session.getAttribute("learner") != null) {
            // Consider handling Optional for enrolledCourseRepository.findById(id)
            Optional<EnrolledCourse> optionalEnrolledCourse = enrolledCourseRepository.findById(id);
            if (!optionalEnrolledCourse.isPresent()) {
                session.setAttribute("fail", "Enrolled Course for certificate not found.");
                return "redirect:/learner/enrolled-courses";
            }
            EnrolledCourse enrolledCourse = optionalEnrolledCourse.get();

            if (!enrolledCourse.isFinalQuizCompleted()) {
                session.setAttribute("fail", "First Complete the Final Quiz to View Certificate");
                return "redirect:/learner/enrolled-courses";
            } else {
                Certificate certificate = certificateRepository.findByLearnerAndCourse((Learner) session.getAttribute("learner"), enrolledCourse.getCourse()); // Ensure this method handles null/Optional if not found
                model.addAttribute("certificate", certificate);
                return "view-certificate.html";
            }
        } else {
            session.setAttribute("fail", "Invalid Session, Login First");
            return "redirect:/login";
        }

    }
}