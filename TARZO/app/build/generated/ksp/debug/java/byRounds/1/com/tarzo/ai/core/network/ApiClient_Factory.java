package com.tarzo.ai.core.network;

import com.tarzo.ai.core.storage.SecureStorage;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class ApiClient_Factory implements Factory<ApiClient> {
  private final Provider<SecureStorage> secureStorageProvider;

  public ApiClient_Factory(Provider<SecureStorage> secureStorageProvider) {
    this.secureStorageProvider = secureStorageProvider;
  }

  @Override
  public ApiClient get() {
    return newInstance(secureStorageProvider.get());
  }

  public static ApiClient_Factory create(Provider<SecureStorage> secureStorageProvider) {
    return new ApiClient_Factory(secureStorageProvider);
  }

  public static ApiClient newInstance(SecureStorage secureStorage) {
    return new ApiClient(secureStorage);
  }
}
