package com.example.multi_cache.messaging;

import com.example.multi_cache.config.L1CacheManager;
import com.example.multi_cache.config.L2CacheManager;
import com.example.multi_cache.dto.ProductDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductChangeListener implements MessageListener {

    private final L1CacheManager l1CacheManager;
    private final L2CacheManager l2CacheManager;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.instance-id:default}")
    private String instanceId;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            ProductChangeMessage changeMsg = objectMapper.readValue(json, ProductChangeMessage.class);

            // Bỏ qua message từ chính instance này
            if (instanceId.equals(changeMsg.getInstanceId())) {
                log.debug("Ignoring message from same instance: {}", instanceId);
                return;
            }

            log.info("Received {} message for product {} from instance {}",
                    changeMsg.getChangeType(), changeMsg.getProductId(), changeMsg.getInstanceId());

            switch (changeMsg.getChangeType()) {
                case CREATE:
                    handleProductCreation(changeMsg);
                    break;
                case DELETE:
                    handleProductDeletion(changeMsg);
                    break;
                default:
                    log.warn("Unknown change type: {}", changeMsg.getChangeType());
            }

        } catch (Exception e) {
            log.error("Error processing product change message", e);
        }
    }

    private void handleProductCreation(ProductChangeMessage msg) {
        ProductDTO product = msg.getProductData();
        if (product != null) {
            // Thêm vào L1 cache
            l1CacheManager.putProduct(product.getId(), product);

            // Thêm vào L2 cache
            l2CacheManager.putProduct(product.getId(), product);

            log.info("Cached product {} (code: {}) from CREATE event",
                    product.getId(), product.getCode());
        }
    }

    private void handleProductDeletion(ProductChangeMessage msg) {
        Long productId = msg.getProductId();
        l1CacheManager.evictProduct(productId);

        log.info("Evicted product {} from caches from DELETE event", productId);
    }
}