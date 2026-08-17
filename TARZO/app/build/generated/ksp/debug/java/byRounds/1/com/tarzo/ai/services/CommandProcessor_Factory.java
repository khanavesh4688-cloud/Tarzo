package com.tarzo.ai.services;

import android.content.Context;
import com.tarzo.ai.core.storage.MemoryManager;
import com.tarzo.ai.features.communication.CallManager;
import com.tarzo.ai.features.communication.SmsManager;
import com.tarzo.ai.features.device.DeviceControlManager;
import com.tarzo.ai.features.reminders.ReminderManager;
import com.tarzo.ai.features.search.SearchManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class CommandProcessor_Factory implements Factory<CommandProcessor> {
  private final Provider<DeviceControlManager> deviceControlManagerProvider;

  private final Provider<CallManager> callManagerProvider;

  private final Provider<SmsManager> smsManagerProvider;

  private final Provider<ReminderManager> reminderManagerProvider;

  private final Provider<MemoryManager> memoryManagerProvider;

  private final Provider<SearchManager> searchManagerProvider;

  private final Provider<Context> appContextProvider;

  public CommandProcessor_Factory(Provider<DeviceControlManager> deviceControlManagerProvider,
      Provider<CallManager> callManagerProvider, Provider<SmsManager> smsManagerProvider,
      Provider<ReminderManager> reminderManagerProvider,
      Provider<MemoryManager> memoryManagerProvider, Provider<SearchManager> searchManagerProvider,
      Provider<Context> appContextProvider) {
    this.deviceControlManagerProvider = deviceControlManagerProvider;
    this.callManagerProvider = callManagerProvider;
    this.smsManagerProvider = smsManagerProvider;
    this.reminderManagerProvider = reminderManagerProvider;
    this.memoryManagerProvider = memoryManagerProvider;
    this.searchManagerProvider = searchManagerProvider;
    this.appContextProvider = appContextProvider;
  }

  @Override
  public CommandProcessor get() {
    return newInstance(deviceControlManagerProvider.get(), callManagerProvider.get(), smsManagerProvider.get(), reminderManagerProvider.get(), memoryManagerProvider.get(), searchManagerProvider.get(), appContextProvider.get());
  }

  public static CommandProcessor_Factory create(
      Provider<DeviceControlManager> deviceControlManagerProvider,
      Provider<CallManager> callManagerProvider, Provider<SmsManager> smsManagerProvider,
      Provider<ReminderManager> reminderManagerProvider,
      Provider<MemoryManager> memoryManagerProvider, Provider<SearchManager> searchManagerProvider,
      Provider<Context> appContextProvider) {
    return new CommandProcessor_Factory(deviceControlManagerProvider, callManagerProvider, smsManagerProvider, reminderManagerProvider, memoryManagerProvider, searchManagerProvider, appContextProvider);
  }

  public static CommandProcessor newInstance(DeviceControlManager deviceControlManager,
      CallManager callManager, SmsManager smsManager, ReminderManager reminderManager,
      MemoryManager memoryManager, SearchManager searchManager, Context appContext) {
    return new CommandProcessor(deviceControlManager, callManager, smsManager, reminderManager, memoryManager, searchManager, appContext);
  }
}
