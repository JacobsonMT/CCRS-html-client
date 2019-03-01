package com.jacobsonmt.ths.services;

import com.jacobsonmt.ths.model.THSJob;
import com.jacobsonmt.ths.settings.ApplicationSettings;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Log4j2
@Service
public class CCRSService {

    @Autowired
    private ApplicationSettings applicationSettings;

    public ResponseEntity<JobSubmissionResponse> submitJob( String userId, String label, String fasta, String email, boolean hidden) {
        RestTemplate restTemplate = new RestTemplate();
        JobSubmission jobSubmission = new JobSubmission( userId, label, fasta, email, hidden );
        HttpEntity<JobSubmission> request =
                new HttpEntity<>( jobSubmission, createHeaders() );
        log.info( jobSubmission );
        return restTemplate.postForEntity( applicationSettings.getProcessServerURI() + "/job/submit", request, JobSubmissionResponse.class );
    }

    public THSJob getJob(String jobId) {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity entity = new HttpEntity(createHeaders());
        // getForObject cannot specify headers so we use exchange

        // TODO: this might be vulnerable to attack, either validate jobId or do some sort of escaping
        log.info( "Client: (" + applicationSettings.getClientId() + "), Job: (" + jobId + ")" );
        ResponseEntity<THSJob> response
                = restTemplate.exchange(applicationSettings.getProcessServerURI() + "/job/" + jobId, HttpMethod.GET, entity, THSJob.class);

        return response.getBody();

    }

    public List<THSJob> getJobsForUser(String userId) {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity entity = new HttpEntity(createHeaders());
        // getForObject cannot specify headers so we use exchange

        // TODO: this might be vulnerable to attack, either validate jobId or do some sort of escaping
        log.info( "Client: (" + applicationSettings.getClientId() + "), User: (" + userId + ")" );
        ResponseEntity<List<THSJob>> response
                = restTemplate.exchange(applicationSettings.getProcessServerURI() + "/queue/client/" + applicationSettings.getClientId() + "/user/" + userId,
                HttpMethod.GET, entity, new ParameterizedTypeReference<List<THSJob>>(){});

        return response.getBody();

    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set( "client", applicationSettings.getClientId() );
        headers.set( "auth_token", applicationSettings.getClientToken() );
        return headers;
    }


    @ToString
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    static class JobSubmission {
        private String userId;
        private String label;
        private String fastaContent;
        private String email;
        private boolean hidden;
    }

    @ToString
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class JobSubmissionResponse {
        private String message;
        private String jobId;
    }
}
