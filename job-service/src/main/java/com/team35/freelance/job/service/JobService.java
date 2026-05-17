package com.team35.freelance.job.service;

import feign.FeignException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.team35.freelance.contracts.feign.ContractServiceClient;
import com.team35.freelance.contracts.feign.ProposalServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.team35.freelance.job.common.adapter.ElasticsearchHitAdapter;
import com.team35.freelance.job.common.observer.EntityObserver;
import com.team35.freelance.job.common.observer.MongoEventLogger;
import com.team35.freelance.job.dto.CloseJobRequest;
import com.team35.freelance.job.dto.ContractLookupProjection;
import com.team35.freelance.job.dto.JobAttachmentAlertDTO;
import com.team35.freelance.job.dto.JobDashboardDTO;
import com.team35.freelance.job.dto.JobProposalSummaryDTO;
import com.team35.freelance.job.dto.JobProposalSummaryDTOBuilder;
import com.team35.freelance.job.dto.RateJobRequestDTO;
import com.team35.freelance.job.dto.TopBudgetJobDTO;
import com.team35.freelance.job.elasticsearch.JobSearchDocument;
import com.team35.freelance.job.exception.BadRequestException;
import com.team35.freelance.job.exception.ResourceNotFoundException;
import com.team35.freelance.job.model.Job;
import com.team35.freelance.job.model.JobAttachment;
import com.team35.freelance.job.model.JobCategory;
import com.team35.freelance.job.model.JobStatus;
import com.team35.freelance.job.repository.JobAttachmentRepository;
import com.team35.freelance.job.repository.JobRepository;
import com.team35.freelance.job.messaging.publisher.JobEventPublisher;
import com.team35.freelance.contracts.events.JobClosedEvent;
import com.team35.freelance.contracts.events.JobRatedEvent;
import com.team35.freelance.contracts.events.JobStatusChangedEvent;

@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private static final List<String> INDEXED_FIELDS = List.of(
            "id",
            "title",
            "name",
            "description",
            "category",
            "budgetMin",
            "budgetMax",
            "rating",
            "status"
    );

    private final JobRepository jobRepository;
    private final JobAttachmentRepository jobAttachmentRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ElasticsearchHitAdapter elasticsearchHitAdapter;
    private final List<EntityObserver> observers = new ArrayList<>();
    private final JobDashboardCacheService jobDashboardCacheService;
    private final RestTemplate elasticsearchRestTemplate = new RestTemplate();
    private final JobEventPublisher jobEventPublisher;

    private final ProposalServiceClient proposalServiceClient;
    private final ContractServiceClient contractServiceClient;

    @Value("${spring.elasticsearch.uris:http://elasticsearch:9200}")
    private String elasticsearchUris;

    public JobService(JobRepository jobRepository,
                      JobAttachmentRepository jobAttachmentRepository,
                      ElasticsearchOperations elasticsearchOperations,
                      ElasticsearchHitAdapter elasticsearchHitAdapter,
                      MongoEventLogger mongoEventLogger,
                      JobDashboardCacheService jobDashboardCacheService,
                      JobEventPublisher jobEventPublisher,

                       

                      ProposalServiceClient proposalServiceClient,
                      ContractServiceClient contractServiceClient) {

        this.jobRepository = jobRepository;
        this.jobAttachmentRepository = jobAttachmentRepository;
        this.elasticsearchOperations = elasticsearchOperations;
        this.elasticsearchHitAdapter = elasticsearchHitAdapter;
        this.jobDashboardCacheService = jobDashboardCacheService;
        this.jobEventPublisher = jobEventPublisher;

        this.proposalServiceClient = proposalServiceClient;
        this.contractServiceClient = contractServiceClient;


        this.observers.add(mongoEventLogger);
    }

    private void notifyObservers(String eventType, Object payload) {
        for (EntityObserver observer : observers) {
            observer.onEvent(eventType, payload);
        }
    }

    public void registerObserver(EntityObserver observer) {
        observers.add(observer);
    }

    public void unregisterObserver(EntityObserver observer) {
        observers.remove(observer);
    }

    @Cacheable(value = "job-service::S2-F3", key = "#id + ':' + #startDate + ':' + #endDate")
    public JobProposalSummaryDTO getProposalSummary(Long id, String startDate, String endDate) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        try {
            com.team35.freelance.contracts.dto.JobProposalSummaryDTO proposalSummary =
                    proposalServiceClient.getJobProposalSummary(id, startDate, endDate);

            return JobProposalSummaryDTO.builder()
                    .jobId(job.getId())
                    .title(job.getTitle())
                    .totalProposals(proposalSummary.getTotalProposals() == null ? 0L : proposalSummary.getTotalProposals())
                    .averageBidAmount(proposalSummary.getAverageBidAmount() == null ? 0.0 : proposalSummary.getAverageBidAmount())
                    .lowestBid(proposalSummary.getLowestBid() == null ? 0.0 : proposalSummary.getLowestBid())
                    .highestBid(proposalSummary.getHighestBid() == null ? 0.0 : proposalSummary.getHighestBid())
                    .build();

        } catch (FeignException.NotFound e) {
            return JobProposalSummaryDTO.builder()
                    .jobId(job.getId())
                    .title(job.getTitle())
                    .totalProposals(0L)
                    .averageBidAmount(0.0)
                    .lowestBid(0.0)
                    .highestBid(0.0)
                    .build();

        } catch (FeignException e) {
            log.warn("proposal-service unavailable while building proposal summary for job {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Proposal service temporarily unavailable");
        }
    }

    @CacheEvict(value = {
            "job-service::job",
            "job-service::S2-F1",
            "job-service::S2-F3",
            "job-service::S2-F5",
            "job-service::S2-F6",
            "job-service::S2-F9",
            "job-service::S2-F10",
            "job-service::S2-F12"
    }, allEntries = true)
    public Job updateRequirements(Long id, Map<String, Object> incomingRequirements) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        if (job.getRequirements() == null) {
            job.setRequirements(new HashMap<>());
        }

        job.getRequirements().putAll(incomingRequirements);

        Job saved = jobRepository.save(job);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "JOB_REQUIREMENTS_UPDATED");
        payload.put("jobId", saved.getId());

        notifyObservers("JOB_UPDATED", payload);

        return saved;
    }

    @CacheEvict(value = {
            "job-service::job",
            "job-service::S2-F1",
            "job-service::S2-F3",
            "job-service::S2-F5",
            "job-service::S2-F6",
            "job-service::S2-F9",
            "job-service::S2-F10",
            "job-service::S2-F12"
    }, allEntries = true)
    public Job createJob(Job job) {
        applyJobDefaults(job);
        validateJob(job);

        if (job.getStatus() == null) {
            job.setStatus(JobStatus.OPEN);
        }
        if (job.getRating() == null) {
            job.setRating(0.0);
        }
        if (job.getTotalRatings() == null) {
            job.setTotalRatings(0);
        }

        Job saved = jobRepository.save(job);
        indexJobForSearchSafely(saved, "auto_crud_create");

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "JOB_CREATED");
        payload.put("jobId", saved.getId());

        notifyObservers("JOB_CREATED", payload);

        return saved;
    }

    @Cacheable(value = "job-service::job", key = "#id")
    public Job getJobById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    @CacheEvict(value = {
            "job-service::job",
            "job-service::S2-F1",
            "job-service::S2-F3",
            "job-service::S2-F5",
            "job-service::S2-F6",
            "job-service::S2-F9",
            "job-service::S2-F10",
            "job-service::S2-F12"
    }, allEntries = true)
    public Job updateJob(Long id, Job updatedJob) {
        Job existing = getJobById(id);
        Long requestedClientId = updatedJob == null ? null : updatedJob.getClientId();

        JobStatus oldStatus = existing.getStatus();

        applyJobDefaults(updatedJob);
        validateJob(updatedJob);

        if (requestedClientId != null) {
            existing.setClientId(requestedClientId);
        }
        existing.setTitle(updatedJob.getTitle());
        existing.setDescription(updatedJob.getDescription());
        existing.setCategory(updatedJob.getCategory());
        existing.setStatus(updatedJob.getStatus());
        existing.setBudgetMin(updatedJob.getBudgetMin());
        existing.setBudgetMax(updatedJob.getBudgetMax());

        if (updatedJob.getRating() != null) {
            existing.setRating(updatedJob.getRating());
        }

        if (updatedJob.getTotalRatings() != null) {
            existing.setTotalRatings(updatedJob.getTotalRatings());
        }

        existing.setRequirements(updatedJob.getRequirements());

        Job saved = jobRepository.save(existing);
        indexJobForSearchSafely(saved, "auto_crud_update");

        if (oldStatus != null && saved.getStatus() != null && oldStatus != saved.getStatus()) {
            jobEventPublisher.publishStatusChanged(
                    new JobStatusChangedEvent(saved.getId(), oldStatus.name(), saved.getStatus().name())
            );
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "JOB_UPDATED");
        payload.put("jobId", saved.getId());

        notifyObservers("JOB_UPDATED", payload);

        return saved;
    }

    @CacheEvict(value = {
            "job-service::job",
            "job-service::S2-F1",
            "job-service::S2-F3",
            "job-service::S2-F5",
            "job-service::S2-F6",
            "job-service::S2-F9",
            "job-service::S2-F10",
            "job-service::S2-F12"
    }, allEntries = true)
    public void deleteJob(Long id) {
        Job existing = getJobById(id);

        jobRepository.delete(existing);
        removeJobFromSearchSafely(id);

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "JOB_DELETED");
        payload.put("jobId", id);

        notifyObservers("JOB_DELETED", payload);
    }

    public List<Job> searchJobs(String query, String status, Double minBudget, Double maxBudget) {
        JobStatus statusFilter = parseStatus(status);
        if (minBudget != null && maxBudget != null && minBudget > maxBudget) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "minBudget cannot be greater than maxBudget"
            );
        }

        return jobRepository.searchJobs(
                query == null || query.isBlank() ? null : query.trim(),
                statusFilter == null ? null : statusFilter.name(),
                minBudget,
                maxBudget);
    }

    @Cacheable(
            value = "job-service::S2-F10",
            key = "(#query == null ? '' : #query.trim().toLowerCase()) + ':' + (#category == null ? '' : #category.trim().toUpperCase()) + ':' + (#status == null ? '' : #status.trim().toUpperCase()) + ':' + (#minBudget == null ? '' : #minBudget) + ':' + (#maxBudget == null ? '' : #maxBudget)"
    )
    public List<Job> fullTextSearchJobs(String query,
                                        String category,
                                        String status,
                                        Double minBudget,
                                        Double maxBudget) {
        String normalizedQuery = normalizeSearchQuery(query);
        JobCategory categoryFilter = parseCategory(category);
        JobStatus statusFilter = parseStatus(status);
        validateBudgetRange(minBudget, maxBudget);

        try {
            if (!ensureJobsIndexExists(false)) {
                return searchJobsFullTextFallback(normalizedQuery, categoryFilter, statusFilter, minBudget, maxBudget);
            }

            Query elasticsearchQuery = buildFullTextQuery(
                    normalizedQuery,
                    categoryFilter,
                    statusFilter,
                    minBudget,
                    maxBudget
            );

            SearchHits<JobSearchDocument> hits = elasticsearchOperations.search(
                    elasticsearchQuery,
                    JobSearchDocument.class
            );

            List<Long> orderedIds = hits.stream()
                    .map(elasticsearchHitAdapter::adapt)
                    .filter(job -> job != null)
                    .map(Job::getId)
                    .filter(id -> id != null)
                    .distinct()
                    .toList();

            if (orderedIds.isEmpty()) {
                return searchJobsFullTextFallback(normalizedQuery, categoryFilter, statusFilter, minBudget, maxBudget);
            }

            Map<Long, Job> jobsById = jobRepository.findAllById(orderedIds).stream()
                    .collect(Collectors.toMap(Job::getId, job -> job));

            List<Job> jobs = orderedIds.stream()
                    .map(jobsById::get)
                    .filter(job -> job != null)
                    .toList();

            if (jobs.isEmpty()) {
                return searchJobsFullTextFallback(normalizedQuery, categoryFilter, statusFilter, minBudget, maxBudget);
            }

            return jobs;

        } catch (Exception ex) {
            log.warn("Elasticsearch full-text search failed, falling back to PostgreSQL: {}", getRootCauseMessage(ex));
            return searchJobsFullTextFallback(normalizedQuery, categoryFilter, statusFilter, minBudget, maxBudget);
        }
    }

    @CacheEvict(value = "job-service::S2-F10", allEntries = true)
    public void indexJobForSearch(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        try {
            saveJobDocumentDirectly(job);

            Map<String, Object> payload = new HashMap<>();
            payload.put("jobId", job.getId());
            payload.put("indexedFields", INDEXED_FIELDS);
            payload.put("source", "explicit");

            notifyObservers("INDEXED", payload);

        } catch (Exception ex) {
            log.error("Explicit Elasticsearch indexing failed for job {}: {}", job.getId(), getRootCauseMessage(ex), ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to index job for search: " + getRootCauseMessage(ex));
        }
    }

    private void indexJobForSearchSafely(Job job, String source) {
        if (job == null || job.getId() == null) {
            return;
        }

        try {
            saveJobDocumentDirectly(job);

            Map<String, Object> payload = new HashMap<>();
            payload.put("jobId", job.getId());
            payload.put("indexedFields", INDEXED_FIELDS);
            payload.put("source", source);

            notifyObservers("INDEXED", payload);

        } catch (Exception ex) {
            log.warn("Elasticsearch auto-index failed for job {}: {}", job.getId(), getRootCauseMessage(ex));
        }
    }

    private void removeJobFromSearchSafely(Long jobId) {
        if (jobId == null) {
            return;
        }

        try {
            String url = elasticsearchBaseUrl() + "/jobs/_doc/" + jobId + "?refresh=true";
            elasticsearchRestTemplate.exchange(url, HttpMethod.DELETE, null, String.class);
        } catch (HttpClientErrorException.NotFound ignored) {
            // Already deleted from Elasticsearch.
        } catch (Exception ex) {
            log.warn("Elasticsearch delete failed for job {}: {}", jobId, getRootCauseMessage(ex));
        }
    }

    private List<Job> searchJobsFullTextFallback(String normalizedQuery,
                                                 JobCategory categoryFilter,
                                                 JobStatus statusFilter,
                                                 Double minBudget,
                                                 Double maxBudget) {
        return jobRepository.searchJobsFullTextFallback(
                normalizedQuery,
                categoryFilter == null ? null : categoryFilter.name(),
                statusFilter == null ? null : statusFilter.name(),
                minBudget,
                maxBudget
        );
    }

    private boolean ensureJobsIndexExists(boolean failWhenUnavailable) {
        try {
            createJobsIndexDirectly();
            return true;
        } catch (Exception ex) {
            String message = getRootCauseMessage(ex);

            if (failWhenUnavailable) {
                log.error("Elasticsearch jobs index is not available: {}", message, ex);
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to prepare jobs search index: " + message
                );
            }

            log.warn("Elasticsearch jobs index is not available; using PostgreSQL fallback: {}", message);
            return false;
        }
    }

    private String elasticsearchBaseUrl() {
        return elasticsearchUris.split(",")[0].trim().replaceAll("/+$", "");
    }

    private void createJobsIndexDirectly() {
        String url = elasticsearchBaseUrl() + "/jobs";

        String mappingJson = """
                {
                  "mappings": {
                    "properties": {
                      "id": { "type": "keyword" },
                      "title": { "type": "text" },
                      "name": { "type": "text", "fields": { "keyword": { "type": "keyword" } } },
                      "description": { "type": "text" },
                      "category": { "type": "keyword" },
                      "budgetMin": { "type": "double" },
                      "budgetMax": { "type": "double" },
                      "rating": { "type": "double" },
                      "status": { "type": "keyword" }
                    }
                  }
                }
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            elasticsearchRestTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    new HttpEntity<>(mappingJson, headers),
                    String.class
            );
        } catch (HttpClientErrorException.BadRequest ex) {
            String body = ex.getResponseBodyAsString();
            if (body != null && body.contains("resource_already_exists_exception")) {
                return;
            }
            throw ex;
        }
    }

    private void saveJobDocumentDirectly(Job job) {
        createJobsIndexDirectly();

        JobSearchDocument searchDocument = toSearchDocument(job);
        elasticsearchOperations.save(searchDocument, IndexCoordinates.of("jobs"));
        elasticsearchOperations.indexOps(IndexCoordinates.of("jobs")).refresh();

        Map<String, Object> document = new HashMap<>();
        document.put("id", job.getId());
        document.put("title", job.getTitle());
        document.put("name", job.getTitle());
        document.put("description", job.getDescription());
        document.put("category", job.getCategory() == null ? null : job.getCategory().name());
        document.put("budgetMin", job.getBudgetMin());
        document.put("budgetMax", job.getBudgetMax());
        document.put("rating", job.getRating());
        document.put("status", job.getStatus() == null ? null : job.getStatus().name());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String url = elasticsearchBaseUrl() + "/jobs/_doc/" + job.getId() + "?refresh=true";

        elasticsearchRestTemplate.exchange(
                url,
                HttpMethod.PUT,
                new HttpEntity<>(document, headers),
                String.class
        );
    }

    private LocalDateTime parseSummaryDate(String value, boolean startOfDay) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date range is required");
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (Exception ignored) {
            try {
                LocalDate date = LocalDate.parse(value.trim());
                return startOfDay ? date.atStartOfDay() : date.atTime(23, 59, 59, 999_000_000);
            } catch (Exception ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format");
            }
        }
    }

    private String getRootCauseMessage(Exception ex) {
        Throwable root = ex;

        while (root.getCause() != null) {
            root = root.getCause();
        }

        return root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
    }

    private JobSearchDocument toSearchDocument(Job job) {
        return new JobSearchDocument(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                job.getCategory() == null ? null : job.getCategory().name(),
                job.getBudgetMin(),
                job.getBudgetMax(),
                job.getRating(),
                job.getStatus() == null ? null : job.getStatus().name()
        );
    }

    private Query buildFullTextQuery(String query,
                                     JobCategory category,
                                     JobStatus status,
                                     Double minBudget,
                                     Double maxBudget) {
        List<String> filters = new ArrayList<>();

        if (category != null) {
            filters.add("""
                    {"term":{"category":"%s"}}
                    """.formatted(escapeJson(category.name())).trim());
        }

        if (status != null) {
            filters.add("""
                    {"term":{"status":"%s"}}
                    """.formatted(escapeJson(status.name())).trim());
        }

        if (minBudget != null) {
            filters.add("""
                    {"range":{"budgetMax":{"gte":%s}}}
                    """.formatted(minBudget).trim());
        }

        if (maxBudget != null) {
            filters.add("""
                    {"range":{"budgetMin":{"lte":%s}}}
                    """.formatted(maxBudget).trim());
        }

        String filterJson = String.join(",", filters);
        String escapedQuery = escapeJson(query);
        String escapedWildcardQuery = escapeJson("*" + query.toLowerCase() + "*");

        String json = """
                {
                  "bool": {
                    "must": [
                      {
                        "bool": {
                          "should": [
                            {
                              "multi_match": {
                                "query": "%s",
                                "fields": ["name^3", "title^2", "description"],
                                "type": "best_fields",
                                "fuzziness": "AUTO"
                              }
                            },
                            {
                              "wildcard": {
                                "name": {
                                  "value": "%s",
                                  "case_insensitive": true
                                }
                              }
                            },
                            {
                              "wildcard": {
                                "title": {
                                  "value": "%s",
                                  "case_insensitive": true
                                }
                              }
                            },
                            {
                              "wildcard": {
                                "description": {
                                  "value": "%s",
                                  "case_insensitive": true
                                }
                              }
                            }
                          ],
                          "minimum_should_match": 1
                        }
                      }
                    ],
                    "filter": [%s]
                  }
                }
                """.formatted(escapedQuery, escapedWildcardQuery, escapedWildcardQuery, escapedWildcardQuery, filterJson);

        return new StringQuery(json);
    }

    private String normalizeSearchQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query is required");
        }
        return query.trim();
    }

    private JobCategory parseCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }

        try {
            return JobCategory.valueOf(category.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category");
        }
    }

    private JobStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return JobStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status must be one of OPEN, IN_PROGRESS, CLOSED");
        }
    }

    private void validateBudgetRange(Double minBudget, Double maxBudget) {
        if (minBudget != null && maxBudget != null && minBudget > maxBudget) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minBudget cannot be greater than maxBudget");
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder escaped = new StringBuilder();

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> escaped.append(c);
            }
        }

        return escaped.toString();
    }

    private void validateJob(Job job) {
        if (job == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job request body is required");
        }

        if (job.getTitle() == null || job.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required");
        }

        if (job.getDescription() == null || job.getDescription().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "description is required");
        }
        if (job.getBudgetMin() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "budgetMin is required");
        }

        if (job.getBudgetMax() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "budgetMax is required");
        }

        if (job.getBudgetMin() > job.getBudgetMax()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "budgetMin cannot be greater than budgetMax"
            );
        }
    }

    private void applyJobDefaults(Job job) {
        if (job == null) {
            return;
        }

        if (job.getCategory() == null) {
            job.setCategory(JobCategory.DEVELOPMENT);
        }
        if (job.getClientId() == null) {
            job.setClientId(1L);
        }
        if (job.getStatus() == null) {
            job.setStatus(JobStatus.OPEN);
        }
        if (job.getRating() == null) {
            job.setRating(0.0);
        }
        if (job.getTotalRatings() == null) {
            job.setTotalRatings(0);
        }
        if (job.getRequirements() == null) {
            job.setRequirements(new HashMap<>());
        }
    }

    @Cacheable(value = "job-service::S2-F9", key = "'expired'")
    public List<JobAttachmentAlertDTO> getJobsWithExpiredAttachments() {
        List<JobAttachmentAlertDTO> alerts = new ArrayList<>();
        LocalDate today = LocalDate.now();

        jobRepository.findJobsWithExpiredAttachments().forEach(row -> {
            Job job = jobRepository.findByIdWithAttachments(row.getJobId())
                    .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

            List<JobAttachment> expiredAttachments =
                    jobAttachmentRepository.findByJobIdAndExpiryDateBefore(job.getId(), today);

            if (!expiredAttachments.isEmpty()) {
                alerts.add(
                        JobAttachmentAlertDTO.builder()
                                .jobId(job.getId())
                                .jobTitle(job.getTitle())
                                .jobStatus(job.getStatus())
                                .expiredAttachments(expiredAttachments)
                                .expiredCount(expiredAttachments.size())
                                .build()
                );
            }
        });

        return alerts;
    }

    @Transactional
    @CacheEvict(value = {
            "job-service::job",
            "job-service::S2-F1",
            "job-service::S2-F3",
            "job-service::S2-F5",
            "job-service::S2-F6",
            "job-service::S2-F9",
            "job-service::S2-F10",
            "job-service::S2-F12"
    }, allEntries = true)
    public Job rateJob(Long jobId, RateJobRequestDTO request) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        if (request.getContractId() == null) {
            throw new BadRequestException("contractId is required");
        }
        if (request.getRating() == null) {
            throw new BadRequestException("rating is required");
        }
        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new BadRequestException("rating must be between 1 and 5 inclusive");
        }

        com.team35.freelance.contracts.dto.ContractDTO contract;

        try {
            contract = contractServiceClient.getContract(request.getContractId());
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Contract not found");
        } catch (FeignException e) {
            log.warn("contract-service unavailable while rating job {} using contract {}: {}",
                    jobId, request.getContractId(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Contract service temporarily unavailable");
        }

        if (contract.getJobId() == null || !contract.getJobId().equals(jobId)) {
            throw new BadRequestException("Contract does not belong to this job");
        }

        if (contract.getStatus() == null || !"COMPLETED".equalsIgnoreCase(contract.getStatus())) {
            throw new BadRequestException("Only completed contracts can be rated");
        }

        double currentRating = job.getRating() == null ? 0.0 : job.getRating();
        int totalRatings = job.getTotalRatings() == null ? 0 : job.getTotalRatings();

        double newAverage =
                ((currentRating * totalRatings) + request.getRating())
                        / (totalRatings + 1);

        job.setRating(newAverage);
        job.setTotalRatings(totalRatings + 1);

        Job saved = jobRepository.save(job);
        indexJobForSearchSafely(saved, "auto_crud_update");

        jobEventPublisher.publishJobRated(
                new JobRatedEvent(saved.getId(), request.getContractId(), request.getRating().doubleValue(), null)
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "JOB_RATED");
        payload.put("jobId", saved.getId());

        notifyObservers("JOB_UPDATED", payload);

        return saved;
    }

    @Transactional
    @CacheEvict(value = {
            "job-service::job",
            "job-service::S2-F1",
            "job-service::S2-F3",
            "job-service::S2-F5",
            "job-service::S2-F6",
            "job-service::S2-F9",
            "job-service::S2-F10",
            "job-service::S2-F12"
    }, allEntries = true)
    public Job closeJob(Long id, CloseJobRequest request) {
        Job job = getJobById(id);

        if (request == null || request.getStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }

        if (request.getStatus() != JobStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status must be CLOSED");
        }

        if (job.getStatus() == JobStatus.CLOSED) {
            return job;
        }

        Integer activeContractCount;

        try {
            activeContractCount = contractServiceClient.getActiveContractCountForJob(id);
        } catch (FeignException e) {
            log.warn("contract-service unavailable while closing job {}: {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Contract service temporarily unavailable");
        }

        if (activeContractCount != null && activeContractCount > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job has active contracts");
        }

        JobStatus oldStatus = job.getStatus();

        job.setStatus(JobStatus.CLOSED);

        Job saved = jobRepository.save(job);
        indexJobForSearchSafely(saved, "auto_crud_update");

        if (oldStatus != JobStatus.CLOSED) {
            jobEventPublisher.publishStatusChanged(
                    new JobStatusChangedEvent(
                            saved.getId(),
                            oldStatus == null ? null : oldStatus.name(),
                            JobStatus.CLOSED.name()
                    )
            );
        }

        jobEventPublisher.publishJobClosed(new JobClosedEvent(saved.getId(), saved.getClientId()));

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "JOB_CLOSED");
        payload.put("jobId", saved.getId());

        notifyObservers("JOB_CLOSED", payload);

        return saved;
    }

    @Cacheable(value = "job-service::S2-F5", key = "#key + ':' + #value + ':' + #status")
    public List<Job> filterJobsByRequirement(String key, String value, String status) {
        if (key == null || key.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "key is required");
        }

        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "value is required");
        }

        List<Job> jobs = jobRepository.findByRequirementAndOptionalStatus(
                key.trim(),
                value.trim(),
                status
        );
        if (jobs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No jobs found for requirement");
        }
        return jobs;
    }

    @Cacheable(value = "job-service::S2-F6", key = "#limit")
    public List<TopBudgetJobDTO> getTopBudgetJobs(Integer limit) {
        if (limit == null || limit <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be greater than 0");
        }

        List<Object[]> rows = jobRepository.findTopBudgetJobs(limit);
        List<TopBudgetJobDTO> result = new ArrayList<>();

        for (Object[] row : rows) {
            Long jobId = ((Number) row[0]).longValue();
            String title = (String) row[1];
            Double budgetMax = ((Number) row[2]).doubleValue();
            Long totalProposals = ((Number) row[3]).longValue();

            result.add(new TopBudgetJobDTO(jobId, title, budgetMax, totalProposals));
        }

        return result;
    }

    public JobDashboardDTO getJobDashboard(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "DASHBOARD_VIEWED");
        payload.put("jobId", job.getId());
        payload.put("title", job.getTitle());

        notifyObservers("JOB", payload);

        return jobDashboardCacheService.getJobDashboard(id);
    }
}
