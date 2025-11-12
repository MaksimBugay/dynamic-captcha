package bmv.org.pushca.captcha.service.similarity;

import bmv.org.pushca.captcha.service.similarity.PuzzleImageGeneratorTypes.SimilarityChallenge;
import org.springframework.stereotype.Service;

import static bmv.org.pushca.captcha.service.similarity.PuzzleImageGeneratorUtils.generateRandomColor;

@Service
public class VisualSimilarityCaptchaService {

    private final VisualSimilarityChallengeGenerator generator = new VisualSimilarityChallengeGenerator();

    public SimilarityChallenge generateCaptchaSet(int gridSize, int pieceSideLengthPx, boolean applyNoise) {
        return generator.generateVisualSimilarityChallenge(
                pieceSideLengthPx / 3,
                pieceSideLengthPx,
                gridSize,
                generateRandomColor(),
                applyNoise
        );
    }

    public VisualSimilarityChallengeGenerator getGenerator() {
        return generator;
    }
}
