package com.team35.freelance.contracts.dto;

public class WalletBalanceDTO {
    private Long id;
    private String status;

    public WalletBalanceDTO() {
    }

    public WalletBalanceDTO(Long id, String status) {
        this.id = id;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
