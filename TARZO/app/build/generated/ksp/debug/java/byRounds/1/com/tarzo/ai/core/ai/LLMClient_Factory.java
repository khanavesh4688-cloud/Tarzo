package com.tarzo.ai.core.ai;

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
public final class LLMClient_Factory implements Factory<LLMClient> {
  private final Provider<SecureStorage> secureStorageProvider;

  public LLMClient_Factory(Provider<SecureStorage> secureStorageProvider) {
    this.secureStorageProvider = secureStorageProvider;
  }

  @Override
  public LLMClient get() {
    return newInstance(secureStorageProvider.get());
  }

  public static LLMClient_Factory create(Provider<SecureStorage> secureStorageProvider) {
    return new LLMClient_Factory(secureStorageProvider);
  }

  public static LLMClient newInstance(SecureStorage secureStorage) {
    return new LLMClient(secureStorage);
  }
}
