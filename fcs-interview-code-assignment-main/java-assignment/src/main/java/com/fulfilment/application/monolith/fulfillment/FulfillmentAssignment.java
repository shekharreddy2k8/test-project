package com.fulfilment.application.monolith.fulfillment;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Represents the association between a Warehouse, Product, and Store for fulfillment purposes.
 * <p>
 * Business constraints enforced at the use-case layer:
 * <ul>
 *   <li>Each Product can be fulfilled by at most 2 different Warehouses per Store.</li>
 *   <li>Each Store can be fulfilled by at most 3 different Warehouses (across all products).</li>
 *   <li>Each Warehouse can handle at most 5 distinct Product types.</li>
 * </ul>
 */
@Entity
@Cacheable
@Table(
    name = "fulfillment_assignment",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_warehouse_product_store",
          columnNames = {"warehouseCode", "productId", "storeId"})
    })
public class FulfillmentAssignment {

  @Id
  @GeneratedValue
  public Long id;

  /** Business unit code of the fulfilling warehouse. */
  @Column(nullable = false)
  public String warehouseCode;

  /** ID referencing the {@code Product} entity. */
  @Column(nullable = false)
  public Long productId;

  /** ID referencing the {@code Store} entity. */
  @Column(nullable = false)
  public Long storeId;

  public FulfillmentAssignment() {}

  public FulfillmentAssignment(String warehouseCode, Long productId, Long storeId) {
    this.warehouseCode = warehouseCode;
    this.productId = productId;
    this.storeId = storeId;
  }
}
