package com.tarzo.ai;

import androidx.hilt.work.HiltWorkerFactory;
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
public final class TarzoApp_MembersInjector implements MembersInjector<TarzoApp> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  public TarzoApp_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
  }

  public static MembersInjector<TarzoApp> create(
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new TarzoApp_MembersInjector(workerFactoryProvider);
  }

  @Override
  public void injectMembers(TarzoApp instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
  }

  @InjectedFieldSignature("com.tarzo.ai.TarzoApp.workerFactory")
  public static void injectWorkerFactory(TarzoApp instance, HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }
}
