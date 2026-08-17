package com.tarzo.ai.features.security;

import android.content.Context;
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
public final class AntiTheftManager_Factory implements Factory<AntiTheftManager> {
  private final Provider<Context> contextProvider;

  private final Provider<SecureStorage> secureStorageProvider;

  public AntiTheftManager_Factory(Provider<Context> contextProvider,
      Provider<SecureStorage> secureStorageProvider) {
    this.contextProvider = contextProvider;
    this.secureStorageProvider = secureStorageProvider;
  }

  @Override
  public AntiTheftManager get() {
    return newInstance(contextProvider.get(), secureStorageProvider.get());
  }

  public static AntiTheftManager_Factory create(Provider<Context> contextProvider,
      Provider<SecureStorage> secureStorageProvider) {
    return new AntiTheftManager_Factory(contextProvider, secureStorageProvider);
  }

  public static AntiTheftManager newInstance(Context context, SecureStorage secureStorage) {
    return new AntiTheftManager(context, secureStorage);
  }
}
