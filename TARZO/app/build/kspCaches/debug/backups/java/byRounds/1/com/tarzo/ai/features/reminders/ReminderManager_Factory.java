package com.tarzo.ai.features.reminders;

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
public final class ReminderManager_Factory implements Factory<ReminderManager> {
  private final Provider<Context> contextProvider;

  private final Provider<ReminderDao> reminderDaoProvider;

  public ReminderManager_Factory(Provider<Context> contextProvider,
      Provider<ReminderDao> reminderDaoProvider) {
    this.contextProvider = contextProvider;
    this.reminderDaoProvider = reminderDaoProvider;
  }

  @Override
  public ReminderManager get() {
    return newInstance(contextProvider.get(), reminderDaoProvider.get());
  }

  public static ReminderManager_Factory create(Provider<Context> contextProvider,
      Provider<ReminderDao> reminderDaoProvider) {
    return new ReminderManager_Factory(contextProvider, reminderDaoProvider);
  }

  public static ReminderManager newInstance(Context context, ReminderDao reminderDao) {
    return new ReminderManager(context, reminderDao);
  }
}
