package com.jacobsonmt.ths.settings;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ths.messages")
@Getter
@Setter
public class Messages {

    @Getter
    @Setter
    public static class EmailMessages {
        private String submit;
        private String complete;
        private String fail;
    }

    private EmailMessages email;


    private String title;

}