package com.tarzo.ai.features.device;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class DeviceControlViewModel_Factory implements Factory<DeviceControlViewModel> {
  private final Provider<DeviceControlManager> managerProvider;

  public DeviceControlViewModel_Factory(Provider<DeviceControlManager> managerProvider) {
    this.managerProvider = managerProvider;
  }

  @Override
  public DeviceControlViewModel get() {
    return newInstance(managerProvider.get());
  }

  public static DeviceControlViewModel_Factory create(
      Provider<DeviceControlManager> managerProvider) {
    return new DeviceControlViewModel_Factory(managerProvider);
  }

  public static DeviceControlViewModel newInstance(DeviceControlManager manager) {
    return new DeviceControlViewModel(manager);
  }
}
