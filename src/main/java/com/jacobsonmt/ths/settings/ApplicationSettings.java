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

    private String processServerURI;

    private int userProcessLimit = 2;
    private int userJobLimit = 200;

    private boolean emailOnJobSubmitted = true;
    private boolean emailOnJobStart = true;

}