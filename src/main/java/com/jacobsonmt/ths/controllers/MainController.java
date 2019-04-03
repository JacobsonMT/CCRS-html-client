package com.jacobsonmt.ths.controllers;

import com.jacobsonmt.ths.model.ContactForm;
import com.jacobsonmt.ths.services.CCRSService;
import com.jacobsonmt.ths.services.EmailService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.context.request.RequestContextHolder;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Log4j2
@Controller
public class MainController {

    @Autowired
    private CCRSService ccrsService;

    @Autowired
    private EmailService emailService;


    @GetMapping("/")
    public String index( Model model) {
        String userId = RequestContextHolder.currentRequestAttributes().getSessionId();
        model.addAttribute("jobs", ccrsService.getJobsForUser( userId ));
        return "index";
    }

    @GetMapping("/job-table")
    public String getJobTable( Model model) {
        String userId = RequestContextHolder.currentRequestAttributes().getSessionId();
        model.addAttribute("jobs", ccrsService.getJobsForUser( userId ));

        return "index :: #job-table";
    }

    @GetMapping("/queue")
    public String queue( Model model) {
        String userId = RequestContextHolder.currentRequestAttributes().getSessionId();
        model.addAttribute("jobs", ccrsService.getJobsForUser( userId ));

        return "queue";
    }

    @GetMapping("/documentation")
    public String documentation( Model model) {
        return "documentation";
    }

    @GetMapping("/faq")
    public String faq( Model model) {
        return "faq";
    }

    @GetMapping("/contact")
    public String contact( Model model) {
        model.addAttribute("contactForm", new ContactForm());
        return "contact";
    }

    @PostMapping("/contact")
    public String contact( Model model,
                           HttpServletRequest request,
                           @Valid ContactForm contactForm,
                           BindingResult bindingResult ) {
        if (bindingResult.hasErrors()) {
            return "contact";
        }

        log.info( contactForm );
        try {
            emailService.sendSupportMessage( contactForm.getMessage(), contactForm.getName(), contactForm.getEmail(), request, contactForm.getAttachment() );
            model.addAttribute( "message", "Sent. We will get back to you shortly." );
            model.addAttribute( "success", true );
        } catch ( MessagingException | MailSendException e) {
            log.error(e);
            model.addAttribute( "message", "There was a problem sending the support request. Please try again later." );
            model.addAttribute( "success", false );
        }

        return "contact";
    }

    @GetMapping("/maintenance")
    public String contact() {
        return "maintenance";
    }
}
