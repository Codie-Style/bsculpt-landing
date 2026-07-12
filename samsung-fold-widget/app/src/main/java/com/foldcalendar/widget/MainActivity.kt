package com.foldcalendar.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private sealed interface SignInState {
    data object Loading : SignInState
    data object SignedOut : SignInState
    data class SignedIn(val username: String) : SignInState
    data class Error(val message: String) : SignInState
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    CalendarAuthScreen()
                }
            }
        }
    }
}

@Composable
private fun CalendarAuthScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as ComponentActivity
    val scope = rememberCoroutineScope()
    val auth = remember { MsalAuthManager.getInstance(context) }
    var state by remember { mutableStateOf<SignInState>(SignInState.Loading) }

    LaunchedEffect(Unit) {
        val account = auth.currentAccount()
        state = if (account != null) SignInState.SignedIn(account.username) else SignInState.SignedOut
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(id = R.string.main_activity_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
            text = stringResource(id = R.string.main_activity_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )

        when (val current = state) {
            is SignInState.Loading -> Text(text = "…")
            is SignInState.SignedOut -> Button(onClick = {
                scope.launch {
                    state = try {
                        val result = auth.signIn(activity)
                        FoldCalendarWidgetReceiver.schedulePeriodicRefresh(context)
                        FoldCalendarWidgetReceiver.requestImmediateRefresh(context)
                        SignInState.SignedIn(result.account.username)
                    } catch (e: Exception) {
                        SignInState.Error(e.message ?: "Unknown error")
                    }
                }
            }) {
                Text(text = stringResource(id = R.string.sign_in_button))
            }

            is SignInState.SignedIn -> {
                Text(text = stringResource(id = R.string.status_signed_in, current.username))
                Button(
                    modifier = Modifier.padding(top = 16.dp),
                    onClick = {
                        scope.launch {
                            auth.signOut()
                            state = SignInState.SignedOut
                        }
                    },
                ) {
                    Text(text = stringResource(id = R.string.sign_out_button))
                }
            }

            is SignInState.Error -> {
                Text(text = stringResource(id = R.string.status_sign_in_error, current.message))
                Button(
                    modifier = Modifier.padding(top = 16.dp),
                    onClick = { state = SignInState.SignedOut },
                ) {
                    Text(text = stringResource(id = R.string.sign_in_button))
                }
            }
        }
    }
}
