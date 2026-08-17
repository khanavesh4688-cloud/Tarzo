package com.tarzo.ai;

import com.tarzo.ai.core.permissions.PermissionManager;
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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<PermissionManager> permissionManagerProvider;

  public MainActivity_MembersInjector(Provider<PermissionManager> permissionManagerProvider) {
    this.permissionManagerProvider = permissionManagerProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<PermissionManager> permissionManagerProvider) {
    return new MainActivity_MembersInjector(permissionManagerProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectPermissionManager(instance, permissionManagerProvider.get());
  }

  @InjectedFieldSignature("com.tarzo.ai.MainActivity.permissionManager")
  public static void injectPermissionManager(MainActivity instance,
      PermissionManager permissionManager) {
    instance.permissionManager = permissionManager;
  }
}
