package com.tarzo.ai.features.translation;

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
public final class TranslationViewModel_Factory implements Factory<TranslationViewModel> {
  private final Provider<TranslationManager> translationManagerProvider;

  public TranslationViewModel_Factory(Provider<TranslationManager> translationManagerProvider) {
    this.translationManagerProvider = translationManagerProvider;
  }

  @Override
  public TranslationViewModel get() {
    return newInstance(translationManagerProvider.get());
  }

  public static TranslationViewModel_Factory create(
      Provider<TranslationManager> translationManagerProvider) {
    return new TranslationViewModel_Factory(translationManagerProvider);
  }

  public static TranslationViewModel newInstance(TranslationManager translationManager) {
    return new TranslationViewModel(translationManager);
  }
}
