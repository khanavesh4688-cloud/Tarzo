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
public final class SmsManager_Factory implements Factory<SmsManager> {
  private final Provider<Context> contextProvider;

  public SmsManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public SmsManager get() {
    return newInstance(contextProvider.get());
  }

  public static SmsManager_Factory create(Provider<Context> contextProvider) {
    return new SmsManager_Factory(contextProvider);
  }

  public static SmsManager newInstance(Context context) {
    return new SmsManager(context);
  }
}
