package com.team35.freelance.contract.mapper;

import com.team35.freelance.contract.model.Contract;
import com.team35.freelance.contracts.dto.ContractDTO;

public final class ContractDtoMapper {

    private ContractDtoMapper() {
    }

    public static ContractDTO toDto(Contract contract) {
        if (contract == null) {
            return null;
        }
        ContractDTO dto = new ContractDTO();
        dto.setId(contract.getId());
        dto.setJobId(contract.getJobId());
        dto.setFreelancerId(contract.getFreelancerId());
        dto.setClientId(contract.getClientId());
        dto.setProposalId(contract.getProposalId());
        dto.setAgreedAmount(contract.getAgreedAmount());
        dto.setStatus(contract.getStatus() != null ? contract.getStatus().name() : null);
        dto.setStartDate(contract.getStartDate());
        dto.setEndDate(contract.getEndDate());
        dto.setMetadata(contract.getMetadata());
        return dto;
    }
}
