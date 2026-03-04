package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void create(Warehouse warehouse) {
    validateMandatoryFields(warehouse);

    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new IllegalArgumentException("Business unit code already exists");
    }

    var location = locationResolver.resolveByIdentifier(warehouse.location);
    if (location == null) {
      throw new IllegalArgumentException("Invalid warehouse location");
    }

    if (warehouse.stock > warehouse.capacity) {
      throw new IllegalArgumentException("Warehouse stock cannot exceed capacity");
    }

    var activeWarehousesAtLocation =
        warehouseStore.getAll().stream()
            .filter(current -> current.archivedAt == null)
            .filter(current -> location.identification.equals(current.location))
            .toList();

    if (activeWarehousesAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new IllegalArgumentException("Maximum number of warehouses reached for location");
    }

    int currentLocationCapacity =
        activeWarehousesAtLocation.stream().mapToInt(current -> current.capacity).sum();
    int futureCapacity = currentLocationCapacity + warehouse.capacity;
    if (futureCapacity > location.maxCapacity) {
      throw new IllegalArgumentException("Location maximum capacity exceeded");
    }

    warehouse.createdAt = LocalDateTime.now();
    warehouse.archivedAt = null;

    // if all went well, create the warehouse
    warehouseStore.create(warehouse);
  }

  private static void validateMandatoryFields(Warehouse warehouse) {
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
