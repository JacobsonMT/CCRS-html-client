package com.jacobsonmt.ths.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class Message {
    @ApiModel( description = "Severity level." )
    public enum MessageLevel {
        INFO, WARNING, ERROR;
    }

    @ApiModelProperty( notes = "Severity of message.")
    private MessageLevel level;
    @ApiModelProperty( notes = "Message contents.")
    private String message;
}
