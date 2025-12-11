package com.cookmate.orchestrator.VoiceAssist.TTS;

import com.microsoft.cognitiveservices.speech.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AzureTtsService {

    private final SpeechConfig speechConfig;

    /**
     * 텍스트를 Azure TTS로 합성해서
     * 16kHz / 16bit / mono **raw PCM** 바이트 배열로 반환
     */
    public byte[] synthesizeToRawPcm(String text) throws Exception {
        // 출력 포맷을 "헤더 없는" Raw PCM 으로 설정
        //    - 16000 Hz
        //    - 16 bit
        //    - mono
        speechConfig.setSpeechSynthesisOutputFormat(
                SpeechSynthesisOutputFormat.Raw16Khz16BitMonoPcm
        );

        try (SpeechSynthesizer synthesizer = new SpeechSynthesizer(speechConfig, null)) {
            SpeechSynthesisResult result = synthesizer.SpeakTextAsync(text).get();

            if (result.getReason() == ResultReason.SynthesizingAudioCompleted) {
                // 이 바이트 배열이 **헤더 없는 raw PCM** 이라서
                // WebSocket BinaryMessage 로 그대로 보내면 됨
                return result.getAudioData();
            } else {
                throw new IllegalStateException("TTS 실패: " + result.getReason());
            }
        }
    }

    /**
     * 참고: 필요하면 WAV(RIFF) 형식도 같이 쓸 수 있게 남겨두는 버전
     */
    public byte[] synthesizeToWav(String text) throws Exception {
        speechConfig.setSpeechSynthesisOutputFormat(
                SpeechSynthesisOutputFormat.Riff16Khz16BitMonoPcm
        );

        try (SpeechSynthesizer synthesizer = new SpeechSynthesizer(speechConfig, null)) {
            SpeechSynthesisResult result = synthesizer.SpeakTextAsync(text).get();

            if (result.getReason() == ResultReason.SynthesizingAudioCompleted) {
                return result.getAudioData();  // WAV 바이트
            } else {
                throw new IllegalStateException("TTS 실패: " + result.getReason());
            }
        }
    }
}