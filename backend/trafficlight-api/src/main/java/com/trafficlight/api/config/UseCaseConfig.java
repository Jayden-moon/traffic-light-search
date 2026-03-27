package com.trafficlight.api.config;

import com.trafficlight.domain.service.AggregationService;
import com.trafficlight.domain.service.DataCollectorService;
import com.trafficlight.domain.service.IndexService;
import com.trafficlight.domain.service.SearchService;
import com.trafficlight.usecase.AggregationUseCase;
import com.trafficlight.usecase.DataSyncUseCase;
import com.trafficlight.usecase.IndexUseCase;
import com.trafficlight.usecase.SearchUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public SearchUseCase searchUseCase(SearchService searchService) {
        return new SearchUseCase(searchService);
    }

    @Bean
    public IndexUseCase indexUseCase(IndexService indexService) {
        return new IndexUseCase(indexService);
    }

    @Bean
    public AggregationUseCase aggregationUseCase(AggregationService aggregationService) {
        return new AggregationUseCase(aggregationService);
    }

    @Bean
    public DataSyncUseCase dataSyncUseCase(DataCollectorService dataCollectorService, IndexService indexService) {
        return new DataSyncUseCase(dataCollectorService, indexService);
    }
}
