package com.tarzo.ai.features.security;

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
public final class SecurityViewModel_Factory implements Factory<SecurityViewModel> {
  private final Provider<AntiTheftManager> antiTheftManagerProvider;

  public SecurityViewModel_Factory(Provider<AntiTheftManager> antiTheftManagerProvider) {
    this.antiTheftManagerProvider = antiTheftManagerProvider;
  }

  @Override
  public SecurityViewModel get() {
    return newInstance(antiTheftManagerProvider.get());
  }

  public static SecurityViewModel_Factory create(
      Provider<AntiTheftManager> antiTheftManagerProvider) {
    return new SecurityViewModel_Factory(antiTheftManagerProvider);
  }

  public static SecurityViewModel newInstance(AntiTheftManager antiTheftManager) {
    return new SecurityViewModel(antiTheftManager);
  }
}
