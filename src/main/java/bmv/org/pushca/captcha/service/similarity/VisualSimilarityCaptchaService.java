package bmv.org.pushca.captcha.service.similarity;

import bmv.org.pushca.captcha.service.similarity.PuzzleImageGeneratorTypes.SimilarityChallenge;
import org.springframework.stereotype.Service;

import static bmv.org.pushca.captcha.service.similarity.PuzzleImageGeneratorUtils.generateRandomColor;

@Service
public class VisualSimilarityCaptchaService {

    /**
     * Below this size the rotated and scaled piece no longer fits the intermediate obfuscation square,
     * so the matched shape gets clipped (and at 100 px or less generation fails outright).
     */
    public static final int MIN_PIECE_SIDE_LENGTH_PX = 300;

    private final VisualSimilarityChallengeGenerator generator = new VisualSimilarityChallengeGenerator();

    public SimilarityChallenge generateCaptchaSet(int gridSize, int pieceSideLengthPx, boolean applyNoise) {
        if (pieceSideLengthPx < MIN_PIECE_SIDE_LENGTH_PX) {
            throw new IllegalArgumentException(
                    "Piece side length must be at least " + MIN_PIECE_SIDE_LENGTH_PX + " px, but was " + pieceSideLengthPx);
        }
        return generator.generateVisualSimilarityChallenge(
                pieceSideLengthPx / 3,
                pieceSideLengthPx,
                gridSize,
                generateRandomColor(),
                applyNoise
        );
    }

    @SuppressWarnings("unused")
    public VisualSimilarityChallengeGenerator getGenerator() {
        return generator;
    }
}
