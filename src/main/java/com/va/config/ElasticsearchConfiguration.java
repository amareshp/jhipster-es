package com.va.config;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.shield.ShieldPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.EntityMapper;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class ElasticsearchConfiguration {

    @Value("${spring.data.elasticsearch.cluster-name}")
    private String clusterId;

    @Value("${spring.data.elasticsearch.cluster-nodes}")
    private String clusterNodes;

    @Value("${spring.data.elasticsearch.region}")
    private String region;

    @Bean
    public ElasticsearchTemplate elasticsearchTemplate(Client client, Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder) {
        //set the client for ssl
        // Build the settings for our client.
        //String clusterId = "6cede37a76d5996185f4ecf9d7df1b4f"; // Your cluster ID here
        //String region = "us-west-2"; // Your region here
        boolean enableSsl = true;

        Settings settings = Settings.settingsBuilder()
            .put("transport.ping_schedule", "5s")
            //.put("transport.sniff", false) // Disabled by default and *must* be disabled.
            .put("cluster.name", clusterId)
            .put("action.bulk.compress", false)
            .put("shield.transport.ssl", enableSsl)
            .put("request.headers.X-Found-Cluster", clusterId)
            .put("shield.user", "admin:admin") // your shield username and password
            .build();

        String hostname = clusterId + "." + region + ".aws.found.io";
// Instantiate a TransportClient and add the cluster to the list of addresses to connect to.
// Only port 9343 (SSL-encrypted) is currently supported.
        Client client2 = null;
        try {
            client2 = TransportClient.builder()
                .addPlugin(ShieldPlugin.class)
                .settings(settings)
                .build()
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(hostname), 9343));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }


        return new ElasticsearchTemplate(client2, new CustomEntityMapper(jackson2ObjectMapperBuilder.createXmlMapper(false).build()));
    }

    public class CustomEntityMapper implements EntityMapper {

        private ObjectMapper objectMapper;

        public CustomEntityMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        }

        @Override
        public String mapToString(Object object) throws IOException {
            return objectMapper.writeValueAsString(object);
        }

        @Override
        public <T> T mapToObject(String source, Class<T> clazz) throws IOException {
            return objectMapper.readValue(source, clazz);
        }
    }
}
