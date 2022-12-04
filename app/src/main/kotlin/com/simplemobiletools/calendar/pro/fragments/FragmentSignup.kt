package com.simplemobiletools.calendar.pro.fragments

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.simplemobiletools.calendar.pro.activities.MainActivity
import com.simplemobiletools.calendar.pro.databinding.FragmentSignupBinding

class FragmentSignup : BaseFragment<FragmentSignupBinding>() {
    private lateinit var firebaseAuth: FirebaseAuth
    private var email = ""
    private var password = ""
    private var repassword = ""
    override fun getViewBinding(): FragmentSignupBinding = FragmentSignupBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
        binding.btnSignup.setOnClickListener {
            validateData()
        }
    }

    private fun validateData() {
        email = binding.edtUsername.text.toString().trim()
        password = binding.edtPassword.text.toString().trim()
        repassword = binding.edtRepassword.text.toString().trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            //invalid email format
            binding.edtUsername.error = "Invalid email format"
        }
        else if (TextUtils.isEmpty(password)) {
            binding.edtPassword.error = "Please enter password"
        }
        else if (password.length < 6 ) {
            binding.edtPassword.error = "Password must be at least 6 characters long"
        }
        else if (password != repassword) {
            binding.edtRepassword.error = "Password doesn't match"
        }
        else {
            firebaseSignUp()
        }
    }

    private fun firebaseSignUp() {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                activity?.finish()
                startActivity(Intent(requireActivity(), MainActivity :: class.java))
                Toast.makeText(requireActivity(), "Signup successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e->
                Toast.makeText(requireActivity(), "Signup failed due to ${e.message}", Toast.LENGTH_SHORT).show()

            }

    }
}
