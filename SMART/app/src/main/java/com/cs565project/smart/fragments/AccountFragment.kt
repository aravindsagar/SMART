package com.cs565project.smart.fragments


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

import com.cs565project.smart.R
import com.cs565project.smart.util.PreferencesHelper

/**
 * Fragment in onboarding activity, to get account details from the user. Currently a placeholder.
 */
class AccountFragment : Fragment() {

    private var emailDescription: EditText? = null
    private var passwordDescription: EditText? = null
    private var confirmDescription: EditText? = null

    // Create an anonymous implementation of OnClickListener
    private val signUpListener = View.OnClickListener {
        val userEmail = emailDescription!!.text.toString()
        val userPassword = passwordDescription!!.text.toString()
        val userConfirmPassword = confirmDescription!!.text.toString()

        PreferencesHelper.setPreference(it.context, USER_EMAIL_KEY, userEmail)
        PreferencesHelper.setPreference(it.context, USER_PASSWORD_KEY, userPassword)
        PreferencesHelper.setPreference(it.context, USER_CONFIRM_KEY, userConfirmPassword)

        Toast.makeText(context, "Signed Up Successfully", Toast.LENGTH_SHORT).show()
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_account, container, false)
        emailDescription = v.findViewById(R.id.userEmail)
        passwordDescription = v.findViewById(R.id.userPassword)
        confirmDescription = v.findViewById(R.id.userConfirmPassword)
        val signUpButton = v.findViewById<Button>(R.id.signUp)
        signUpButton.setOnClickListener(signUpListener)
        return v
    }

    companion object {
        val USER_EMAIL_KEY = "user_email"
        val USER_PASSWORD_KEY = "user_password"
        val USER_CONFIRM_KEY = "user_confirm_password"
    }

}// Required empty public constructor
