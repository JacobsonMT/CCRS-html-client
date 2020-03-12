package com.jacobsonmt.ths.controllers;

import com.jacobsonmt.ths.model.ContactForm;
import com.jacobsonmt.ths.model.THSJob;
import com.jacobsonmt.ths.services.CCRSService;
import com.jacobsonmt.ths.services.EmailService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSendException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;

import javax.mail.MessagingException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;

@Log4j2
@Controller
public class MainController {

    @Autowired
    private CCRSService ccrsService;

    @Autowired
    private EmailService emailService;


    @GetMapping("/")
    public String index( Model model, HttpServletResponse response, @RequestParam(value = "session", required = false) String session ) {
        if (session == null) {
            session = RequestContextHolder.currentRequestAttributes().getSessionId();
        } else {
            Cookie cookie = new Cookie("JSESSIONID", session);
            response.addCookie( cookie );
        }
        model.addAttribute("jobs", ccrsService.getJobsForUser( session ).getBody());
        model.addAttribute("sessionId", session);
        return "index";
    }

    @GetMapping("/job-table")
    public String getJobTable( Model model) {
        String userId = RequestContextHolder.currentRequestAttributes().getSessionId();
        model.addAttribute("jobs", ccrsService.getJobsForUser( userId ).getBody());

        return "index :: #job-table";
    }

    @GetMapping("/queue")
    public String queue( Model model) {
        String userId = RequestContextHolder.currentRequestAttributes().getSessionId();
        model.addAttribute("jobs", ccrsService.getJobsForUser( userId ).getBody());

        return "queue";
    }

    @GetMapping("/pending")
    public ResponseEntity<Long> pendingCount() {
        String userId = RequestContextHolder.currentRequestAttributes().getSessionId();
        List<THSJob> jobs = ccrsService.getJobsForUser( userId ).getBody();
        if (jobs == null) {
            return ResponseEntity.status( 500 ).body( 0L );
        }
        return ResponseEntity.ok().body(jobs.stream().filter( j -> j.getResult() == null ).count());
    }

    @GetMapping("/documentation")
    public String documentation( Model model) {
        return "documentation";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
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
