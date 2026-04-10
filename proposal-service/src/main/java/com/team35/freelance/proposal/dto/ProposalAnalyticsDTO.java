package com.team35.freelance.proposal.dto;



    public class ProposalAnalyticsDTO {
        private Long totalProposals;
        private Long acceptedProposals;
        private Long rejectedProposals;
        private Double totalBidValue;
        private Double averageBid;
        private Double acceptanceRate;

        public ProposalAnalyticsDTO(Long totalProposals, Long acceptedProposals,
                                    Long rejectedProposals, Double totalBidValue,
                                    Double averageBid, Double acceptanceRate) {
            this.totalProposals    = totalProposals;
            this.acceptedProposals = acceptedProposals;
            this.rejectedProposals = rejectedProposals;
            this.totalBidValue     = totalBidValue;
            this.averageBid        = averageBid;
            this.acceptanceRate    = acceptanceRate;
        }

        public Long getTotalProposals()    { return totalProposals; }
        public Long getAcceptedProposals() { return acceptedProposals; }
        public Long getRejectedProposals() { return rejectedProposals; }
        public Double getTotalBidValue()   { return totalBidValue; }
        public Double getAverageBid()      { return averageBid; }
        public Double getAcceptanceRate()  { return acceptanceRate; }
    }

