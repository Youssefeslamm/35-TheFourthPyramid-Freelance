CREATE KEYSPACE IF NOT EXISTS freelanceks
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

USE freelanceks;

CREATE TABLE IF NOT EXISTS contract_milestone_events (
                                                         contract_id BIGINT,
                                                         timestamp TIMESTAMP,
                                                         milestone_order INT,
                                                         status TEXT,
                                                         recorded_by TEXT,
                                                         notes TEXT,
                                                         PRIMARY KEY (contract_id, timestamp)
) WITH CLUSTERING ORDER BY (timestamp DESC);