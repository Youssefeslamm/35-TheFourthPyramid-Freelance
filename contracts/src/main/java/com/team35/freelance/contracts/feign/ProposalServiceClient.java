package com.team35.freelance.contracts.feign;

import com.team35.freelance.contracts.dto.ProposalDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "proposal-service", url = "${feign.proposal-service.url}")
public interface ProposalServiceClient {

    @GetMapping("/api/proposals/{id}")
    ProposalDTO getProposalById(@PathVariable("id") Long id);
}

