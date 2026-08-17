package com.tarzo.ai.features.media;

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
public final class MediaControlManager_Factory implements Factory<MediaControlManager> {
  private final Provider<Context> contextProvider;

  public MediaControlManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public MediaControlManager get() {
    return newInstance(contextProvider.get());
  }

  public static MediaControlManager_Factory create(Provider<Context> contextProvider) {
    return new MediaControlManager_Factory(contextProvider);
  }

  public static MediaControlManager newInstance(Context context) {
    return new MediaControlManager(context);
  }
}
