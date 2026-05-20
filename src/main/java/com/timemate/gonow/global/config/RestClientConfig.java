package com.timemate.gonow.global.config;

import com.timemate.gonow.global.client.FlaskClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class RestClientConfig {

    @Value("${flask.url}")
    private String flaskUrl;

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.baseUrl(flaskUrl).build();
    }

    @Bean
    public HttpServiceProxyFactory factory(RestClient restClient) {
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        return HttpServiceProxyFactory.builderFor(adapter).build();
    }

    @Bean
    public FlaskClient flaskClient(HttpServiceProxyFactory factory) {
        return factory.createClient(FlaskClient.class);
    }
}
