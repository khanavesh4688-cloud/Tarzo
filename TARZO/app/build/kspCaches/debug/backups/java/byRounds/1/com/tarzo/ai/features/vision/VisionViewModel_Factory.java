package com.tarzo.ai.features.vision;

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
public final class VisionViewModel_Factory implements Factory<VisionViewModel> {
  private final Provider<VisionAnalyzer> visionAnalyzerProvider;

  public VisionViewModel_Factory(Provider<VisionAnalyzer> visionAnalyzerProvider) {
    this.visionAnalyzerProvider = visionAnalyzerProvider;
  }

  @Override
  public VisionViewModel get() {
    return newInstance(visionAnalyzerProvider.get());
  }

  public static VisionViewModel_Factory create(Provider<VisionAnalyzer> visionAnalyzerProvider) {
    return new VisionViewModel_Factory(visionAnalyzerProvider);
  }

  public static VisionViewModel newInstance(VisionAnalyzer visionAnalyzer) {
    return new VisionViewModel(visionAnalyzer);
  }
}
