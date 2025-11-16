package com.cookmate.orchestrator.VoiceAssist.TTS;

import com.microsoft.cognitiveservices.speech.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AzureTtsService {

    private final SpeechConfig speechConfig;

    /**
     * 텍스트를 Azure TTS로 합성해서 오디오 바이트(byte[])로 반환
     * - 현재는 WAV(RIFF) 형식 기준 예시
     *   (Raw PCM으로 바꾸고 싶으면 OutputFormat만 바꿔주면 됨)
     */
    public byte[] synthesizeToWav(String text) throws Exception {
        // 필요하다면 여기서 한번만 포맷 지정
        // speechConfig.setSpeechSynthesisOutputFormat(
        //         SpeechSynthesisOutputFormat.Riff16Khz16BitMonoPcm);

        try (SpeechSynthesizer synthesizer = new SpeechSynthesizer(speechConfig, null)) {
            SpeechSynthesisResult result = synthesizer.SpeakTextAsync(text).get();

            if (result.getReason() == ResultReason.SynthesizingAudioCompleted) {
                return result.getAudioData();  // byte[]
            } else {
                throw new IllegalStateException("TTS 실패: " + result.getReason());
            }
        }
    }

    /**
     * 프론트에서 raw PCM(헤더 없는)으로 바로 재생하고 싶다면
     * OutputFormat을 Raw16Khz16BitMonoPcm 등으로 바꿔서 사용
     */
    public byte[] synthesizeToRawPcm(String text) throws Exception {
        speechConfig.setSpeechSynthesisOutputFormat(
                SpeechSynthesisOutputFormat.Raw16Khz16BitMonoPcm);

        try (SpeechSynthesizer synthesizer = new SpeechSynthesizer(speechConfig, null)) {
            SpeechSynthesisResult result = synthesizer.SpeakTextAsync(text).get();

            if (result.getReason() == ResultReason.SynthesizingAudioCompleted) {
                return result.getAudioData();
            } else {
                throw new IllegalStateException("TTS 실패: " + result.getReason());
            }
        }
    }
}