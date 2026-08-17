package com.tarzo.ai.core.storage;

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
public final class MemoryManager_Factory implements Factory<MemoryManager> {
  private final Provider<MemoryDao> memoryDaoProvider;

  public MemoryManager_Factory(Provider<MemoryDao> memoryDaoProvider) {
    this.memoryDaoProvider = memoryDaoProvider;
  }

  @Override
  public MemoryManager get() {
    return newInstance(memoryDaoProvider.get());
  }

  public static MemoryManager_Factory create(Provider<MemoryDao> memoryDaoProvider) {
    return new MemoryManager_Factory(memoryDaoProvider);
  }

  public static MemoryManager newInstance(MemoryDao memoryDao) {
    return new MemoryManager(memoryDao);
  }
}
