package brut.androlib.callback;

import brut.androlib.enums.DecodeState;
import brut.androlib.enums.DecodeStep;

public interface IDecodeCallback {
    void onDecodeProgress(DecodeStep step, DecodeState state, float progress, String message);
}
