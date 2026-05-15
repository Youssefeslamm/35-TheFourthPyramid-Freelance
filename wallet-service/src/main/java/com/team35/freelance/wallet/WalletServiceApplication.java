package com.team35.freelance.wallet;

import com.team35.freelance.contracts.feign.ContractServiceClient;
import com.team35.freelance.contracts.feign.JobServiceClient;
import com.team35.freelance.contracts.feign.UserServiceClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableCaching
@EnableFeignClients(clients = {
        UserServiceClient.class,
        ContractServiceClient.class,
        JobServiceClient.class
})
@SpringBootApplication
public class WalletServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletServiceApplication.class, args);
    }

}
