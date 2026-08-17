package com.tarzo.ai.features.reminders;

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
public final class ReminderViewModel_Factory implements Factory<ReminderViewModel> {
  private final Provider<ReminderManager> reminderManagerProvider;

  public ReminderViewModel_Factory(Provider<ReminderManager> reminderManagerProvider) {
    this.reminderManagerProvider = reminderManagerProvider;
  }

  @Override
  public ReminderViewModel get() {
    return newInstance(reminderManagerProvider.get());
  }

  public static ReminderViewModel_Factory create(
      Provider<ReminderManager> reminderManagerProvider) {
    return new ReminderViewModel_Factory(reminderManagerProvider);
  }

  public static ReminderViewModel newInstance(ReminderManager reminderManager) {
    return new ReminderViewModel(reminderManager);
  }
}
