package com.example.loginfirebasemultiple

import android.annotation.SuppressLint
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private var callbackManager: CallbackManager? = null
    private var firebaseAuth: FirebaseAuth? = null
    private var mAuthListener: FirebaseAuth.AuthStateListener? = null
    private var txtUser: TextView? = null
    private var txtEmail: TextView? = null
    private var imgProfile: ImageView? = null
    private var logoutButton: LoginButton? = null
    private var loginButton: LoginButton? = null
    private var signInButton: SignInButton? = null
    private var googleSignInClient: GoogleApiClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //AppEventsLogger.activateApp(this);
        setContentView(R.layout.activity_main)
        txtUser = findViewById<View>(R.id.txtUser) as TextView
        txtEmail = findViewById<View>(R.id.txtEmail) as TextView
        imgProfile = findViewById<View>(R.id.imgProfile) as ImageView
        firebaseAuth = FirebaseAuth.getInstance()
        callbackManager = CallbackManager.Factory.create()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()



        googleSignInClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this) { Toast.makeText(this@MainActivity, "Something Went Wrong", Toast.LENGTH_SHORT).show() }
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build()

        signInButton = findViewById<View>(R.id.googleBtn) as SignInButton
        loginButton = findViewById<View>(R.id.btnFacebookIn) as LoginButton
        logoutButton = findViewById<View>(R.id.btnFacebookOut) as LoginButton
        logoutButton!!.setOnClickListener(this)

        signInButton!!.setOnClickListener { signIn() }

        loginButton!!.setReadPermissions("email", "public_profile")//user_status, publish_actions..
        loginButton!!.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                handleFacebookAccessToken(loginResult.accessToken)
            }

            override fun onCancel() {
                Toast.makeText(this@MainActivity, "Sign In canceled", Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: FacebookException) {
                Toast.makeText(this@MainActivity, error.toString(), Toast.LENGTH_SHORT).show()
                Log.e("error", error.toString())
            }
        })

        mAuthListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (AccessToken.getCurrentAccessToken() != null)
                Toast.makeText(this@MainActivity, AccessToken.getCurrentAccessToken().expires.toString(), Toast.LENGTH_SHORT).show()
            if (user != null) {
                val email = user.email
                val userName = user.displayName
                txtEmail!!.text = email
                txtUser!!.text = userName
                Picasso.get().load(user.photoUrl).into(imgProfile)
                loginButton!!.visibility = View.GONE
                logoutButton!!.visibility = View.VISIBLE
            } else {
                Log.d("TG", "SIGNED OUT")
                txtEmail!!.text = ""
                txtUser!!.text = ""
                imgProfile!!.setImageBitmap(null)
                loginButton!!.visibility = View.VISIBLE
                logoutButton!!.visibility = View.GONE
            }
        }
    }


    private fun signIn() {
        val signIntent = Auth.GoogleSignInApi.getSignInIntent(googleSignInClient)
        startActivityForResult(signIntent, RC_SIGN_IN)
    }

    public override fun onStart() {
        super.onStart()
        firebaseAuth!!.addAuthStateListener(mAuthListener!!)
    }

    public override fun onStop() {
        super.onStop()
        if (mAuthListener != null) {
            firebaseAuth!!.removeAuthStateListener(mAuthListener!!)
        }
    }

    private fun handleFacebookAccessToken(token: AccessToken) {

        val credential = FacebookAuthProvider.getCredential(token.token)
        firebaseAuth!!.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (!task.isSuccessful) {
                        Toast.makeText(this@MainActivity, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()
                    }
                }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager!!.onActivityResult(requestCode,
                resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess) {
                val account = result.signInAccount
                firebaseAuthWithGoogle(account!!)
            } else {
                Toast.makeText(this, "Auth went Wrong", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.id!!)

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        firebaseAuth!!.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithCredential:success")
                        val user = firebaseAuth!!.currentUser

                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                        Toast.makeText(this@MainActivity, "Authentication Failed.", Toast.LENGTH_SHORT).show()

                    }

                    // ...
                }
    }

    override fun onClick(v: View) {
        firebaseAuth!!.signOut()
        LoginManager.getInstance().logOut()
    }

    companion object {
        private val TAG = "Google"
        private val RC_SIGN_IN = 9001
    }
}
