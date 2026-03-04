package com.fulfilment.application.monolith.fulfillment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Use-case for assigning a Warehouse as a fulfilment unit for a Product in a Store.
 *
 * <p>Business constraints:
 * <ol>
 *   <li>Each Product can be fulfilled by at most <strong>2</strong> different Warehouses per Store.</li>
 *   <li>Each Store can be fulfilled by at most <strong>3</strong> different Warehouses in total.</li>
 *   <li>Each Warehouse can handle at most <strong>5</strong> distinct Product types.</li>
 * </ol>
 */
@ApplicationScoped
public class AssignFulfillmentUseCase {

  static final int MAX_WAREHOUSES_PER_PRODUCT_PER_STORE = 2;
  static final int MAX_WAREHOUSES_PER_STORE = 3;
  static final int MAX_PRODUCTS_PER_WAREHOUSE = 5;

  private final FulfillmentStore store;

  public AssignFulfillmentUseCase(FulfillmentStore store) {
    this.store = store;
  }

  /**
   * Assigns a warehouse to fulfil a product for a store.
   *
   * @throws IllegalArgumentException if a business constraint would be violated or the
   *     assignment already exists.
   */
  @Transactional
  public FulfillmentAssignment assign(String warehouseCode, Long productId, Long storeId) {
    validateInput(warehouseCode, productId, storeId);

    if (store.exists(warehouseCode, productId, storeId)) {
      throw new IllegalArgumentException(
          "Fulfillment assignment already exists for this warehouse, product and store");
    }

    // Constraint 1: max 2 warehouses per product per store
    long warehousesForProductStore =
        store.countDistinctWarehousesForProductAndStore(productId, storeId);
    if (warehousesForProductStore >= MAX_WAREHOUSES_PER_PRODUCT_PER_STORE) {
      throw new IllegalArgumentException(
          "Product " + productId + " already has " + MAX_WAREHOUSES_PER_PRODUCT_PER_STORE
              + " warehouses assigned for store " + storeId);
    }

    // Constraint 2: max 3 warehouses per store (across all products)
    // Only count if this warehouse is new for the store
    boolean warehouseAlreadyServesStore =
        store.findByStoreId(storeId).stream()
            .anyMatch(a -> a.warehouseCode.equals(warehouseCode));
    if (!warehouseAlreadyServesStore) {
      long distinctWarehousesForStore = store.countDistinctWarehousesForStore(storeId);
      if (distinctWarehousesForStore >= MAX_WAREHOUSES_PER_STORE) {
        throw new IllegalArgumentException(
            "Store " + storeId + " already has " + MAX_WAREHOUSES_PER_STORE
                + " different warehouses assigned");
      }
    }

    // Constraint 3: max 5 product types per warehouse
    // Only count if this product is new for the warehouse
    boolean productAlreadyInWarehouse =
        store.findByWarehouseCode(warehouseCode).stream()
            .anyMatch(a -> a.productId.equals(productId));
    if (!productAlreadyInWarehouse) {
      long distinctProductsInWarehouse =
          store.countDistinctProductsInWarehouse(warehouseCode);
      if (distinctProductsInWarehouse >= MAX_PRODUCTS_PER_WAREHOUSE) {
        throw new IllegalArgumentException(
            "Warehouse " + warehouseCode + " already stores " + MAX_PRODUCTS_PER_WAREHOUSE
                + " different product types");
      }
    }

    var assignment = new FulfillmentAssignment(warehouseCode, productId, storeId);
    store.persist(assignment);
    return assignment;
  }

  private static void validateInput(String warehouseCode, Long productId, Long storeId) {
    if (warehouseCode == null || warehouseCode.isBlank()) {
      throw new IllegalArgumentException("warehouseCode must not be blank");
    }
    if (productId == null || productId <= 0) {
      throw new IllegalArgumentException("productId must be a positive number");
    }
    if (storeId == null || storeId <= 0) {
      throw new IllegalArgumentException("storeId must be a positive number");
    }
  }
}
