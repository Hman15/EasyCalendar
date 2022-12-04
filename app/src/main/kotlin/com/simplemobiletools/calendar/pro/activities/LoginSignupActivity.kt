package com.simplemobiletools.calendar.pro.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.tabs.TabLayoutMediator
import com.simplemobiletools.calendar.pro.adapters.LoginViewPagerAdapter
import com.simplemobiletools.calendar.pro.databinding.ActivityLoginSignupBinding

class LoginSignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val binding = ActivityLoginSignupBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val vpAdapter = LoginViewPagerAdapter(supportFragmentManager, lifecycle)
        binding.loginViewPager.adapter = vpAdapter
        TabLayoutMediator(binding.loginTabLayout, binding.loginViewPager) {tab, position ->
            when(position) {
                0 -> tab.text = "Login"
                1 -> tab.text = "Signup"
            }
        }.attach()

    }
}
