package com.rnbwarden.redisearch.client.lettuce;

import com.redislabs.lettusearch.search.SearchResults;
import com.rnbwarden.redisearch.client.PageableSearchResults;
import com.rnbwarden.redisearch.client.PagedSearchResult;
import com.rnbwarden.redisearch.entity.RedisSearchableEntity;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class LettucePagingSearchResults<E extends RedisSearchableEntity> implements PageableSearchResults<E> {

    private final com.redislabs.lettusearch.search.SearchResults<String, Object> delegate;
    private final LettuceRediSearchClient<E> lettuceRediSearchClient;
    private final Consumer<Exception> exceptionConsumer;
    private final String keyPrefix;

    LettucePagingSearchResults(String keyPrefix,
                               SearchResults<String, Object> delegate,
                               LettuceRediSearchClient<E> lettuceRediSearchClient,
                               Consumer<Exception> exceptionConsumer) {

        this.keyPrefix = keyPrefix;
        this.delegate = delegate;
        this.lettuceRediSearchClient = lettuceRediSearchClient;
        this.exceptionConsumer = exceptionConsumer;
    }

    @Override
    public Long getTotalResults() {

        return delegate.count();
    }

    @Override
    public Stream<PagedSearchResult<E>> getResultStream(boolean useParallel) {

        return (useParallel ? delegate.parallelStream() : delegate.stream())
                .filter(Objects::nonNull)
                .map(this::createSearchResult);
    }

    private PagedSearchResult<E> createSearchResult(com.redislabs.lettusearch.search.SearchResult<String, Object> searchResult) {

        return new LettucePagedSearchResult<>(keyPrefix, searchResult.documentId(), lettuceRediSearchClient, exceptionConsumer);
    }
}
