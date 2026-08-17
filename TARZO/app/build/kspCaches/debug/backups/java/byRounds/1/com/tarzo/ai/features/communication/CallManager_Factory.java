package com.tarzo.ai.features.communication;

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
public final class CallManager_Factory implements Factory<CallManager> {
  private final Provider<Context> contextProvider;

  public CallManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public CallManager get() {
    return newInstance(contextProvider.get());
  }

  public static CallManager_Factory create(Provider<Context> contextProvider) {
    return new CallManager_Factory(contextProvider);
  }

  public static CallManager newInstance(Context context) {
    return new CallManager(context);
  }
}
