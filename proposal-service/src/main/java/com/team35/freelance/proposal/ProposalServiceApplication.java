package com.team35.freelance.proposal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;


@EnableCaching
@EnableFeignClients
@SpringBootApplication
@EnableNeo4jRepositories(basePackages = "com.team35.freelance.proposal.repository")
@EnableJpaRepositories(basePackages = "com.team35.freelance.proposal.repository")
public class ProposalServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProposalServiceApplication.class, args);
    }

}
