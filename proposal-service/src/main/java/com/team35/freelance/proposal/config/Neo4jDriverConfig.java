package com.team35.freelance.proposal.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class Neo4jDriverConfig {

    @Value("${spring.data.neo4j.uri:bolt://neo4j:7687}")
    private String uri;

    @Value("${spring.data.neo4j.username:neo4j}")
    private String neo4jUsername;

    @Value("${spring.data.neo4j.password:neo4jpass}")
    private String neo4jPassword;

    @Bean
    @Primary
    public Driver neo4jDriver() {
        return GraphDatabase.driver(uri, AuthTokens.basic(neo4jUsername, neo4jPassword));
    }
}