package com.fulfilment.application.monolith.fulfillment;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.List;

@ApplicationScoped
public class FulfillmentRepository implements FulfillmentStore, PanacheRepository<FulfillmentAssignment> {

  @Inject EntityManager entityManager;

  @Override
  public void persist(FulfillmentAssignment assignment) {
    entityManager.persist(assignment);
  }

  /** Returns all assignments for a given store. */
  public List<FulfillmentAssignment> findByStoreId(Long storeId) {
    return list("storeId", storeId);
  }

  /** Returns all assignments for a given warehouse. */
  public List<FulfillmentAssignment> findByWarehouseCode(String warehouseCode) {
    return list("warehouseCode", warehouseCode);
  }

  /**
   * Returns the number of distinct warehouses already fulfilling a specific product for a store.
   */
  public long countDistinctWarehousesForProductAndStore(Long productId, Long storeId) {
    return find("productId = ?1 and storeId = ?2", productId, storeId)
        .stream()
        .map(a -> a.warehouseCode)
        .distinct()
        .count();
  }

  /**
   * Returns the number of distinct warehouses fulfilling a store (across all products).
   */
  public long countDistinctWarehousesForStore(Long storeId) {
    return find("storeId", storeId)
        .stream()
        .map(a -> a.warehouseCode)
        .distinct()
        .count();
  }

  /**
   * Returns the number of distinct product types stored in a warehouse.
   */
  public long countDistinctProductsInWarehouse(String warehouseCode) {
    return find("warehouseCode", warehouseCode)
        .stream()
        .map(a -> a.productId)
        .distinct()
        .count();
  }

  /** Checks if the exact same assignment already exists. */
  public boolean exists(String warehouseCode, Long productId, Long storeId) {
    return count("warehouseCode = ?1 and productId = ?2 and storeId = ?3",
        warehouseCode, productId, storeId) > 0;
  }
}
