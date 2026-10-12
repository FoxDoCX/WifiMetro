package pw.thedrhax.mosmetro.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.InputType;
import android.util.AttributeSet;

import androidx.preference.EditTextPreference;

/**
 * androidx {@link EditTextPreference} ignores android:inputType from XML; apply it manually.
 */
public class InputEditTextPreference extends EditTextPreference {
    private static final int[] ATTRS = {android.R.attr.inputType};

    public InputEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    public InputEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public InputEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public InputEditTextPreference(Context context) {
        super(context);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, ATTRS);
        final int inputType = ta.getInt(0, InputType.TYPE_CLASS_TEXT);
        ta.recycle();

        setOnBindEditTextListener(edit -> {
            edit.setInputType(inputType);
            edit.setSelection(edit.getText().length());
        });
    }
}
