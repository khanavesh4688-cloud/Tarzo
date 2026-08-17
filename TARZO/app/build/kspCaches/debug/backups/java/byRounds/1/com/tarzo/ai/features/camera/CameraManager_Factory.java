package com.tarzo.ai.features.camera;

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
public final class CameraManager_Factory implements Factory<CameraManager> {
  private final Provider<Context> contextProvider;

  public CameraManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public CameraManager get() {
    return newInstance(contextProvider.get());
  }

  public static CameraManager_Factory create(Provider<Context> contextProvider) {
    return new CameraManager_Factory(contextProvider);
  }

  public static CameraManager newInstance(Context context) {
    return new CameraManager(context);
  }
}
