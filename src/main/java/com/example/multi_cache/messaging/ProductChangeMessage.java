
package com.example.multi_cache.messaging;

import com.example.multi_cache.dto.ProductDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductChangeMessage implements Serializable {
    private Long productId;
    private String instanceId;
    private Long timestamp;
    private ChangeType changeType;
    private ProductDTO productData; // Dùng cho CREATE/UPDATE

    public enum ChangeType {
        CREATE, UPDATE, DELETE
    }
}
