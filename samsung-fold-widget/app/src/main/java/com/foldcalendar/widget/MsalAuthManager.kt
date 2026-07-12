package com.foldcalendar.widget

import android.app.Activity
import android.content.Context
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val SCOPES = arrayOf("Calendars.Read")

/**
 * Wraps MSAL's callback-based single-account API in suspend functions.
 * Requires res/raw/auth_config_single_account.json to hold a real Azure AD
 * app registration's client ID and redirect URI (see the project README).
 */
class MsalAuthManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private var client: ISingleAccountPublicClientApplication? = null

    private suspend fun client(): ISingleAccountPublicClientApplication {
        client?.let { return it }
        return suspendCancellableCoroutine { continuation ->
            PublicClientApplication.createSingleAccountPublicClientApplication(
                appContext,
                R.raw.auth_config_single_account,
                object : PublicClientApplication.ISingleAccountApplicationCreatedListener {
                    override fun onCreated(application: ISingleAccountPublicClientApplication) {
                        client = application
                        continuation.resume(application)
                    }

                    override fun onError(exception: MsalException) {
                        continuation.resumeWithException(exception)
                    }
                },
            )
        }
    }

    suspend fun currentAccount(): IAccount? {
        val app = client()
        return suspendCancellableCoroutine { continuation ->
            app.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
                override fun onAccountLoaded(activeAccount: IAccount?) {
                    continuation.resume(activeAccount)
                }

                override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                    // no-op: onAccountLoaded already reports the account to use
                }

                override fun onError(exception: MsalException) {
                    continuation.resumeWithException(exception)
                }
            })
        }
    }

    /** Returns a fresh access token if a signed-in account exists, else null. */
    suspend fun acquireTokenSilent(): String? {
        val app = client()
        val account = currentAccount() ?: return null
        return suspendCancellableCoroutine { continuation ->
            app.acquireTokenSilentAsync(
                SCOPES,
                account.authority,
                object : SilentAuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) {
                        continuation.resume(authenticationResult.accessToken)
                    }

                    override fun onError(exception: MsalException) {
                        continuation.resumeWithException(exception)
                    }
                },
            )
        }
    }

    /** Launches the interactive Microsoft sign-in flow from the given activity. */
    suspend fun signIn(activity: Activity): IAuthenticationResult {
        val app = client()
        return suspendCancellableCoroutine { continuation ->
            app.signIn(
                activity,
                null,
                SCOPES,
                object : AuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) {
                        continuation.resume(authenticationResult)
                    }

                    override fun onError(exception: MsalException) {
                        continuation.resumeWithException(exception)
                    }

                    override fun onCancel() {
                        continuation.resumeWithException(IllegalStateException("Sign-in cancelled"))
                    }
                },
            )
        }
    }

    suspend fun signOut() {
        val app = client()
        suspendCancellableCoroutine<Unit> { continuation ->
            app.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
                override fun onSignOut() {
                    continuation.resume(Unit)
                }

                override fun onError(exception: MsalException) {
                    continuation.resumeWithException(exception)
                }
            })
        }
    }

    companion object {
        @Volatile private var instance: MsalAuthManager? = null

        fun getInstance(context: Context): MsalAuthManager =
            instance ?: synchronized(this) {
                instance ?: MsalAuthManager(context).also { instance = it }
            }
    }
}
