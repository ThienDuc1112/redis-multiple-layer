package com.example.multi_cache.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO implements Serializable {  // <-- THÊM Serializable
    private static final long serialVersionUID = 1L;  // <-- THÊM serialVersionUID

    private Long id;
    private String code;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer quantity;
    private Boolean active;
}