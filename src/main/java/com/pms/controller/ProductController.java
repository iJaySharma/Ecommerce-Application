package com.pms.controller;

import com.pms.dto.request.InventoryUpdateRequest;
import com.pms.dto.request.PriceUpdateRequest;
import com.pms.dto.request.ProductRequest;
import com.pms.dto.response.ProductResponse;
import com.pms.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ADMIN, SUPER_ADMIN (enforced in SecurityConfig)
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @PatchMapping("/{id}/price")
    public ResponseEntity<ProductResponse> updatePrice(@PathVariable Long id, @Valid @RequestBody PriceUpdateRequest request) {
        return ResponseEntity.ok(productService.updatePrice(id, request));
    }

    @PatchMapping("/{id}/inventory")
    public ResponseEntity<ProductResponse> updateInventory(@PathVariable Long id, @Valid @RequestBody InventoryUpdateRequest request) {
        return ResponseEntity.ok(productService.updateInventory(id, request));
    }

    @PatchMapping("/{id}/enable")
    public ResponseEntity<ProductResponse> enable(@PathVariable Long id) {
        return ResponseEntity.ok(productService.setEnabled(id, true));
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<ProductResponse> disable(@PathVariable Long id) {
        return ResponseEntity.ok(productService.setEnabled(id, false));
    }

    // public
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProduct(id));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAll() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponse>> getByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId));
    }
}
