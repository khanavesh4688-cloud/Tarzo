package com.tarzo.ai.core.ai;

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
public final class IntentDetector_Factory implements Factory<IntentDetector> {
  @Override
  public IntentDetector get() {
    return newInstance();
  }

  public static IntentDetector_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static IntentDetector newInstance() {
    return new IntentDetector();
  }

  private static final class InstanceHolder {
    private static final IntentDetector_Factory INSTANCE = new IntentDetector_Factory();
  }
}
