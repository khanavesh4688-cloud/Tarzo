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
public final class CallDetectionService_MembersInjector implements MembersInjector<CallDetectionService> {
  private final Provider<TTSManager> ttsManagerProvider;

  public CallDetectionService_MembersInjector(Provider<TTSManager> ttsManagerProvider) {
    this.ttsManagerProvider = ttsManagerProvider;
  }

  public static MembersInjector<CallDetectionService> create(
      Provider<TTSManager> ttsManagerProvider) {
    return new CallDetectionService_MembersInjector(ttsManagerProvider);
  }

  @Override
  public void injectMembers(CallDetectionService instance) {
    injectTtsManager(instance, ttsManagerProvider.get());
  }

  @InjectedFieldSignature("com.tarzo.ai.services.CallDetectionService.ttsManager")
  public static void injectTtsManager(CallDetectionService instance, TTSManager ttsManager) {
    instance.ttsManager = ttsManager;
  }
}
