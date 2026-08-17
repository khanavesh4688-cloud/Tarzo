package com.tarzo.ai.features.search;

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
public final class SearchViewModel_Factory implements Factory<SearchViewModel> {
  private final Provider<SearchManager> searchManagerProvider;

  public SearchViewModel_Factory(Provider<SearchManager> searchManagerProvider) {
    this.searchManagerProvider = searchManagerProvider;
  }

  @Override
  public SearchViewModel get() {
    return newInstance(searchManagerProvider.get());
  }

  public static SearchViewModel_Factory create(Provider<SearchManager> searchManagerProvider) {
    return new SearchViewModel_Factory(searchManagerProvider);
  }

  public static SearchViewModel newInstance(SearchManager searchManager) {
    return new SearchViewModel(searchManager);
  }
}
