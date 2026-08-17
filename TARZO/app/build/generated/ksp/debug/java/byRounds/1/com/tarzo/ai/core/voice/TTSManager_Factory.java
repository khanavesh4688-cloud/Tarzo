package com.tarzo.ai.core.voice;

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
public final class TTSManager_Factory implements Factory<TTSManager> {
  private final Provider<Context> contextProvider;

  public TTSManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public TTSManager get() {
    return newInstance(contextProvider.get());
  }

  public static TTSManager_Factory create(Provider<Context> contextProvider) {
    return new TTSManager_Factory(contextProvider);
  }

  public static TTSManager newInstance(Context context) {
    return new TTSManager(context);
  }
}
