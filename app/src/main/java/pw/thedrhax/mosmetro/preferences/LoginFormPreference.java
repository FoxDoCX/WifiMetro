package pw.thedrhax.mosmetro.preferences;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.util.CredentialStore;

public class LoginFormPreference extends DialogPreference {
    private CredentialStore creds;
    private EditText text_login;
    private EditText text_password;

    public LoginFormPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public LoginFormPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public LoginFormPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LoginFormPreference(Context context) {
        super(context);
        init(context);
    }
    
    private void init(Context context) {
        creds = new CredentialStore(context);
    }

    @Override
    protected View onCreateDialogView() {
        View v = LayoutInflater
                .from(getContext())
                .inflate(R.layout.loginform_preference, null);

        text_login = (EditText) v.findViewById(R.id.text_login);
        text_login.setText(creds.getLogin(getKey()));

        text_password = (EditText) v.findViewById(R.id.text_password);
        text_password.setText(creds.getPassword(getKey()));

        return v;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            creds.put(getKey(),
                    text_login.getText().toString(),
                    text_password.getText().toString());
        }

        super.onDialogClosed(positiveResult);
    }
}
