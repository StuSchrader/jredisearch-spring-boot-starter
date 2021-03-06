package com.rnbwarden.redisearch.client.jedis;

import com.rnbwarden.redisearch.client.SearchableTagField;
import io.redisearch.Schema;

import java.util.function.Function;

public class SearchableJedisTagField<E> extends SearchableJedisField<E> implements SearchableTagField {

    public SearchableJedisTagField(String name,
                                   boolean sortable,
                                   Function<E, String> serializeFunction) {

        super(name, serializeFunction, QUERY_SYNTAX, new Schema.TagField(name)); //WARDEN: implement SORTABLE
    }
}
