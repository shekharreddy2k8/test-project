package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fulfilment.application.monolith.location.LocationGateway;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.usecases.ArchiveWarehouseUseCase;
import com.fulfilment.application.monolith.warehouses.domain.usecases.CreateWarehouseUseCase;
import com.fulfilment.application.monolith.warehouses.domain.usecases.ReplaceWarehouseUseCase;
import jakarta.ws.rs.WebApplicationException;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WarehouseEndpointTest {

  private WarehouseResourceImpl resource;
  private InMemoryWarehouseRepository repository;

  @BeforeEach
  void setUp() throws Exception {
    repository = new InMemoryWarehouseRepository();
    resource = new WarehouseResourceImpl();

    var createUseCase = new CreateWarehouseUseCase(repository, new LocationGateway());
    var replaceUseCase = new ReplaceWarehouseUseCase(repository, new LocationGateway());
    var archiveUseCase = new ArchiveWarehouseUseCase(repository);

    setField(resource, "warehouseRepository", repository);
    setField(resource, "createWarehouseUseCase", createUseCase);
    setField(resource, "replaceWarehouseUseCase", replaceUseCase);
    setField(resource, "archiveWarehouseUseCase", archiveUseCase);

    repository.create(seed("MWH.001", "ZWOLLE-001", 100, 10));
    repository.create(seed("MWH.012", "AMSTERDAM-001", 50, 5));
    repository.create(seed("MWH.023", "TILBURG-001", 30, 27));
  }

  @Test
  void testListWarehouses() {
    var response = resource.listAllWarehousesUnits();
    assertEquals(3, response.size());
  }

  @Test
  void testGetWarehouseByBusinessUnitCode() {
    var response = resource.getAWarehouseUnitByID("MWH.001");
    assertEquals("MWH.001", response.getBusinessUnitCode());
    assertEquals("ZWOLLE-001", response.getLocation());
  }

  @Test
  void testCreateWarehouseShouldValidateStockAgainstCapacity() {
    var payload = new com.warehouse.api.beans.Warehouse();
    payload.setBusinessUnitCode("MWH.100");
    payload.setLocation("AMSTERDAM-001");
    payload.setCapacity(10);
    payload.setStock(99);

    WebApplicationException exception =
        assertThrows(WebApplicationException.class, () -> resource.createANewWarehouseUnit(payload));
    assertEquals(400, exception.getResponse().getStatus());
  }

  @Test
  void testCreateWarehouseShouldSucceed() {
    var payload = new com.warehouse.api.beans.Warehouse();
    payload.setBusinessUnitCode("MWH.299");
    payload.setLocation("AMSTERDAM-001");
    payload.setCapacity(20);
    payload.setStock(5);

    var response = resource.createANewWarehouseUnit(payload);
    assertEquals("MWH.299", response.getBusinessUnitCode());
    assertNotNull(repository.findByBusinessUnitCode("MWH.299"));
  }

  @Test
  void testReplaceWarehouseShouldValidateStockMatching() {
    var payload = new com.warehouse.api.beans.Warehouse();
    payload.setLocation("ZWOLLE-001");
    payload.setCapacity(15);
    payload.setStock(9);

    WebApplicationException exception =
        assertThrows(
            WebApplicationException.class,
            () -> resource.replaceTheCurrentActiveWarehouse("MWH.001", payload));
    assertEquals(400, exception.getResponse().getStatus());
  }

  @Test
  void testArchiveWarehouseShouldHideFromActiveLookup() {
    resource.archiveAWarehouseUnitByID("MWH.023");

    WebApplicationException exception =
        assertThrows(WebApplicationException.class, () -> resource.getAWarehouseUnitByID("MWH.023"));
    assertEquals(404, exception.getResponse().getStatus());
  }

  private static Warehouse seed(String buCode, String location, Integer capacity, Integer stock) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = buCode;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    warehouse.createdAt = LocalDateTime.now().minusDays(1);
    return warehouse;
  }

  private static void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private static class InMemoryWarehouseRepository extends WarehouseRepository {

    private final List<Warehouse> warehouses = new ArrayList<>();

    @Override
    public List<Warehouse> getAll() {
      return warehouses.stream().filter(current -> current.archivedAt == null).toList();
    }

    @Override
    public void create(Warehouse warehouse) {
      warehouses.add(copy(warehouse));
    }

    @Override
    public void update(Warehouse warehouse) {
      Warehouse current = findByBusinessUnitCode(warehouse.businessUnitCode);
      if (current == null) {
        return;
      }
      current.location = warehouse.location;
      current.capacity = warehouse.capacity;
      current.stock = warehouse.stock;
      current.createdAt = warehouse.createdAt;
      current.archivedAt = warehouse.archivedAt;
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

    private static Warehouse copy(Warehouse source) {
      Warehouse copy = new Warehouse();
      copy.businessUnitCode = source.businessUnitCode;
      copy.location = source.location;
      copy.capacity = source.capacity;
      copy.stock = source.stock;
      copy.createdAt = source.createdAt;
      copy.archivedAt = source.archivedAt;
      return copy;
    }
  }
}
