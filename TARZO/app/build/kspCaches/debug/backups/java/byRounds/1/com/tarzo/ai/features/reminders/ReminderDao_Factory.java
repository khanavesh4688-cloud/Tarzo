package com.tarzo.ai.features.reminders;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class ReminderDao_Factory implements Factory<ReminderDao> {
  @Override
  public ReminderDao get() {
    return newInstance();
  }

  public static ReminderDao_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ReminderDao newInstance() {
    return new ReminderDao();
  }

  private static final class InstanceHolder {
    private static final ReminderDao_Factory INSTANCE = new ReminderDao_Factory();
  }
}
