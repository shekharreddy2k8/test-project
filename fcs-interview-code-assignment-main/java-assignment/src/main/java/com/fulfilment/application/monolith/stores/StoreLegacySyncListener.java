package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;

@ApplicationScoped
public class StoreLegacySyncListener {

  @Inject LegacyStoreManagerGateway legacyStoreManagerGateway;

  public void onStoreChanged(@Observes(during = TransactionPhase.AFTER_SUCCESS) StoreSyncEvent event) {
    if (event.action() == StoreSyncEvent.Action.CREATE) {
      legacyStoreManagerGateway.createStoreOnLegacySystem(event.store());
      return;
    }

    legacyStoreManagerGateway.updateStoreOnLegacySystem(event.store());
  }
}
