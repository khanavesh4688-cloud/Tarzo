package com.tarzo.ai.features.search;

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
public final class SearchManager_Factory implements Factory<SearchManager> {
  private final Provider<Context> contextProvider;

  private final Provider<SecureStorage> secureStorageProvider;

  private final Provider<ApiClient> apiClientProvider;

  public SearchManager_Factory(Provider<Context> contextProvider,
      Provider<SecureStorage> secureStorageProvider, Provider<ApiClient> apiClientProvider) {
    this.contextProvider = contextProvider;
    this.secureStorageProvider = secureStorageProvider;
    this.apiClientProvider = apiClientProvider;
  }

  @Override
  public SearchManager get() {
    return newInstance(contextProvider.get(), secureStorageProvider.get(), apiClientProvider.get());
  }

  public static SearchManager_Factory create(Provider<Context> contextProvider,
      Provider<SecureStorage> secureStorageProvider, Provider<ApiClient> apiClientProvider) {
    return new SearchManager_Factory(contextProvider, secureStorageProvider, apiClientProvider);
  }

  public static SearchManager newInstance(Context context, SecureStorage secureStorage,
      ApiClient apiClient) {
    return new SearchManager(context, secureStorage, apiClient);
  }
}
