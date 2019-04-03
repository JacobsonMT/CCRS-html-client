package com.jacobsonmt.ths.controllers;

import com.jacobsonmt.ths.services.CCRSService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

@Log4j2
@RestController
public class SseController {

    @Autowired
    private CCRSService ccrsService;

    private final List<SseEmitter> sseEmitter = new LinkedList<>();

    /**
     * Initialize the controller and start a repeated task to model state changes.
     */
    @PostConstruct
    public void postConstruct() {
        log.info("Create SseController");
    }

    @Scheduled(fixedRate = 2000)
    public void pollAndUpdate() throws IOException {
        if ( ccrsService.hasChanged() ) {
            synchronized ( sseEmitter ) {
                sseEmitter.forEach( ( SseEmitter emitter ) -> {
                    try {
                        emitter.send( "Update", MediaType.APPLICATION_JSON );
                    } catch ( IOException e ) {
                        emitter.complete();
                    }
                } );
            }
        }
    }

    /**
     * Viewer can register here to get sse messages.
     * @return an server state event emitter
     * @throws IOException if registering the new emitter fails
     */
    @RequestMapping( path = "/register", method = RequestMethod.GET )
    public SseEmitter register() throws IOException {

        SseEmitter emitter = new SseEmitter(60000L );

        synchronized ( sseEmitter ) {
            sseEmitter.add( emitter );
        }
        emitter.onTimeout( () -> {
            synchronized ( sseEmitter ) {
                sseEmitter.remove( emitter );
            }
        } );
        emitter.onCompletion( () -> {
            synchronized ( sseEmitter ) {
                sseEmitter.remove( emitter );
            }
        } );

        return emitter;
    }

    @ExceptionHandler( AsyncRequestTimeoutException.class)
    public void asyncRequestTimeoutExceptionHandler( HttpServletRequest req) {}


}
