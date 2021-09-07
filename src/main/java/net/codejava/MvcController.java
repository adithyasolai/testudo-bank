package net.codejava;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class MvcController {
	
	@GetMapping("/")
	public String showForm(Model model) {
		User user = new User();
		model.addAttribute("user", user);
		
		return "register_form";
	}
	
	@PostMapping("/login")
	public String submitForm(@ModelAttribute("user") User user) {
		System.out.println(user);
		return "register_success";
	}

	@GetMapping("/account")
	public String show(Model model) {
		User user = new User();
		model.addAttribute("user", user);

		
		return "register_form";
	}
	
	@PostMapping("/account")
	public String submit(@ModelAttribute("user") User user) {
		System.out.println(user);
		return "register_success";
	}
}
