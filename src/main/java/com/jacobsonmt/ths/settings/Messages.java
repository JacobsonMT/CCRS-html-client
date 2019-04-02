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

    private String title;

}