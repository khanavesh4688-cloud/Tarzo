package com.tarzo.ai.core.storage;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class MemoryDao_Factory implements Factory<MemoryDao> {
  @Override
  public MemoryDao get() {
    return newInstance();
  }

  public static MemoryDao_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static MemoryDao newInstance() {
    return new MemoryDao();
  }

  private static final class InstanceHolder {
    private static final MemoryDao_Factory INSTANCE = new MemoryDao_Factory();
  }
}
