package com.cs565project.smart.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.cs565project.smart.R;
import com.cs565project.smart.util.PreferencesHelper;

/**
 * Fragment in onboarding activity, to get account details from the user. Currently a placeholder.
 */
public class AccountFragment extends Fragment {

    private EditText emailDescription, passwordDescription, confirmDescription;
    public static final String USER_EMAIL_KEY = "user_email";
    public static final String USER_PASSWORD_KEY = "user_password";
    public static final String USER_CONFIRM_KEY = "user_confirm_password";
    public AccountFragment() {
        // Required empty public constructor
    }

    // Create an anonymous implementation of OnClickListener
    private View.OnClickListener signUpListener = new View.OnClickListener() {
        public void onClick(View v) {


            String user_email = emailDescription.getText().toString();
            String user_password = passwordDescription.getText().toString();
            String user_confirm_password = confirmDescription.getText().toString();

            PreferencesHelper.setPreference(getContext(),USER_EMAIL_KEY, user_email);
            PreferencesHelper.setPreference(getContext(),USER_PASSWORD_KEY, user_password);
            PreferencesHelper.setPreference(getContext(),USER_CONFIRM_KEY, user_confirm_password);

            Toast.makeText(getContext(), "Signed Up Successfully", Toast.LENGTH_SHORT).show();
        }
    };



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_account, container, false);
        emailDescription = v.findViewById(R.id.user_email);
        passwordDescription = v.findViewById(R.id.user_password);
        confirmDescription = v.findViewById(R.id.user_confirm_password);
        Button signUpButton = v.findViewById(R.id.signUp);
        signUpButton.setOnClickListener(signUpListener);
        return v;
    }

}
