package com.team35.freelance.contract.cassandra;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.LocalDateTime;

@Table("contract_milestone_events")
public class ContractMilestoneEvent {

    @PrimaryKeyColumn(name = "contract_id", type = PrimaryKeyType.PARTITIONED)
    private Long contractId;

    @PrimaryKeyColumn(name = "timestamp", type = PrimaryKeyType.CLUSTERED, ordinal = 0, ordering = Ordering.DESCENDING)
    private LocalDateTime timestamp;

    @Column("milestone_order")
    private Integer milestoneOrder;

    private String status;

    @Column("recorded_by")
    private Long recordedBy;

    private String notes;

    public ContractMilestoneEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public ContractMilestoneEvent(Long contractId,
                                  Integer milestoneOrder,
                                  String status,
                                  Long recordedBy,
                                  String notes) {
        this.contractId = contractId;
        this.timestamp = LocalDateTime.now();
        this.milestoneOrder = milestoneOrder;
        this.status = status;
        this.recordedBy = recordedBy;
        this.notes = notes;
    }

    public Long getContractId() { return contractId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Integer getMilestoneOrder() { return milestoneOrder; }
    public String getStatus() { return status; }
    public Long getRecordedBy() { return recordedBy; }
    public String getNotes() { return notes; }

    public void setContractId(Long contractId) { this.contractId = contractId; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setMilestoneOrder(Integer milestoneOrder) { this.milestoneOrder = milestoneOrder; }
    public void setStatus(String status) { this.status = status; }
    public void setRecordedBy(Long recordedBy) { this.recordedBy = recordedBy; }
    public void setNotes(String notes) { this.notes = notes; }
}
