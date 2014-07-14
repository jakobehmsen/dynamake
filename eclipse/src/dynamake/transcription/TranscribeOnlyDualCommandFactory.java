package dynamake.transcription;

/**
 * Instances of implementers each represent transaction builders, for which the built transactions
 * are only to be transcribed but not to be used to affect the model history.
 */
public interface TranscribeOnlyDualCommandFactory<T> extends DualCommandFactory<T> {

}
