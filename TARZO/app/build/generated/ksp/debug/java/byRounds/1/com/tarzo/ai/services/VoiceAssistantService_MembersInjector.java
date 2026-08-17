package com.tarzo.ai.services;

import com.tarzo.ai.core.ai.IntentDetector;
import com.tarzo.ai.core.ai.LLMClient;
import com.tarzo.ai.core.storage.MemoryManager;
import com.tarzo.ai.core.voice.SpeechRecognitionManager;
import com.tarzo.ai.core.voice.TTSManager;
import com.tarzo.ai.core.voice.WakeWordEngine;
import com.tarzo.ai.features.communication.CallManager;
import com.tarzo.ai.features.communication.SmsManager;
import com.tarzo.ai.features.device.DeviceControlManager;
import com.tarzo.ai.features.reminders.ReminderManager;
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
public final class VoiceAssistantService_MembersInjector implements MembersInjector<VoiceAssistantService> {
  private final Provider<WakeWordEngine> wakeWordEngineProvider;

  private final Provider<SpeechRecognitionManager> speechRecognitionManagerProvider;

  private final Provider<TTSManager> ttsManagerProvider;

  private final Provider<IntentDetector> intentDetectorProvider;

  private final Provider<LLMClient> llmClientProvider;

  private final Provider<MemoryManager> memoryManagerProvider;

  private final Provider<DeviceControlManager> deviceControlManagerProvider;

  private final Provider<CallManager> callManagerProvider;

  private final Provider<SmsManager> smsManagerProvider;

  private final Provider<ReminderManager> reminderManagerProvider;

  private final Provider<CommandProcessor> commandProcessorProvider;

  public VoiceAssistantService_MembersInjector(Provider<WakeWordEngine> wakeWordEngineProvider,
      Provider<SpeechRecognitionManager> speechRecognitionManagerProvider,
      Provider<TTSManager> ttsManagerProvider, Provider<IntentDetector> intentDetectorProvider,
      Provider<LLMClient> llmClientProvider, Provider<MemoryManager> memoryManagerProvider,
      Provider<DeviceControlManager> deviceControlManagerProvider,
      Provider<CallManager> callManagerProvider, Provider<SmsManager> smsManagerProvider,
      Provider<ReminderManager> reminderManagerProvider,
      Provider<CommandProcessor> commandProcessorProvider) {
    this.wakeWordEngineProvider = wakeWordEngineProvider;
    this.speechRecognitionManagerProvider = speechRecognitionManagerProvider;
    this.ttsManagerProvider = ttsManagerProvider;
    this.intentDetectorProvider = intentDetectorProvider;
    this.llmClientProvider = llmClientProvider;
    this.memoryManagerProvider = memoryManagerProvider;
    this.deviceControlManagerProvider = deviceControlManagerProvider;
    this.callManagerProvider = callManagerProvider;
    this.smsManagerProvider = smsManagerProvider;
    this.reminderManagerProvider = reminderManagerProvider;
    this.commandProcessorProvider = commandProcessorProvider;
  }

  public static MembersInjector<VoiceAssistantService> create(
      Provider<WakeWordEngine> wakeWordEngineProvider,
      Provider<SpeechRecognitionManager> speechRecognitionManagerProvider,
      Provider<TTSManager> ttsManagerProvider, Provider<IntentDetector> intentDetectorProvider,
      Provider<LLMClient> llmClientProvider, Provider<MemoryManager> memoryManagerProvider,
      Provider<DeviceControlManager> deviceControlManagerProvider,
      Provider<CallManager> callManagerProvider, Provider<SmsManager> smsManagerProvider,
      Provider<ReminderManager> reminderManagerProvider,
      Provider<CommandProcessor> commandProcessorProvider) {
    return new VoiceAssistantService_MembersInjector(wakeWordEngineProvider, speechRecognitionManagerProvider, ttsManagerProvider, intentDetectorProvider, llmClientProvider, memoryManagerProvider, deviceControlManagerProvider, callManagerProvider, smsManagerProvider, reminderManagerProvider, commandProcessorProvider);
  }

  @Override
  public void injectMembers(VoiceAssistantService instance) {
    injectWakeWordEngine(instance, wakeWordEngineProvider.get());
    injectSpeechRecognitionManager(instance, speechRecognitionManagerProvider.get());
    injectTtsManager(instance, ttsManagerProvider.get());
    injectIntentDetector(instance, intentDetectorProvider.get());
    injectLlmClient(instance, llmClientProvider.get());
    injectMemoryManager(instance, memoryManagerProvider.get());
    injectDeviceControlManager(instance, deviceControlManagerProvider.get());
    injectCallManager(instance, callManagerProvider.get());
    injectSmsManager(instance, smsManagerProvider.get());
    injectReminderManager(instance, reminderManagerProvider.get());
    injectCommandProcessor(instance, commandProcessorProvider.get());
  }

  @InjectedFieldSignature("com.tarzo.ai.services.VoiceAssistantService.wakeWordEngine")
  public static void injectWakeWordEngine(VoiceAssistantService instance,
      WakeWordEngine wakeWordEngine) {
    instance.wakeWordEngine = wakeWordEngine;
  }

  @InjectedFieldSignature("com.tarzo.ai.services.VoiceAssistantService.speechRecognitionManager")
  public static void injectSpeechRecognitionManager(VoiceAssistantService instance,
      SpeechRecognitionManager speechRecognitionManager) {
    instance.speechRecognitionManager = speechRecognitionManager;
  }

  @InjectedFieldSignature("com.tarzo.ai.services.VoiceAssistantService.ttsManager")
  public static void injectTtsManager(VoiceAssistantService instance, TTSManager ttsManager) {
    instance.ttsManager = ttsManager;
  }

  @InjectedFieldSignature("com.tarzo.ai.services.VoiceAssistantService.intentDetector")
  public static void injectIntentDetector(VoiceAssistantService instance,
      IntentDetector intentDetector) {
    instance.intentDetector = intentDetector;
  }

  @InjectedFieldSignature("com.tarzo.ai.services.VoiceAssistantService.llmClient")
  public static void injectLlmClient(VoiceAssistantService instance, LLMClient llmClient) {
    instance.llmClient = llmClient;
  }

  @InjectedFieldSignature("com.tarzo.ai.services.VoiceAssistantService.memoryManager")
  public static void injectMemoryManager(VoiceAssistantService instance,
      MemoryManager memoryManager) {
    instance.memoryManager = memoryManager;
  }

  @InjectedFieldSignature("com.tarzo.ai.services.VoiceAssistantService.deviceControlManager")
  public static void injectDeviceControlManager(VoiceAssistantService instance,
      DeviceControlManager deviceControlManager) {
    instance.deviceControlManager = deviceControlManager;
  }

  @InjectedFieldSignature("com.tarzo.ai.services.VoiceAssistantService.callManager")
  public static void injectCallManager(VoiceAssistantService instance, CallManager callManager) {
    instance.callManager = callManager;
  }

  @InjectedFieldSignature("com.tarzo.ai.services.VoiceAssistantService.smsManager")
  public static void injectSmsManager(VoiceAssistantService instance, SmsManager smsManager) {
    instance.smsManager = smsManager;
  }

  @InjectedFieldSignature("com.tarzo.ai.services.VoiceAssistantService.reminderManager")
  public static void injectReminderManager(VoiceAssistantService instance,
      ReminderManager reminderManager) {
    instance.reminderManager = reminderManager;
  }

  @InjectedFieldSignature("com.tarzo.ai.services.VoiceAssistantService.commandProcessor")
  public static void injectCommandProcessor(VoiceAssistantService instance,
      CommandProcessor commandProcessor) {
    instance.commandProcessor = commandProcessor;
  }
}
