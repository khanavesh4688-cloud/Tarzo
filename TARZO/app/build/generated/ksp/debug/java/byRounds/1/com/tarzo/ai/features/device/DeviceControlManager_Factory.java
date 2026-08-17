package com.tarzo.ai.features.device;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class DeviceControlManager_Factory implements Factory<DeviceControlManager> {
  private final Provider<Context> contextProvider;

  public DeviceControlManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public DeviceControlManager get() {
    return newInstance(contextProvider.get());
  }

  public static DeviceControlManager_Factory create(Provider<Context> contextProvider) {
    return new DeviceControlManager_Factory(contextProvider);
  }

  public static DeviceControlManager newInstance(Context context) {
    return new DeviceControlManager(context);
  }
}
