package com.rnbwarden.redisearch.client;

import com.rnbwarden.redisearch.entity.RedisSearchableEntity;
import com.rnbwarden.redisearch.entity.SearchableField;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RediSearchClient<E extends RedisSearchableEntity> {

    void recreateIndex();
    void dropIndex();
    Long getKeyCount();

    void save(E entity);
    void delete(String key);

    SearchableField<E> getField(String name);

    Optional<E> findByKey(String key);

    default SearchResults<E> findByFields(Map<String, String> fieldNameValues) {

        return findByFields(fieldNameValues, new SearchContext());
    }

    SearchResults<E> findByFields(Map<String, String> fieldNameValues, SearchContext searchContext);

    SearchResults<E> find(SearchContext searchContext);

    PageableSearchResults<E> search(SearchContext searchContext);

    PageableSearchResults<E> findAll(Integer limit);

    PageableSearchResults<E> findAll(SearchContext pagingSearchContext);

    List<E> deserialize(SearchResults<E> searchResults);
}