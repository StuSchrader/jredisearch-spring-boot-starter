package com.rnbwarden.redisearch.client.lettuce;

import com.rnbwarden.redisearch.client.SearchResult;

import java.util.Map;

public class LettuceSearchResult<K extends String, V> implements SearchResult<K, V> {

    private final com.redislabs.lettusearch.search.SearchResult<K, V> delegate;
    private final String keyPrefix;

    LettuceSearchResult(String keyPrefix, com.redislabs.lettusearch.search.SearchResult<K, V> searchResult) {

        this.keyPrefix = keyPrefix;
        this.delegate = searchResult;
    }

    @Override
    public Map<K, V> getFields() {

        return delegate;
    }

    @Override
    public V getField(K key) {

        return delegate.get(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public K getId() {

        String documentId = delegate.documentId();
        String key = documentId.substring(keyPrefix.length());
        return (K) key;
    }
}