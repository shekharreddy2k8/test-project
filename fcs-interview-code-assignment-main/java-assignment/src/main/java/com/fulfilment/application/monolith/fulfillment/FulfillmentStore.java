package com.fulfilment.application.monolith.fulfillment;

import java.util.List;

/** Port interface abstracting persistence operations for {@link FulfillmentAssignment}. */
public interface FulfillmentStore {

  void persist(FulfillmentAssignment assignment);

  boolean exists(String warehouseCode, Long productId, Long storeId);

  List<FulfillmentAssignment> findByStoreId(Long storeId);

  List<FulfillmentAssignment> findByWarehouseCode(String warehouseCode);

  long countDistinctWarehousesForProductAndStore(Long productId, Long storeId);

  long countDistinctWarehousesForStore(Long storeId);

  long countDistinctProductsInWarehouse(String warehouseCode);
}
