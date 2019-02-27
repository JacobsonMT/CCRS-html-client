package com.jacobsonmt.ths.model;

import lombok.extern.log4j.Log4j2;

import java.util.Iterator;
import java.util.Map;

@Log4j2
public class PurgeOldJobs implements Runnable {

    private Map<String, THSJob> savedJobs;

    public PurgeOldJobs( Map<String, THSJob> savedJobs ) {
        this.savedJobs = savedJobs;
    }

    @Override
    public void run() {
        int jobsPurged = 0;
        synchronized ( savedJobs ) {
            for ( Iterator<Map.Entry<String, THSJob>> it = savedJobs.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, THSJob> entry = it.next();
                THSJob job = entry.getValue();
                if ( job.isComplete() && System.currentTimeMillis() > job.getSaveExpiredDate() ) {
                    job.setSaved( false );
                    job.setSaveExpiredDate( null );
                    it.remove();
                    log.debug( "Purged " + job.getJobId() );
                    jobsPurged++;
                }
            }
        }
        log.info( "Purged " + Integer.toString( jobsPurged ) + " old jobs." );
    }

}