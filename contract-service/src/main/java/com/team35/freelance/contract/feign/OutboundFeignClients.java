package com.team35.freelance.contract.feign;

import com.team35.freelance.contracts.dto.JobDTO;
import com.team35.freelance.contracts.dto.UserProfileDTO;
import com.team35.freelance.contracts.feign.JobServiceClient;
import com.team35.freelance.contracts.feign.UserServiceClient;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

// tal3 le user-service w job-service b try/catch (§2.4) — Member 11 yesta5dem el methods de enrichment; el 5 endpoints el S4-READ-DB lesa DB bas
@Component
public class OutboundFeignClients {

    private static final Logger log = LoggerFactory.getLogger(OutboundFeignClients.class);

    private final UserServiceClient userServiceClient;
    private final JobServiceClient jobServiceClient;

    public OutboundFeignClients(UserServiceClient userServiceClient, JobServiceClient jobServiceClient) {
        this.userServiceClient = userServiceClient;
        this.jobServiceClient = jobServiceClient;
    }

    // user-service getUserById — NotFound WARN mokhtalef 3an ba2y FeignException (§2.4)
    public Optional<UserProfileDTO> tryFetchUserProfile(Long userId, String authorization) {
        try {
            return Optional.ofNullable(userServiceClient.getUserById(userId));
        } catch (FeignException.NotFound e) {
            log.warn("Feign user-service NotFound userId={}", userId, e);
            return Optional.empty();
        } catch (FeignException e) {
            log.warn("Feign user-service userId={} httpStatus={}", userId, e.status(), e);
            return Optional.empty();
        }
    }

    // job-service getJobById — nafs el pattern
    public Optional<JobDTO> tryFetchJobById(Long jobId) {
        try {
            return Optional.ofNullable(jobServiceClient.getJobById(jobId));
        } catch (FeignException.NotFound e) {
            log.warn("Feign job-service NotFound jobId={}", jobId, e);
            return Optional.empty();
        } catch (FeignException e) {
            log.warn("Feign job-service jobId={} httpStatus={}", jobId, e.status(), e);
            return Optional.empty();
        }
    }
}
