package com.example.multi_cache.service;

import com.example.multi_cache.dto.ProductDTO;

import java.util.List;

public interface ProductService {
    ProductDTO createProduct(ProductDTO productDTO);
    ProductDTO getProductById(Long id);
    void deleteProduct(Long id);
}
