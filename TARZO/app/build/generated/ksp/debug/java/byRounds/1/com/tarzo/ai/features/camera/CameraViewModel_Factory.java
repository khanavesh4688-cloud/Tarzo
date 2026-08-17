package com.tarzo.ai.features.camera;

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
public final class CameraViewModel_Factory implements Factory<CameraViewModel> {
  private final Provider<CameraManager> cameraManagerProvider;

  public CameraViewModel_Factory(Provider<CameraManager> cameraManagerProvider) {
    this.cameraManagerProvider = cameraManagerProvider;
  }

  @Override
  public CameraViewModel get() {
    return newInstance(cameraManagerProvider.get());
  }

  public static CameraViewModel_Factory create(Provider<CameraManager> cameraManagerProvider) {
    return new CameraViewModel_Factory(cameraManagerProvider);
  }

  public static CameraViewModel newInstance(CameraManager cameraManager) {
    return new CameraViewModel(cameraManager);
  }
}
