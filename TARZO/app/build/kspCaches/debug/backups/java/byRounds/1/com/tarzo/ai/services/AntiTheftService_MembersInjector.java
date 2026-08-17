package com.tarzo.ai.services;

import com.tarzo.ai.core.voice.TTSManager;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class AntiTheftService_MembersInjector implements MembersInjector<AntiTheftService> {
  private final Provider<TTSManager> ttsManagerProvider;

  public AntiTheftService_MembersInjector(Provider<TTSManager> ttsManagerProvider) {
    this.ttsManagerProvider = ttsManagerProvider;
  }

  public static MembersInjector<AntiTheftService> create(Provider<TTSManager> ttsManagerProvider) {
    return new AntiTheftService_MembersInjector(ttsManagerProvider);
  }

  @Override
  public void injectMembers(AntiTheftService instance) {
    injectTtsManager(instance, ttsManagerProvider.get());
  }

  @InjectedFieldSignature("com.tarzo.ai.services.AntiTheftService.ttsManager")
  public static void injectTtsManager(AntiTheftService instance, TTSManager ttsManager) {
    instance.ttsManager = ttsManager;
  }
}
