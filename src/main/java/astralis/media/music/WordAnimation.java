package astralis.media.music;

import java.util.List;

public record WordAnimation(SpringValue scale, SpringValue lift, List<LetterAnimation> letters) {

    public boolean isSettled() {
        if (this.scale.isSettledAt(1.0) && this.lift.isSettledAt(0.0)) {
            for (LetterAnimation letter : this.letters) {
                if (!letter.getScale().isSettledAt(1.0) || !letter.getLift().isSettledAt(0.0)) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public SpringValue getScale() {
        return this.scale;
    }

    public SpringValue getLift() {
        return this.lift;
    }

    public List<LetterAnimation> getLetters() {
        return this.letters;
    }
}
