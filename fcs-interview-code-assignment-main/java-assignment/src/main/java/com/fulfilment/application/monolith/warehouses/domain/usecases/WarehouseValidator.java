package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;

/** Shared validation utilities for warehouse use cases. */
public final class WarehouseValidator {

  private WarehouseValidator() {}

  /**
   * Validates mandatory fields that apply to both creating and replacing a warehouse.
   *
   * @throws IllegalArgumentException if any mandatory field is missing or invalid.
   */
  public static void validateMandatoryFields(Warehouse warehouse) {
    if (warehouse == null
        || warehouse.businessUnitCode == null
        || warehouse.businessUnitCode.isBlank()
        || warehouse.location == null
        || warehouse.location.isBlank()
        || warehouse.capacity == null
        || warehouse.stock == null) {
      throw new IllegalArgumentException("Warehouse payload is invalid");
    }

    if (warehouse.capacity <= 0 || warehouse.stock < 0) {
      throw new IllegalArgumentException("Warehouse capacity/stock values are invalid");
    }
  }
}
