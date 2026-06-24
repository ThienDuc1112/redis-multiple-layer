package com.example.multi_cache.service;

import com.example.multi_cache.config.L1CacheManager;
import com.example.multi_cache.config.L2CacheManager;
import com.example.multi_cache.dto.ProductDTO;
import com.example.multi_cache.entity.Product;
import com.example.multi_cache.messaging.ProductChangePublisher;
import com.example.multi_cache.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final L1CacheManager l1CacheManager;
    private final L2CacheManager l2CacheManager;
    private final ProductChangePublisher changePublisher;
    private final ModelMapper modelMapper;

    @Override
    @CachePut(value = "products", key = "#result.id")
    @Transactional
    public ProductDTO createProduct(ProductDTO productDTO) {
        // Convert DTO to Entity
        Product product = modelMapper.map(productDTO, Product.class);

        // Save to database
        Product saved = productRepository.save(product);

        // Convert back to DTO
        ProductDTO savedDTO = modelMapper.map(saved, ProductDTO.class);

        // Add to L1 cache (local)
        l1CacheManager.putProduct(saved.getId(), savedDTO);

        // Add to L2 cache (Redis)
        l2CacheManager.putProduct(saved.getId(), savedDTO);

        // Publish creation event to other instances
        changePublisher.publishProductCreation(savedDTO);

        log.info("Created product {} with code {} from instance", saved.getId(), saved.getCode());
        return savedDTO;
    }

    @Override
    @Cacheable(value = "products", key = "#id")
    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        // Check L1 cache first
        Object cachedProduct = l1CacheManager.getProduct(id);
        if (cachedProduct != null) {
            log.debug("Product {} found in L1 cache", id);
            return (ProductDTO) cachedProduct;
        }

        // Check L2 cache (Redis) - handled by @Cacheable
        log.info("Fetching product {} from database", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        ProductDTO productDTO = modelMapper.map(product, ProductDTO.class);

        // Add to L1 cache for future requests
        l1CacheManager.putProduct(id, productDTO);

        return productDTO;
    }

    @Override
    @CacheEvict(value = "products", key = "#id")
    @Transactional
    public void deleteProduct(Long id) {
        // Check if product exists
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }

        // Delete from database
        productRepository.deleteById(id);

        // Delete from L1 cache (local)
        l1CacheManager.evictProduct(id);

        // Delete from L2 cache (Redis)
        l2CacheManager.evictProduct(id);

        // Publish deletion event to other instances
        changePublisher.publishProductDeletion(id);

        log.info("Deleted product {} from DB and caches", id);
    }

    /**
     * Cập nhật product - Thêm method nếu cần
     */
    @CachePut(value = "products", key = "#id")
    @Transactional
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        // Find existing product
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        // Update fields
        existing.setCode(productDTO.getCode());
        existing.setName(productDTO.getName());
        existing.setDescription(productDTO.getDescription());
        existing.setPrice(productDTO.getPrice());
        existing.setQuantity(productDTO.getQuantity());
        existing.setActive(productDTO.getActive());

        // Save to database
        Product updated = productRepository.save(existing);

        // Convert to DTO
        ProductDTO updatedDTO = modelMapper.map(updated, ProductDTO.class);

        // Update L1 cache
        l1CacheManager.putProduct(id, updatedDTO);

        // Update L2 cache
        l2CacheManager.putProduct(id, updatedDTO);

        // Publish update event
        changePublisher.publishProductUpdate(updatedDTO);

        log.info("Updated product {} with code {} from instance", id, updated.getCode());
        return updatedDTO;
    }

    /**
     * Lấy tất cả products - Method thêm nếu cần
     */
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        log.info("Fetching all products from database");
        return productRepository.findAll().stream()
                .map(product -> modelMapper.map(product, ProductDTO.class))
                .toList();
    }

    /**
     * Tìm product theo code
     */
    @Transactional(readOnly = true)
    public ProductDTO getProductByCode(String code) {
        log.info("Fetching product by code: {}", code);
        Product product = productRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Product not found with code: " + code));
        return modelMapper.map(product, ProductDTO.class);
    }
}