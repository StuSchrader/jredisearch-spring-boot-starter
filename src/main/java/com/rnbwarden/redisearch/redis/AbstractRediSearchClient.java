package com.rnbwarden.redisearch.redis;

import io.redisearch.Query;
import io.redisearch.SearchResult;
import io.redisearch.querybuilder.QueryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.StopWatch;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.redisearch.querybuilder.QueryBuilder.intersect;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.reflect.FieldUtils.getAllFieldsList;

public abstract class AbstractRediSearchClient<E extends /**RediSearchEntity &*/RedisSearchableEntity> implements RediSearchClient<E> {

    public static final String ID = "id";
    protected static final String SERIALIZED_DOCUMENT = "sdoc";

    protected static final String ALL_QUERY = "*";
    private final Logger logger = LoggerFactory.getLogger(AbstractRediSearchClient.class);

    @Value("${redis.search.defaultResultLimit:0x7fffffff}")
    protected int defaultMaxValue;

    protected final RedisSerializer<E> redisSerializer;
    protected final List<SearchableField<E>> fields;
    protected final Class<E> clazz;

    protected final Map<RediSearchFieldType, BiFunction<String, Function<E, Object>, ? extends SearchableField<E>>> fieldStrategy = new HashMap<>();
    {
        fieldStrategy.put(RediSearchFieldType.TEXT, SearchableTextField::new);
        fieldStrategy.put(RediSearchFieldType.TAG, SearchableTagField::new);
    }

    public AbstractRediSearchClient(CompressingJacksonRedisSerializer<E> redisSerializer) {

        this.redisSerializer = redisSerializer;
        this.clazz = redisSerializer.getClazz();
        this.fields = getSearchableFields();
    }

    @PostConstruct
    protected abstract void checkAndCreateIndex();

    @Override
    public void recreateIndex() {

        dropIndex();
        checkAndCreateIndex();
    }

    protected List<SearchableField<E>> getSearchableFields() {

        List<SearchableField<E>> searchFields = new ArrayList<>();

        List<Field> fields = getAllFieldsList(clazz);
        fields.forEach(field -> stream(field.getAnnotations())
                .filter(annotation -> annotation instanceof RediSearchField)
                .map(a -> (RediSearchField) a)
                .forEach(annotation -> {
                    searchFields.add(fieldStrategy.get(annotation.type()).apply(annotation.name(), e -> getFieldValue(field, e)));
                }));

        return searchFields;
    }

    protected static Object getFieldValue(Field f, Object obj) {

        try {
            boolean accessible = f.isAccessible();

            f.setAccessible(true);
            Object o = f.get(obj);
            f.setAccessible(accessible);

            return o;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(format("Unable to get RediSearch annotated field value for field: %s of class: %s", f.getName(), obj.getClass()), e);
        }
    }

    protected List<SearchableField<E>> getFields() {

        return fields;
    }

    private SearchableField<E> getField(String name) {

        return fields.stream()
                .filter(f -> f.getName().equals(name))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("invalid field name: " + name));
    }

    protected Map<String, Object> serialize(E entity) {

        Map<String, Object> fields = new HashMap<>();
        getFields().forEach(field -> fields.put(field.getName(), field.serialize(entity)));
        fields.put(SERIALIZED_DOCUMENT, redisSerializer.serialize(entity));
        return fields;
    }

    @Override
    public SearchResult findAll(Integer offset,
                                Integer limit,
                                boolean includeContent) {

        offset = ofNullable(offset).orElse(0);
        limit = ofNullable(limit).orElse(defaultMaxValue);

        Query query = new Query(ALL_QUERY)
                .limit(offset, limit);

        if (!includeContent) {
            query.setNoContent();
        }

        return performTimedOperation("findAll", () -> search(query));
    }

    @Override
    public Long getKeyCount() {

        return findAll(0, 0, false).totalResults;
    }

    protected abstract SearchResult search(Query query);

    protected Query intersectQueryBuilder(Map<String, String> fieldNameValues) {

        QueryNode node = intersect();
        fieldNameValues.forEach((name, value) -> node.add(name, getField(name).getQuerySyntax(value)));
        return new Query(node.toString());
    }

    protected List<E> deserialize(SearchResult searchResult) {

        return Optional.of(searchResult)
                .map(sr -> sr.docs.stream()
                        .map(document -> (byte[]) document.get(SERIALIZED_DOCUMENT))
                        .filter(Objects::nonNull)
                        .map(redisSerializer::deserialize)
                        .collect(toList()))
                .orElse(emptyList());
    }

    /**
     * Simple method to handle the stopWatch and logging requirements around a given RedisClient operation
     */
    protected <E> E performTimedOperation(String name, Supplier<E> supplier) {

        StopWatch stopWatch = new StopWatch(name);
        stopWatch.start();

        E entity = supplier.get();

        stopWatch.stop();
        logger.debug("{}", stopWatch.prettyPrint());

        return entity;
    }

    public static String getIndex(Class<?> clazz) {

        return stream(clazz.getAnnotations())
                .filter(annotation -> annotation instanceof RediSearchEntity)
                .findAny()
                .map(RediSearchEntity.class::cast)
                .map(RediSearchEntity::name)
                .get();
    }

}