package com.fulfilment.application.monolith.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class LocationGatewayTest {

  @Test
  public void testWhenResolveExistingLocationShouldReturn() {
    LocationGateway locationGateway = new LocationGateway();

    var location = locationGateway.resolveByIdentifier("ZWOLLE-001");

    assertNotNull(location);
    assertEquals("ZWOLLE-001", location.identification);
  }

  @Test
  public void testWhenResolveUnknownLocationShouldReturnNull() {
    LocationGateway locationGateway = new LocationGateway();

    var location = locationGateway.resolveByIdentifier("UNKNOWN-999");

    assertNull(location);
  }

  @Test
  public void testWhenNullIdentifierShouldThrow() {
    LocationGateway locationGateway = new LocationGateway();

    assertThrows(IllegalArgumentException.class, () -> locationGateway.resolveByIdentifier(null));
  }

  @Test
  public void testWhenBlankIdentifierShouldThrow() {
    LocationGateway locationGateway = new LocationGateway();

    assertThrows(IllegalArgumentException.class, () -> locationGateway.resolveByIdentifier("  "));
  }
}
