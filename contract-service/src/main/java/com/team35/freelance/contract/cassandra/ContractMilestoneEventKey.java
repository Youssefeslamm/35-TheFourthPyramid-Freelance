package com.team35.freelance.contract.cassandra;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@PrimaryKeyClass
public class ContractMilestoneEventKey implements Serializable {

    @PrimaryKeyColumn(name = "contract_id", type = PrimaryKeyType.PARTITIONED)
    private Long contractId;

    @PrimaryKeyColumn(name = "event_time", type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private Instant eventTime;

    public ContractMilestoneEventKey() {
    }

    public ContractMilestoneEventKey(Long contractId, Instant eventTime) {
        this.contractId = contractId;
        this.eventTime = eventTime;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public void setEventTime(Instant eventTime) {
        this.eventTime = eventTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContractMilestoneEventKey that)) return false;
        return Objects.equals(contractId, that.contractId) && Objects.equals(eventTime, that.eventTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contractId, eventTime);
    }
}
