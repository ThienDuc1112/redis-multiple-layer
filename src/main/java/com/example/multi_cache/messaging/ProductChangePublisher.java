package com.example.multi_cache.messaging;

import com.example.multi_cache.dto.ProductDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductChangePublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public static final String PRODUCT_CHANGE_CHANNEL = "product-changes";

    @Value("${spring.application.instance-id:default}")
    private String instanceId;

    public void publishProductCreation(ProductDTO product) {
        ProductChangeMessage message = new ProductChangeMessage(
                product.getId(),
                instanceId,
                System.currentTimeMillis(),
                ProductChangeMessage.ChangeType.CREATE,
                product
        );

        redisTemplate.convertAndSend(PRODUCT_CHANGE_CHANNEL, message);
        log.info("Published CREATE message for product {} (code: {}) from instance {}",
                product.getId(), product.getCode(), instanceId);
    }

    public void publishProductUpdate(ProductDTO product) {
        ProductChangeMessage message = new ProductChangeMessage(
                product.getId(),
                instanceId,
                System.currentTimeMillis(),
                ProductChangeMessage.ChangeType.UPDATE,
                product
        );

        redisTemplate.convertAndSend(PRODUCT_CHANGE_CHANNEL, message);
        log.info("Published UPDATE message for product {} (code: {}) from instance {}",
                product.getId(), product.getCode(), instanceId);
    }

    public void publishProductDeletion(Long productId) {
        ProductChangeMessage message = new ProductChangeMessage(
                productId,
                instanceId,
                System.currentTimeMillis(),
                ProductChangeMessage.ChangeType.DELETE,
                null
        );

        redisTemplate.convertAndSend(PRODUCT_CHANGE_CHANNEL, message);
        log.info("Published DELETE message for product {} from instance {}",
                productId, instanceId);
    }
}
