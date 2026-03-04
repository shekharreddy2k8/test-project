package com.fulfilment.application.monolith.stores;

public class StoreSyncEvent {

  public enum Action {
    CREATE,
    UPDATE
  }

  private final Store store;
  private final Action action;

  private StoreSyncEvent(Store store, Action action) {
    this.store = store;
    this.action = action;
  }

  public static StoreSyncEvent create(Store store, Action action) {
    return new StoreSyncEvent(store, action);
  }

  public Store store() {
    return store;
  }

  public Action action() {
    return action;
  }
}
