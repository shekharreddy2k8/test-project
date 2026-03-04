package com.fulfilment.application.monolith.fulfillment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AssignFulfillmentUseCaseTest {

  private InMemoryFulfillmentStore store;
  private AssignFulfillmentUseCase useCase;

  @BeforeEach
  void setUp() {
    store = new InMemoryFulfillmentStore();
    useCase = new AssignFulfillmentUseCase(store);
  }

  // -----------------------------------------------------------------------
  // Happy path
  // -----------------------------------------------------------------------

  @Test
  void shouldCreateAssignmentWhenAllConstraintsAreSatisfied() {
    var result = assertDoesNotThrow(() -> useCase.assign("MWH.001", 1L, 1L));

    assertNotNull(result);
    assertEquals("MWH.001", result.warehouseCode);
    assertEquals(1L, result.productId);
    assertEquals(1L, result.storeId);
    assertEquals(1, store.all().size());
  }

  // -----------------------------------------------------------------------
  // Duplicate check
  // -----------------------------------------------------------------------

  @Test
  void shouldRejectDuplicateAssignment() {
    useCase.assign("MWH.001", 1L, 1L);

    assertThrows(IllegalArgumentException.class, () -> useCase.assign("MWH.001", 1L, 1L));
  }

  // -----------------------------------------------------------------------
  // Constraint 1: max 2 warehouses per product per store
  // -----------------------------------------------------------------------

  @Test
  void shouldRejectWhenProductAlreadyHasTwoWarehousesForStore() {
    useCase.assign("MWH.001", 1L, 1L);
    useCase.assign("MWH.002", 1L, 1L);

    // Third warehouse for same product + store must be rejected
    assertThrows(IllegalArgumentException.class, () -> useCase.assign("MWH.003", 1L, 1L));
  }

  @Test
  void shouldAllowSameWarehouseForDifferentProductsInSameStore() {
    // Product 1 is fine with MWH.001
    useCase.assign("MWH.001", 1L, 1L);
    // Product 2 with same warehouse in same store is separate – OK
    assertDoesNotThrow(() -> useCase.assign("MWH.001", 2L, 1L));
  }

  // -----------------------------------------------------------------------
  // Constraint 2: max 3 warehouses per store
  // -----------------------------------------------------------------------

  @Test
  void shouldRejectWhenStoreAlreadyHasThreeDistinctWarehouses() {
    useCase.assign("MWH.001", 1L, 1L);
    useCase.assign("MWH.002", 2L, 1L);
    useCase.assign("MWH.003", 3L, 1L);

    // A fourth distinct warehouse for the same store must be rejected
    assertThrows(IllegalArgumentException.class, () -> useCase.assign("MWH.004", 4L, 1L));
  }

  @Test
  void shouldAllowExistingWarehouseToFulfillAdditionalProductForSameStore() {
    // MWH.001, MWH.002, MWH.003 are the 3 allowed warehouses for store 1
    useCase.assign("MWH.001", 1L, 1L);
    useCase.assign("MWH.002", 2L, 1L);
    useCase.assign("MWH.003", 3L, 1L);

    // MWH.001 adds product 2 – still only 3 distinct warehouses, so OK
    assertDoesNotThrow(() -> useCase.assign("MWH.001", 2L, 1L));
  }

  // -----------------------------------------------------------------------
  // Constraint 3: max 5 product types per warehouse
  // -----------------------------------------------------------------------

  @Test
  void shouldRejectWhenWarehouseAlreadyHandlesFiveProductTypes() {
    // Fill MWH.001 with 5 distinct products across different stores
    useCase.assign("MWH.001", 1L, 1L);
    useCase.assign("MWH.001", 2L, 1L);
    useCase.assign("MWH.001", 3L, 1L);
    useCase.assign("MWH.001", 4L, 1L);
    useCase.assign("MWH.001", 5L, 1L);

    // Sixth product type must be rejected
    assertThrows(IllegalArgumentException.class, () -> useCase.assign("MWH.001", 6L, 2L));
  }

  @Test
  void shouldAllowExistingProductInWarehouseForNewStore() {
    // Product 1 already in MWH.001 – count stays the same, so adding to another store is fine
    useCase.assign("MWH.001", 1L, 1L);

    assertDoesNotThrow(() -> useCase.assign("MWH.001", 1L, 2L));
  }

  // -----------------------------------------------------------------------
  // Input validation
  // -----------------------------------------------------------------------

  @Test
  void shouldRejectBlankWarehouseCode() {
    assertThrows(IllegalArgumentException.class, () -> useCase.assign("", 1L, 1L));
  }

  @Test
  void shouldRejectNullProductId() {
    assertThrows(IllegalArgumentException.class, () -> useCase.assign("MWH.001", null, 1L));
  }

  @Test
  void shouldRejectNullStoreId() {
    assertThrows(IllegalArgumentException.class, () -> useCase.assign("MWH.001", 1L, null));
  }

  // -----------------------------------------------------------------------
  // In-memory FulfillmentStore implementation for unit testing
  // -----------------------------------------------------------------------

  private static class InMemoryFulfillmentStore implements FulfillmentStore {

    private final List<FulfillmentAssignment> assignments = new ArrayList<>();
    private long idSeq = 1;

    public List<FulfillmentAssignment> all() {
      return List.copyOf(assignments);
    }

    @Override
    public void persist(FulfillmentAssignment assignment) {
      assignment.id = idSeq++;
      assignments.add(assignment);
    }

    @Override
    public boolean exists(String warehouseCode, Long productId, Long storeId) {
      return assignments.stream().anyMatch(
          a -> a.warehouseCode.equals(warehouseCode)
              && a.productId.equals(productId)
              && a.storeId.equals(storeId));
    }

    @Override
    public List<FulfillmentAssignment> findByStoreId(Long storeId) {
      return assignments.stream().filter(a -> a.storeId.equals(storeId)).toList();
    }

    @Override
    public List<FulfillmentAssignment> findByWarehouseCode(String warehouseCode) {
      return assignments.stream().filter(a -> a.warehouseCode.equals(warehouseCode)).toList();
    }

    @Override
    public long countDistinctWarehousesForProductAndStore(Long productId, Long storeId) {
      return assignments.stream()
          .filter(a -> a.productId.equals(productId) && a.storeId.equals(storeId))
          .map(a -> a.warehouseCode)
          .distinct()
          .count();
    }

    @Override
    public long countDistinctWarehousesForStore(Long storeId) {
      return assignments.stream()
          .filter(a -> a.storeId.equals(storeId))
          .map(a -> a.warehouseCode)
          .distinct()
          .count();
    }

    @Override
    public long countDistinctProductsInWarehouse(String warehouseCode) {
      return assignments.stream()
          .filter(a -> a.warehouseCode.equals(warehouseCode))
          .map(a -> a.productId)
          .distinct()
          .count();
    }
  }
}
