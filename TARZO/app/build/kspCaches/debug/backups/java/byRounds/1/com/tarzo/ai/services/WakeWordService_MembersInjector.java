package com.tarzo.ai.services;

import com.tarzo.ai.core.voice.TTSManager;
import com.tarzo.ai.core.voice.WakeWordEngine;
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
public final class WakeWordService_MembersInjector implements MembersInjector<WakeWordService> {
  private final Provider<WakeWordEngine> wakeWordEngineProvider;

  private final Provider<TTSManager> ttsManagerProvider;

  public WakeWordService_MembersInjector(Provider<WakeWordEngine> wakeWordEngineProvider,
      Provider<TTSManager> ttsManagerProvider) {
    this.wakeWordEngineProvider = wakeWordEngineProvider;
    this.ttsManagerProvider = ttsManagerProvider;
  }

  public static MembersInjector<WakeWordService> create(
      Provider<WakeWordEngine> wakeWordEngineProvider, Provider<TTSManager> ttsManagerProvider) {
    return new WakeWordService_MembersInjector(wakeWordEngineProvider, ttsManagerProvider);
  }

  @Override
  public void injectMembers(WakeWordService instance) {
    injectWakeWordEngine(instance, wakeWordEngineProvider.get());
    injectTtsManager(instance, ttsManagerProvider.get());
  }

  @InjectedFieldSignature("com.tarzo.ai.services.WakeWordService.wakeWordEngine")
  public static void injectWakeWordEngine(WakeWordService instance, WakeWordEngine wakeWordEngine) {
    instance.wakeWordEngine = wakeWordEngine;
  }

  @InjectedFieldSignature("com.tarzo.ai.services.WakeWordService.ttsManager")
  public static void injectTtsManager(WakeWordService instance, TTSManager ttsManager) {
    instance.ttsManager = ttsManager;
  }
}
