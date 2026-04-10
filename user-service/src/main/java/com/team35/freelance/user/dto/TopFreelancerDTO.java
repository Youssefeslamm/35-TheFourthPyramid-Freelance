package com.team35.freelance.user.dto;

public class TopFreelancerDTO {
    private Long userId;
    private String name;
    private Double totalEarnings;
    private Long contractCount;

    public TopFreelancerDTO() {}
    public TopFreelancerDTO(Long userId, String name, Double totalEarnings, Long contractCount) {
        this.userId = userId; this.name = name;
        this.totalEarnings = totalEarnings; this.contractCount = contractCount;
    }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(Double totalEarnings) { this.totalEarnings = totalEarnings; }
    public Long getContractCount() { return contractCount; }
    public void setContractCount(Long contractCount) { this.contractCount = contractCount; }
}
