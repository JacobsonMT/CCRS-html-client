package com.jacobsonmt.ths.services;

import com.jacobsonmt.ths.model.THSJob;
import com.jacobsonmt.ths.settings.ApplicationSettings;
import com.jacobsonmt.ths.settings.SiteSettings;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

@Log4j2
@Service
public class CCRSService {

    @Autowired
    private ApplicationSettings applicationSettings;

    @Autowired
    private SiteSettings siteSettings;

    private Integer completionCount = 0;

    public ResponseEntity<JobSubmissionResponse> submitJob( String userId, String label, String fasta, String email, boolean hidden) {
        RestTemplate restTemplate = new RestTemplateBuilder().errorHandler(new NoOpResponseErrorHandler()).build();
        JobSubmission jobSubmission = new JobSubmission( userId, label, fasta, email, hidden,
                siteSettings.getFullUrl() + "job/",
                applicationSettings.isEmailOnJobSubmitted(),
                applicationSettings.isEmailOnJobStart(),
                applicationSettings.isEmailOnJobComplete());
        HttpEntity<JobSubmission> request =
                new HttpEntity<>( jobSubmission, createHeaders() );
        return restTemplate.postForEntity( applicationSettings.getProcessServerURI() + "/job/submit", request, JobSubmissionResponse.class );
    }

    public THSJob getJob(String jobId) {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity entity = new HttpEntity(createHeaders());
        // getForObject cannot specify headers so we use exchange

        log.info( "Client: (" + applicationSettings.getClientId() + "), Job: (" + jobId + ")" );
        ResponseEntity<THSJob> response
                = restTemplate.exchange(
                        applicationSettings.getProcessServerURI() + "/job/{jobId}",
                HttpMethod.GET,
                entity,
                THSJob.class,
                jobId
        );

        // Add parsed version of result csv to aid in creation of front-end visualisations
        THSJob job = response.getBody();

        if (job != null && job.getResult() != null) {
            job.migrateCSVResultToSequence();
        }

        return response.getBody();

    }

    public String deleteJob(String jobId) {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity entity = new HttpEntity(createHeaders());
        // getForObject cannot specify headers so we use exchange

        log.info( "Client: (" + applicationSettings.getClientId() + "), Job: (" + jobId + ")" );
        ResponseEntity<String> response
                = restTemplate.exchange(applicationSettings.getProcessServerURI() + "/job/{jobId}/delete",
                HttpMethod.GET,
                entity,
                String.class,
                jobId
        );

        return response.getBody();

    }

    public synchronized boolean hasChanged() {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity entity = new HttpEntity( createHeaders() );
        // getForObject cannot specify headers so we use exchange

        try {
            ResponseEntity<Integer> response
                    = restTemplate.exchange( applicationSettings.getProcessServerURI() + "/queue/client/{clientId}/complete",
                    HttpMethod.GET,
                    entity,
                    Integer.class,
                    applicationSettings.getClientId()
            );

            Integer newCompletionCount = response.getBody();

            if ( newCompletionCount != null && newCompletionCount > completionCount ) {
                completionCount = newCompletionCount;
                return true;
            }
        } catch ( Exception e ){
            log.warn( "Issue polling CCRS" );
        }

        return false;
    }

    public List<THSJob> getJobsForUser( String userId ) {
        return getJobsForUser( userId, false );
    }

    public List<THSJob> getJobsForUser( String userId, boolean withResults ) {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity entity = new HttpEntity(createHeaders());
        // getForObject cannot specify headers so we use exchange

        log.info( "Get Jobs for Client: (" + applicationSettings.getClientId() + "), User: (" + userId + ")" );
        ResponseEntity<List<THSJob>> response = restTemplate.exchange(
                applicationSettings.getProcessServerURI() + "/queue/client/{clientId}/user/{userId}?withResults={withResults}",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<THSJob>>(){},
                applicationSettings.getClientId(),
                userId,
                withResults
                );

        if ( withResults && response.getBody() != null ) {
            for ( THSJob job : response.getBody() ) {
                job.migrateCSVResultToSequence();
            }
        }

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
        private String emailJobLinkPrefix;
        private Boolean emailOnJobSubmitted;
        private Boolean emailOnJobStart;
        private Boolean emailOnJobComplete;
    }

    @ToString
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class JobSubmissionResponse {
        private String message;
        private List<String> jobIds;
    }

    private static class NoOpResponseErrorHandler extends
            DefaultResponseErrorHandler {

        @Override
        public void handleError( ClientHttpResponse response) throws IOException {
        }

    }
}
