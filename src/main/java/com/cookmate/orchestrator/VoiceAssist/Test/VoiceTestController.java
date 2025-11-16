package com.cookmate.orchestrator.VoiceAssist.Test;

import com.cookmate.orchestrator.VoiceAssist.NLU.DialogueService;
import com.cookmate.orchestrator.VoiceAssist.NLU.NLUService;
import com.cookmate.orchestrator.VoiceAssist.TTS.AzureTtsService;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class VoiceTestController {

    private final NLUService nluService;
    private final DialogueService dialogueService;
    private final SpeechConfig speechConfig;
    private final AzureTtsService azureTtsService;

    // (1) 텍스트만 넣어서 NLU + Dialogue 테스트
    @PostMapping("/text")
    public String testText(@RequestBody String text) {
        var intentResult = nluService.analyze(text);
        return dialogueService.handleIntent("dummy-session", intentResult);
    }

    // (2) STT 통합 테스트
    @PostMapping("/voice")
    public ResponseEntity<byte[]> testVoice(@RequestParam("file") MultipartFile file) throws Exception {
        // 1) 파일을 임시 wav로 저장
        Path temp = Files.createTempFile("voice-test-", ".wav");
        file.transferTo(temp.toFile());

        try (AudioConfig audioConfig = AudioConfig.fromWavFileInput(temp.toString());
             SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig, audioConfig)) {

            // 2) STT: 음성 → 텍스트
            SpeechRecognitionResult sttResult = recognizer.recognizeOnceAsync().get();

            if (sttResult.getReason() != ResultReason.RecognizedSpeech) {
                byte[] wav = azureTtsService.synthesizeToWav("음성 인식 실패: " + sttResult.getReason());
                return ResponseEntity
                        .ok()
                        .header(HttpHeaders.CONTENT_TYPE, "audio/wav")
                        .body(wav);
            }

            String text = sttResult.getText();
            System.out.println("[STT 결과] " + text);

            // 3) NLU
            var intentResult = nluService.analyze(text);

            // 4) Dialogue (세션키는 일단 테스트용으로 dummy-session)
            String answer = dialogueService.handleIntent("dummy-session", intentResult);

            byte[] wav = azureTtsService.synthesizeToWav(answer);
            return ResponseEntity
                    .ok()
                    .header(HttpHeaders.CONTENT_TYPE, "audio/wav")
                    .body(wav);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    @PostMapping("/tts")
    public ResponseEntity<byte[]> testTts(@RequestBody String text) throws Exception {
        byte[] wav = azureTtsService.synthesizeToWav(text);
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE, "audio/wav")
                .body(wav);
    }
}
