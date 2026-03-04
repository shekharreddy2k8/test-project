package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CreateWarehouseUseCaseTest {

	private InMemoryWarehouseStore warehouseStore;
	private CreateWarehouseUseCase useCase;

	@BeforeEach
	void setUp() {
		warehouseStore = new InMemoryWarehouseStore();
		useCase = new CreateWarehouseUseCase(warehouseStore, new FixedLocationResolver());
	}

	@Test
	void shouldCreateWarehouseWhenPayloadIsValid() {
		Warehouse warehouse = warehouse("MWH.500", "ZWOLLE-001", 20, 8);

		assertDoesNotThrow(() -> useCase.create(warehouse));

		Warehouse created = warehouseStore.findByBusinessUnitCode("MWH.500");
		assertNotNull(created);
		assertNotNull(created.createdAt);
		assertEquals("ZWOLLE-001", created.location);
	}

	@Test
	void shouldRejectWhenBusinessUnitCodeAlreadyExists() {
		Warehouse existing = warehouse("MWH.500", "ZWOLLE-001", 20, 8);
		warehouseStore.create(existing);

		Warehouse duplicate = warehouse("MWH.500", "ZWOLLE-001", 15, 4);

		assertThrows(IllegalArgumentException.class, () -> useCase.create(duplicate));
	}

	@Test
	void shouldRejectWhenLocationDoesNotExist() {
		Warehouse warehouse = warehouse("MWH.501", "UNKNOWN", 20, 5);

		assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
	}

	@Test
	void shouldRejectWhenStockIsGreaterThanCapacity() {
		Warehouse warehouse = warehouse("MWH.502", "ZWOLLE-001", 10, 11);

		assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
	}

	private static Warehouse warehouse(String businessUnitCode, String location, Integer capacity, Integer stock) {
		Warehouse warehouse = new Warehouse();
		warehouse.businessUnitCode = businessUnitCode;
		warehouse.location = location;
		warehouse.capacity = capacity;
		warehouse.stock = stock;
		return warehouse;
	}

	private static class FixedLocationResolver implements LocationResolver {

		@Override
		public Location resolveByIdentifier(String identifier) {
			if ("ZWOLLE-001".equals(identifier)) {
				return new Location("ZWOLLE-001", 2, 40);
			}
			if ("AMSTERDAM-001".equals(identifier)) {
				return new Location("AMSTERDAM-001", 5, 100);
			}
			return null;
		}
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
			Warehouse existing = findByBusinessUnitCode(warehouse.businessUnitCode);
			if (existing == null) {
				return;
			}
			existing.location = warehouse.location;
			existing.capacity = warehouse.capacity;
			existing.stock = warehouse.stock;
			existing.createdAt = warehouse.createdAt;
			existing.archivedAt = warehouse.archivedAt;
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
