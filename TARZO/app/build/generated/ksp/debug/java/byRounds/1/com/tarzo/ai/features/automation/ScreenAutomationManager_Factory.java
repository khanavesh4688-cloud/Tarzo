package com.tarzo.ai.features.automation;

import android.content.Context;
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
public final class ScreenAutomationManager_Factory implements Factory<ScreenAutomationManager> {
  private final Provider<Context> contextProvider;

  public ScreenAutomationManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ScreenAutomationManager get() {
    return newInstance(contextProvider.get());
  }

  public static ScreenAutomationManager_Factory create(Provider<Context> contextProvider) {
    return new ScreenAutomationManager_Factory(contextProvider);
  }

  public static ScreenAutomationManager newInstance(Context context) {
    return new ScreenAutomationManager(context);
  }
}
