package com.jacobsonmt.ths.settings;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ths.settings")
@Getter
@Setter
public class ApplicationSettings {

    private String command;
    private String jobsDirectory;
    private String outputScoredPDBFilename;
    private String outputCSVFilename;
    private String inputPDBFilename;
    private String inputFASTAFilename;
    private String jobSerializationFilename;
    private boolean loadJobsFromDisk;

    private int concurrentJobs = 1;
    private int userProcessLimit = 2;
    private int userJobLimit = 200;
    private boolean purgeSavedJobs = true;
    private int purgeSavedJobsTimeHours = 1;
    private int purgeAfterHours = 24;
    private boolean emailOnJobSubmitted = true;
    private boolean emailOnJobStart = true;

}