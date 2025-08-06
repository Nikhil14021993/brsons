package com.brsons.controller;

import com.brsons.model.User;
import com.brsons.repository.UserRepository;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {

    private final UserRepository userRepository;

    public LoginController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/login")
    public String loginPage(Model model, HttpSession session, @RequestParam(required = false) String success) {
        model.addAttribute("successMessage", success);
        model.addAttribute("loggedInUser", session.getAttribute("user"));
        return "login";
    }

    @PostMapping("/login")
    public String loginSubmit(@RequestParam String emailOrPhone,
                              @RequestParam String password,
                              HttpSession session,
                              Model model) {

        User user = userRepository.findByEmailOrPhone(emailOrPhone, emailOrPhone).orElse(null);

        if (user != null && user.getPassword().equals(password)) {
            session.setAttribute("user", user); // Store logged-in user in session
            return "redirect:/?userId=" + user.getPhone();
        } else {
            model.addAttribute("error", "Invalid credentials");
            return "login";
        }
    }

    @GetMapping("/signup")
    public String signupPage(Model model) {
        model.addAttribute("user", new User());
        return "signup";
    }

    @PostMapping("/signup")
    public String signupSubmit(@Valid @ModelAttribute("user") User user,
                                BindingResult result,
                                @RequestParam String confirmPassword,
                                Model model) {
        if (result.hasErrors()) {
            return "signup";
        }
        if (!user.getPassword().equals(confirmPassword)) {
            model.addAttribute("passwordError", "Passwords do not match");
            return "signup";
        }
        userRepository.save(user);
        return "redirect:/login?success=Account+successfully+created";
    }
    
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // Clear session
        return "redirect:/";
    }
}
