package com.team35.freelance.contract.dto;

public class BatchStatusUpdateDTO {
    private Long contractId;
    private String status;

    public BatchStatusUpdateDTO() {}

    public BatchStatusUpdateDTO(Long contractId, String status) {
        this.contractId = contractId;
        this.status = status;
    }

    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
