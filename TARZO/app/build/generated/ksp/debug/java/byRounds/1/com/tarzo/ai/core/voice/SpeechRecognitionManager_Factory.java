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
public final class SpeechRecognitionManager_Factory implements Factory<SpeechRecognitionManager> {
  private final Provider<Context> contextProvider;

  public SpeechRecognitionManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public SpeechRecognitionManager get() {
    return newInstance(contextProvider.get());
  }

  public static SpeechRecognitionManager_Factory create(Provider<Context> contextProvider) {
    return new SpeechRecognitionManager_Factory(contextProvider);
  }

  public static SpeechRecognitionManager newInstance(Context context) {
    return new SpeechRecognitionManager(context);
  }
}
