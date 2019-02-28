package com.jacobsonmt.ths.services;

import com.jacobsonmt.ths.model.THSJob;
import lombok.*;
import lombok.extern.log4j.Log4j2;
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

    private static final String CCRS_API_URI = "http://localhost:8080/api";
    private static final String CLIENT_ID = "client1"; //TODO: Move to properties
    private static final String CLIENT_TOKEN = "client1token"; //TODO: Move to properties


    public ResponseEntity<JobSubmissionResponse> submitJob( String userId, String label, String fasta, String email, boolean hidden) {
        RestTemplate restTemplate = new RestTemplate();
        JobSubmission jobSubmission = new JobSubmission( userId, label, fasta, email, hidden );
        HttpEntity<JobSubmission> request =
                new HttpEntity<>( jobSubmission, createHeaders() );
        log.info( jobSubmission );
        return restTemplate.postForEntity( CCRS_API_URI + "/job/submit", request, JobSubmissionResponse.class );
    }

    public THSJob getJob(String jobId) {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity entity = new HttpEntity(createHeaders());
        // getForObject cannot specify headers so we use exchange

        // TODO: this might be vulnerable to attack, either validate jobId or do some sort of escaping
        log.info( "Client: (" + CLIENT_ID + "), Job: (" + jobId + ")" );
        ResponseEntity<THSJob> response
                = restTemplate.exchange(CCRS_API_URI + "/job/" + jobId, HttpMethod.GET, entity, THSJob.class);

        return response.getBody();

    }

    public List<THSJob> getJobsForUser(String userId) {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity entity = new HttpEntity(createHeaders());
        // getForObject cannot specify headers so we use exchange

        // TODO: this might be vulnerable to attack, either validate jobId or do some sort of escaping
        log.info( "Client: (" + CLIENT_ID + "), User: (" + userId + ")" );
        ResponseEntity<List<THSJob>> response
                = restTemplate.exchange(CCRS_API_URI + "/queue/client/" + CLIENT_ID + "/user/" + userId,
                HttpMethod.GET, entity, new ParameterizedTypeReference<List<THSJob>>(){});

        return response.getBody();

    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set( "client", CLIENT_ID );
        headers.set( "auth_token", CLIENT_TOKEN );
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
