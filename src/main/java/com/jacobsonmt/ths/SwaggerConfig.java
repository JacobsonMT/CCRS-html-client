package com.jacobsonmt.ths;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.DocExpansion;
import springfox.documentation.swagger.web.UiConfiguration;
import springfox.documentation.swagger.web.UiConfigurationBuilder;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket api() {
        return new Docket( DocumentationType.SWAGGER_2)
                .useDefaultResponseMessages(false)
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths( PathSelectors.regex("/api/.*"))
                .build()
                .apiInfo( apinfo() );
    }

    @Bean
    UiConfiguration uiConfig() {
        return UiConfigurationBuilder.builder()
                .docExpansion( DocExpansion.LIST) // or DocExpansion.NONE or DocExpansion.FULL
                .deepLinking( false )
                .build();
    }

    private ApiInfo apinfo() {
        return new ApiInfoBuilder()
                .title("LIST-SI API")
                .description("Rest API to automate access to LIST-SI")
                .license("HTML Server")
                .licenseUrl("http://list-si.msl.ubc.ca")
                .build();
    }

}