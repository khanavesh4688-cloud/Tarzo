package com.tarzo.ai.features.translation;

import android.content.Context;
import com.tarzo.ai.core.network.ApiClient;
import com.tarzo.ai.core.storage.SecureStorage;
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
public final class TranslationManager_Factory implements Factory<TranslationManager> {
  private final Provider<Context> contextProvider;

  private final Provider<ApiClient> apiClientProvider;

  private final Provider<SecureStorage> secureStorageProvider;

  public TranslationManager_Factory(Provider<Context> contextProvider,
      Provider<ApiClient> apiClientProvider, Provider<SecureStorage> secureStorageProvider) {
    this.contextProvider = contextProvider;
    this.apiClientProvider = apiClientProvider;
    this.secureStorageProvider = secureStorageProvider;
  }

  @Override
  public TranslationManager get() {
    return newInstance(contextProvider.get(), apiClientProvider.get(), secureStorageProvider.get());
  }

  public static TranslationManager_Factory create(Provider<Context> contextProvider,
      Provider<ApiClient> apiClientProvider, Provider<SecureStorage> secureStorageProvider) {
    return new TranslationManager_Factory(contextProvider, apiClientProvider, secureStorageProvider);
  }

  public static TranslationManager newInstance(Context context, ApiClient apiClient,
      SecureStorage secureStorage) {
    return new TranslationManager(context, apiClient, secureStorage);
  }
}
