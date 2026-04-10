package com.team35.freelance.job.dto;

public class RateJobRequestDTO {

    private Long contractId;
    private Integer rating;

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }
}