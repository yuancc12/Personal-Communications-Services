package com.example.smsvote.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)

    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun signInWithGoogle(): Result<FirebaseUser> {
        return try {
            val idToken = getGoogleIdToken()
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            Result.success(authResult.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getGoogleIdToken(): String {
        return try {
            requestGoogleIdToken(filterByAuthorized = true)
        } catch (e: NoCredentialException) {
            // No previously authorized account — show full account picker
            requestGoogleIdToken(filterByAuthorized = false)
        }
    }

    private suspend fun requestGoogleIdToken(filterByAuthorized: Boolean): String {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorized)
            .setServerClientId(WEB_CLIENT_ID)
            .setAutoSelectEnabled(filterByAuthorized)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        val result = credentialManager.getCredential(context, request)
        return GoogleIdTokenCredential.createFrom(result.credential.data).idToken
    }

    fun signOut() {
        auth.signOut()
    }

    companion object {
        // Replace after Firebase setup:
        // Firebase Console → Authentication → Sign-in method → Google → Web SDK configuration → Web client ID
        const val WEB_CLIENT_ID = "15818876851-uvb57q8rgkpciduv50gocnsst31rus96.apps.googleusercontent.com"
    }
}
