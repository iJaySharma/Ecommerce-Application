package com.pms.service;

import com.pms.dto.request.InventoryUpdateRequest;
import com.pms.dto.request.PriceUpdateRequest;
import com.pms.dto.request.ProductRequest;
import com.pms.dto.response.ProductResponse;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);
    ProductResponse updateProduct(Long id, ProductRequest request);
    ProductResponse updatePrice(Long id, PriceUpdateRequest request);
    ProductResponse updateInventory(Long id, InventoryUpdateRequest request);
    ProductResponse setEnabled(Long id, boolean enabled);
    ProductResponse getProduct(Long id);
    List<ProductResponse> getAllProducts();
    List<ProductResponse> getProductsByCategory(Long categoryId);
}
