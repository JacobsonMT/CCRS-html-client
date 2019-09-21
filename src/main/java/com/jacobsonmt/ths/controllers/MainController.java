package com.jacobsonmt.ths.controllers;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Log4j2
@Controller
public class MainController {

    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/documentation")
    public String documentation() {
        return "documentation";
    }

    @GetMapping("/")
    public String test() {
        return "docs/swagger-ui.html";
    }

    @GetMapping("/swagger-ui.html")
    public String swagger() {
        return "docs/swagger-ui.html";
    }
}
