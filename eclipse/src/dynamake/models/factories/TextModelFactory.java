package dynamake.models.factories;

import java.awt.Rectangle;
import java.util.Hashtable;

import dynamake.Fraction;
import dynamake.models.Model;
import dynamake.models.PropogationContext;
import dynamake.models.TextModel;
import dynamake.transcription.TranscriberBranch;

public class TextModelFactory implements Factory {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String getName() {
		return "Text";
	}

	@Override
	public Object create(Model rootModel, Rectangle creationBounds, Hashtable<String, Object> arguments, PropogationContext propCtx, int propDistance, TranscriberBranch<Model> branch) {
		TextModel model = new TextModel();
		Fraction fontSize = new Fraction(12);
		fontSize = fontSize.multiply(new Fraction(creationBounds.height, 20));
//		Fraction fontSize = 12 * 40 creationBounds.height;
		TranscriberBranch<Model> setFontSizeBranch = branch.isolatedBranch();
		model.setProperty("FontSize", fontSize, propCtx, propDistance, setFontSizeBranch);
		return model;
	}
}
