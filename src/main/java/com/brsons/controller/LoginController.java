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
            // Store logged-in user in session
            session.setAttribute("user", user);
            
            // Debug logging
            System.out.println("=== LOGIN SUCCESSFUL ===");
            System.out.println("User ID: " + user.getId());
            System.out.println("User Name: " + user.getName());
            System.out.println("User Email: " + user.getEmail());
            System.out.println("User Phone: " + user.getPhone());
            System.out.println("Session ID: " + session.getId());
            System.out.println("User stored in session: " + (session.getAttribute("user") != null ? "YES" : "NO"));
            
            // Redirect to home page
            return "redirect:/";
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
        
        // Validate GSTIN format if provided
        if (user.getGstin() != null && !user.getGstin().trim().isEmpty()) {
            String gstinPattern = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}[Z]{1}[0-9A-Z]{1}$";
            if (!user.getGstin().matches(gstinPattern)) {
                model.addAttribute("gstinError", "Invalid GSTIN format. Please enter a valid 15-character GSTIN.");
                return "signup";
            }
        }
        
        // Set default values
        if (user.getType() == null) {
            user.setType("Retail");
        }
        if (user.getStatus() == null) {
            user.setStatus("ACTIVE");
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
