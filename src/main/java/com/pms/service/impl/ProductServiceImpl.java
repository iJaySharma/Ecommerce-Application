package com.pms.service.impl;

import com.pms.dto.request.InventoryUpdateRequest;
import com.pms.dto.request.PriceUpdateRequest;
import com.pms.dto.request.ProductRequest;
import com.pms.dto.response.ProductResponse;
import com.pms.entity.Category;
import com.pms.entity.Inventory;
import com.pms.entity.Product;
import com.pms.exception.ResourceNotFoundException;
import com.pms.repository.CategoryRepository;
import com.pms.repository.InventoryRepository;
import com.pms.repository.ProductRepository;
import com.pms.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(category)
                .enabled(true)
                .build();
        Product savedProduct = productRepository.save(product);

        Inventory inventory = Inventory.builder()
                .product(savedProduct)
                .quantity(request.getQuantity())
                .build();
        Inventory savedInventory = inventoryRepository.save(inventory);
        savedProduct.setInventory(savedInventory);

        return toResponse(savedProduct, savedInventory.getQuantity());
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCategory(category);
        Product saved = productRepository.save(product);

        Inventory inventory = inventoryRepository.findByProductId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", id));
        inventory.setQuantity(request.getQuantity());
        inventoryRepository.save(inventory);

        return toResponse(saved, inventory.getQuantity());
    }

    @Override
    @Transactional
    public ProductResponse updatePrice(Long id, PriceUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        product.setPrice(request.getPrice());
        Product saved = productRepository.save(product);
        Integer qty = inventoryRepository.findByProductId(id).map(Inventory::getQuantity).orElse(0);
        return toResponse(saved, qty);
    }

    @Override
    @Transactional
    public ProductResponse updateInventory(Long id, InventoryUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        Inventory inventory = inventoryRepository.findByProductId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", id));
        inventory.setQuantity(request.getQuantity());
        inventoryRepository.save(inventory);
        return toResponse(product, inventory.getQuantity());
    }

    @Override
    @Transactional
    public ProductResponse setEnabled(Long id, boolean enabled) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        product.setEnabled(enabled);
        Product saved = productRepository.save(product);
        Integer qty = inventoryRepository.findByProductId(id).map(Inventory::getQuantity).orElse(0);
        return toResponse(saved, qty);
    }

    @Override
    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        Integer qty = inventoryRepository.findByProductId(id).map(Inventory::getQuantity).orElse(0);
        return toResponse(product, qty);
    }

    @Override
    public List<ProductResponse> getAllProducts() {
        return productRepository.findByEnabledTrue().stream()
                .map(p -> toResponse(p, inventoryRepository.findByProductId(p.getId()).map(Inventory::getQuantity).orElse(0)))
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> getProductsByCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category", "id", categoryId);
        }
        return productRepository.findByCategoryIdAndEnabledTrue(categoryId).stream()
                .map(p -> toResponse(p, inventoryRepository.findByProductId(p.getId()).map(Inventory::getQuantity).orElse(0)))
                .collect(Collectors.toList());
    }

    private ProductResponse toResponse(Product product, Integer quantity) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .enabled(product.isEnabled())
                .availableQuantity(quantity)
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .build();
    }
}
