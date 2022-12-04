package com.simplemobiletools.calendar.pro.fragments

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.simplemobiletools.calendar.pro.activities.MainActivity
import com.simplemobiletools.calendar.pro.databinding.FragmentLoginBinding
import com.simplemobiletools.calendar.pro.firebase.UploadSave

class FragmentLogin : BaseFragment<FragmentLoginBinding>() {
    override fun getViewBinding(): FragmentLoginBinding = FragmentLoginBinding.inflate(layoutInflater)
    private lateinit var firebaseAuth : FirebaseAuth
    private var username = ""
    private var password = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return super.onCreateView(inflater, container, savedInstanceState)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
        //checkUser()
        binding.btnLogin.setOnClickListener {
            validateData()
        }

    }
    private fun checkUser() {
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null) {
            //user is already logged in
            startActivity(Intent(requireContext(), MainActivity :: class.java))

        }
    }
    private fun validateData() {
        username  = binding.edtUsername.text.toString().trim()
        password = binding.edtPassword.text.toString().trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
            binding.edtUsername.error = "Invalid email format"
        }
        else if (TextUtils.isEmpty(password)) {
            binding.edtPassword.error = "Please enter password"
        }
        else {
            firebaseLogin()
        }
    }

    private fun firebaseLogin() {
        firebaseAuth.signInWithEmailAndPassword(username, password)
            .addOnSuccessListener {
                val firebaseUser = firebaseAuth.currentUser
                val email = firebaseUser!!.email
                UploadSave.downloadFile(firebaseUser.uid, "${requireContext().filesDir}/${firebaseUser.uid}".toUri(),{
                        message->
                    Toast.makeText(context,"File save not found", Toast.LENGTH_LONG)
//                    startActivity(Intent(context, MainActivity :: class.java))
//                    activity?.finish()
                }){
                    val intent = Intent(context, MainActivity :: class.java)
                    intent.putExtra("userId", email)
                    intent.putExtra("isLoggedIn", true)
//                    Toast.makeText(context, "Login success", Toast.LENGTH_SHORT).show()
                    startActivity(intent)
                    activity?.finish()
                }
                //Toast.makeText(context, "Login success", Toast.LENGTH_SHORT).show()

            }
            .addOnFailureListener {
                Toast.makeText(context, "Login failed due to ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }



}
