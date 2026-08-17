package com.tarzo.ai.features.media;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class MediaViewModel_Factory implements Factory<MediaViewModel> {
  private final Provider<MediaControlManager> mediaControlManagerProvider;

  public MediaViewModel_Factory(Provider<MediaControlManager> mediaControlManagerProvider) {
    this.mediaControlManagerProvider = mediaControlManagerProvider;
  }

  @Override
  public MediaViewModel get() {
    return newInstance(mediaControlManagerProvider.get());
  }

  public static MediaViewModel_Factory create(
      Provider<MediaControlManager> mediaControlManagerProvider) {
    return new MediaViewModel_Factory(mediaControlManagerProvider);
  }

  public static MediaViewModel newInstance(MediaControlManager mediaControlManager) {
    return new MediaViewModel(mediaControlManager);
  }
}
