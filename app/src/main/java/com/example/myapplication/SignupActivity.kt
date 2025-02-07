package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import com.example.myapplication.databinding.ActivitySignupBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import java.util.regex.Pattern


class SignupActivity : AppCompatActivity() {
     lateinit var binding: ActivitySignupBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivitySignupBinding.inflate(layoutInflater)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(binding.root)

        val videoPath = "android.resource://com.example.myapplication/" + R.raw.video
        val videoUri = Uri.parse(videoPath)
        binding.video.setVideoURI(videoUri)
        binding.video.start()


        binding.createaccountBtn.setOnClickListener {
            val email = binding.emailEdittext.text.toString()
            val password = binding.passwordEdittext.text.toString()
            val confirmpassword = binding.confirmpasswordEdittext.text.toString()

           if(!Pattern.matches(Patterns.EMAIL_ADDRESS.pattern(),email)) {
                binding.emailEdittext.setError("Invalid Email Address")
               return@setOnClickListener
           }

            if(password.length<6){
                binding.passwordEdittext.setError("Length should be atleast 6 characters")
                return@setOnClickListener
            }

            if(!confirmpassword.equals(password)){
                binding.confirmpasswordEdittext.setError("Passwords do not match ")
                return@setOnClickListener
            }

            createAccWithFireBase(email,password)
        }
        binding.gotoLogin.setOnClickListener{
           finish()
        }
    }

    override fun onPause() {
        super.onPause()
        // Pause the video when the activity is paused
        binding.video.pause()
    }

    override fun onResume() {
        super.onResume()
        // Resume playing the video when the activity is resumed
        binding.video.resume()
    }

    fun createAccWithFireBase(email : String,password :String){
        setInProgress(true)
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email,password)
            .addOnSuccessListener {
                setInProgress(false)
                Toast.makeText(applicationContext,"User Created Successfully",Toast.LENGTH_LONG).show()
                finish()

            }.addOnFailureListener{
                setInProgress(false)
                Toast.makeText(applicationContext,"There was a problem in creating the account",Toast.LENGTH_LONG).show()
            }
    }

    fun setInProgress(InProgress : Boolean){
        if(InProgress){
            binding.createaccountBtn.visibility= View.GONE
            binding.progressBar.visibility= View.VISIBLE
        }else{
            binding.createaccountBtn.visibility= View.VISIBLE
            binding.progressBar.visibility= View.GONE
        }
    }
}