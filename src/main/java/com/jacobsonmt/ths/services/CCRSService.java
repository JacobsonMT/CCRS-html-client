package com.jacobsonmt.ths.services;

import com.jacobsonmt.ths.model.Base;
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
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
@Service
public class CCRSService {

    @Autowired
    private ApplicationSettings applicationSettings;

    @Autowired
    private SiteSettings siteSettings;

    public ResponseEntity<JobSubmissionResponse> submitJob( String userId, String label, String fasta, String email, boolean hidden) {
        RestTemplate restTemplate = new RestTemplateBuilder().errorHandler(new NoOpResponseErrorHandler()).build();
        JobSubmission jobSubmission = new JobSubmission( userId, label, fasta, email, hidden,
                siteSettings.getFullUrl() + "job/",
                applicationSettings.isEmailOnJobSubmitted(),
                applicationSettings.isEmailOnJobStart(),
                applicationSettings.isEmailOnJobComplete());
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

        // Add parsed version of result csv to aid in creation of front-end visualisations
        THSJob job = response.getBody();

        if (job != null && job.getResult() != null) {
            List<Base> sequence = Arrays.stream( job.getResult().getResultCSV().split( "\\r?\\n" ) )
                    .skip( 1 )
                    .map( mapBase )
                    .collect( Collectors.toList() );
            job.getResult().setSequence( sequence );

            // Remove string version of results to cut down ont data transfer
            job.getResult().setResultCSV( "" );
        }

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

    private static Function<String, Base> mapBase = ( rawLine ) -> {
        List<String> line = Arrays.asList( rawLine.split( "\t" ) );

        Base base = new Base( line.get( 2 ), Integer.valueOf( line.get( 3 ) ), Double.valueOf( line.get( 4 ) ) );

        if ( line.size() > 5 ) {
            base.setList( line.stream().skip( 5 ).map( Double::parseDouble ).collect( Collectors.toList() ) );
        }
        return base;
    };


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
