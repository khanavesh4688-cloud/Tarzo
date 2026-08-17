package com.tarzo.ai.features.communication;

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
public final class CommunicationViewModel_Factory implements Factory<CommunicationViewModel> {
  private final Provider<CallManager> callManagerProvider;

  private final Provider<SmsManager> smsManagerProvider;

  public CommunicationViewModel_Factory(Provider<CallManager> callManagerProvider,
      Provider<SmsManager> smsManagerProvider) {
    this.callManagerProvider = callManagerProvider;
    this.smsManagerProvider = smsManagerProvider;
  }

  @Override
  public CommunicationViewModel get() {
    return newInstance(callManagerProvider.get(), smsManagerProvider.get());
  }

  public static CommunicationViewModel_Factory create(Provider<CallManager> callManagerProvider,
      Provider<SmsManager> smsManagerProvider) {
    return new CommunicationViewModel_Factory(callManagerProvider, smsManagerProvider);
  }

  public static CommunicationViewModel newInstance(CallManager callManager, SmsManager smsManager) {
    return new CommunicationViewModel(callManager, smsManager);
  }
}
