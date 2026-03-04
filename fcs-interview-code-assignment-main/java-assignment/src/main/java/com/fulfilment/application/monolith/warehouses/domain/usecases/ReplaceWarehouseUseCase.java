package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    if (newWarehouse == null
        || newWarehouse.businessUnitCode == null
        || newWarehouse.businessUnitCode.isBlank()
        || newWarehouse.location == null
        || newWarehouse.location.isBlank()
        || newWarehouse.capacity == null
        || newWarehouse.stock == null) {
      throw new IllegalArgumentException("Warehouse payload is invalid");
    }

    Warehouse currentWarehouse = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (currentWarehouse == null) {
      throw new IllegalStateException("Warehouse to replace does not exist");
    }

    var location = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (location == null) {
      throw new IllegalArgumentException("Invalid warehouse location");
    }

    if (!newWarehouse.stock.equals(currentWarehouse.stock)) {
      throw new IllegalArgumentException("Replacement warehouse stock must match current stock");
    }

    if (newWarehouse.capacity <= 0 || newWarehouse.stock < 0 || newWarehouse.capacity < currentWarehouse.stock) {
      throw new IllegalArgumentException(
          "Replacement warehouse capacity cannot accommodate current stock");
    }

    var activeWarehousesExcludingCurrent =
        warehouseStore.getAll().stream()
            .filter(warehouse -> warehouse.archivedAt == null)
            .filter(
                warehouse ->
                    !warehouse.businessUnitCode.equals(currentWarehouse.businessUnitCode))
            .toList();

    var activeAtTargetLocation =
        activeWarehousesExcludingCurrent.stream()
            .filter(warehouse -> location.identification.equals(warehouse.location))
            .toList();

    if (activeAtTargetLocation.size() >= location.maxNumberOfWarehouses) {
      throw new IllegalArgumentException("Maximum number of warehouses reached for location");
    }

    int currentLocationCapacity = activeAtTargetLocation.stream().mapToInt(warehouse -> warehouse.capacity).sum();
    int futureCapacity = currentLocationCapacity + newWarehouse.capacity;
    if (futureCapacity > location.maxCapacity) {
      throw new IllegalArgumentException("Location maximum capacity exceeded");
    }

    currentWarehouse.archivedAt = LocalDateTime.now();
    warehouseStore.update(currentWarehouse);

    newWarehouse.createdAt = LocalDateTime.now();
    newWarehouse.archivedAt = null;

    warehouseStore.create(newWarehouse);
  }
}
