package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ArchiveWarehouseUseCaseTest {

	private InMemoryWarehouseStore warehouseStore;
	private ArchiveWarehouseUseCase useCase;

	@BeforeEach
	void setUp() {
		warehouseStore = new InMemoryWarehouseStore();
		useCase = new ArchiveWarehouseUseCase(warehouseStore);

		Warehouse warehouse = warehouse("MWH.700", "ZWOLLE-001", 20, 5);
		warehouseStore.create(warehouse);
	}

	@Test
	void shouldArchiveWarehouse() {
		Warehouse warehouse = warehouseStore.findByBusinessUnitCode("MWH.700");

		useCase.archive(warehouse);

		assertNotNull(warehouse.archivedAt);
	}

	private static Warehouse warehouse(String businessUnitCode, String location, Integer capacity, Integer stock) {
		Warehouse warehouse = new Warehouse();
		warehouse.businessUnitCode = businessUnitCode;
		warehouse.location = location;
		warehouse.capacity = capacity;
		warehouse.stock = stock;
		return warehouse;
	}

	private static class InMemoryWarehouseStore implements WarehouseStore {
		private final List<Warehouse> warehouses = new ArrayList<>();

		@Override
		public List<Warehouse> getAll() {
			return new ArrayList<>(warehouses);
		}

		@Override
		public void create(Warehouse warehouse) {
			warehouses.add(copy(warehouse));
		}

		@Override
		public void update(Warehouse warehouse) {
			Warehouse existing = warehouses.stream()
					.filter(current -> current.businessUnitCode.equals(warehouse.businessUnitCode))
					.findFirst()
					.orElse(null);
			if (existing == null) {
				return;
			}
			existing.archivedAt = warehouse.archivedAt;
			existing.capacity = warehouse.capacity;
			existing.stock = warehouse.stock;
			existing.location = warehouse.location;
			existing.createdAt = warehouse.createdAt;
		}

		@Override
		public void remove(Warehouse warehouse) {
			warehouses.removeIf(current -> current.businessUnitCode.equals(warehouse.businessUnitCode));
		}

		@Override
		public Warehouse findByBusinessUnitCode(String buCode) {
			return warehouses.stream()
					.filter(current -> current.archivedAt == null)
					.filter(current -> current.businessUnitCode.equals(buCode))
					.findFirst()
					.orElse(null);
		}

		private static Warehouse copy(Warehouse warehouse) {
			Warehouse copy = new Warehouse();
			copy.businessUnitCode = warehouse.businessUnitCode;
			copy.location = warehouse.location;
			copy.capacity = warehouse.capacity;
			copy.stock = warehouse.stock;
			copy.createdAt = warehouse.createdAt;
			copy.archivedAt = warehouse.archivedAt;
			return copy;
		}
	}
}
