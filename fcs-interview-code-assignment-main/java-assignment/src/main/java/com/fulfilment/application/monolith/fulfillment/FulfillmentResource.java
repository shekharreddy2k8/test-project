package com.fulfilment.application.monolith.fulfillment;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * REST resource for managing warehouse fulfilment assignments.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code POST   /fulfillment}                            – Assign a warehouse to a product/store</li>
 *   <li>{@code GET    /fulfillment}                            – List all assignments</li>
 *   <li>{@code GET    /fulfillment/store/{storeId}}            – List assignments for a store</li>
 *   <li>{@code GET    /fulfillment/warehouse/{warehouseCode}}  – List assignments for a warehouse</li>
 *   <li>{@code DELETE /fulfillment/{id}}                       – Remove an assignment</li>
 * </ul>
 */
@Path("/fulfillment")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class FulfillmentResource {

  @Inject FulfillmentRepository repository;
  @Inject AssignFulfillmentUseCase assignUseCase;

  @POST
  public Response assign(FulfillmentRequest request) {
    try {
      var assignment = assignUseCase.assign(
          request.warehouseCode(), request.productId(), request.storeId());
      return Response.status(Response.Status.CREATED).entity(assignment).build();
    } catch (IllegalArgumentException ex) {
      throw new WebApplicationException(ex.getMessage(), Response.Status.BAD_REQUEST);
    }
  }

  @GET
  public List<FulfillmentAssignment> listAll() {
    return repository.listAll();
  }

  @GET
  @Path("/store/{storeId}")
  public List<FulfillmentAssignment> byStore(@PathParam("storeId") Long storeId) {
    return repository.findByStoreId(storeId);
  }

  @GET
  @Path("/warehouse/{warehouseCode}")
  public List<FulfillmentAssignment> byWarehouse(
      @PathParam("warehouseCode") String warehouseCode) {
    return repository.findByWarehouseCode(warehouseCode);
  }

  @DELETE
  @Path("/{id}")
  @Transactional
  public Response remove(@PathParam("id") Long id) {
    var assignment = repository.findById(id);
    if (assignment == null) {
      throw new WebApplicationException("Assignment not found", Response.Status.NOT_FOUND);
    }
    repository.delete(assignment);
    return Response.noContent().build();
  }

  /** Immutable request payload for creating a fulfilment assignment. */
  public record FulfillmentRequest(String warehouseCode, Long productId, Long storeId) {}
}
