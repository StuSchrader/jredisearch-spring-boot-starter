package com.rnbwarden.redisearch.autoconfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rnbwarden.redisearch.CompressingJacksonSerializer;
import com.rnbwarden.redisearch.entity.RediSearchEntity;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.redis.serializer.RedisSerializer;

import javax.annotation.PostConstruct;
import java.util.Set;

@EnableConfigurationProperties(RedisProperties.class)
public abstract class AbstractRediSearchClientAutoConfiguration implements RediSearchClientAutoConfiguration {

    @Value("${redis.search.base-package}")
    protected String basePackage;

//    @Value("${redis.search.useCompression:true}")
//    protected boolean useCompression;

    @Autowired
    @Qualifier("rediSearchObjectMapper")
    private ObjectMapper rediSearchObjectMapper;

    @Value("${redis.search.defaultResultLimit:1000000}")
    protected Long defaultMaxResults;

    protected DefaultListableBeanFactory beanFactory;
    protected ApplicationContext applicationContext;

    @Override
    @PostConstruct
    public void init() {

        initializeRediSearchBeans();
    }

    private void initializeRediSearchBeans() {

        getRediSearchEntities().stream()
                .map(BeanDefinition::getBeanClassName)
                .map(className -> {
                    try {
                        return Class.forName(className, false, applicationContext.getClassLoader());
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                })
                .forEach(this::createRediSearchBeans);
    }

    private Set<BeanDefinition> getRediSearchEntities() {

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RediSearchEntity.class));

        return scanner.findCandidateComponents(basePackage);
    }

    abstract void createRediSearchBeans(Class<?> clazz);

    String getRedisearchBeanName(Class<?> clazz) {

        String simpleName = clazz.getSimpleName();
        simpleName = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
        return simpleName + "RediSearchClient";
    }

    <T> RedisSerializer<T> createRedisSerializer(Class<T> clazz) {

        return new CompressingJacksonSerializer<>(clazz, rediSearchObjectMapper);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {

        this.beanFactory = (DefaultListableBeanFactory) beanFactory;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

        this.applicationContext = applicationContext;
    }

}
