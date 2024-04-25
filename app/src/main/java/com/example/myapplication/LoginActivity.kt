package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import java.util.regex.Pattern


class LoginActivity : AppCompatActivity() {
    lateinit var binding: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       binding = ActivityLoginBinding.inflate(layoutInflater)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(binding.root)
        binding.loginBtn.setOnClickListener{
            val email = binding.emailEdittext.text.toString()
            val password = binding.passwordEdittext.text.toString()


            if(!Pattern.matches(Patterns.EMAIL_ADDRESS.pattern(),email)) {
                binding.emailEdittext.setError("Invalid Email Address")
                return@setOnClickListener
            }

            if(password.length<6){
                binding.passwordEdittext.setError("Length should be atleast 6 characters")
                return@setOnClickListener
            }

            LoginWithFireBase(email,password)
        }
        binding.gotoSignup.setOnClickListener{
            startActivity(Intent(this,SignupActivity::class.java))
        }
    }

    fun LoginWithFireBase(email : String,password :String){
        setInProgress(true)
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email,password)
            .addOnSuccessListener {
                setInProgress(false)
                startActivity(Intent(this@LoginActivity,MainActivity::class.java))
                finish()
                Toast.makeText(applicationContext,"Log in Successful", Toast.LENGTH_LONG).show()

            }.addOnFailureListener{
                setInProgress(false)
                Toast.makeText(applicationContext,"Log in Failed", Toast.LENGTH_LONG).show()
            }
    }

    override fun onResume() {
        super.onResume()
        FirebaseAuth.getInstance().currentUser?.apply {
            startActivity(Intent(this@LoginActivity,MainActivity::class.java))
            finish()
        }
    }


    fun setInProgress(InProgress: Boolean) {
        if (InProgress) {
            binding.loginBtn.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.loginBtn.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
        }
    }
}