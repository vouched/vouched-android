package id.vouched.sample
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.vouched.sdk.VouchedSession
import id.vouched.sdk.models.FlowResult

@Composable
fun MainView(session: VouchedSession, onFinish: () -> Unit) {
    var flowResult by remember { mutableStateOf<FlowResult?>(null) }

    when (val result = flowResult) {
        null -> {
            FlowWithCustomInstructions(session, { result ->
                flowResult = result
            })
        }

        is FlowResult.Success -> {
            // Show success screen
            ResultScreen(
                title = "Verification Complete! ✓",
                message = "Job ID: ${result.job.id}",
                onDismiss = onFinish,
            )
        }

        is FlowResult.Cancelled -> {
            // Show cancelled screen
            ResultScreen(
                title = "Verification Cancelled",
                message = result.reason ?: "You cancelled the verification process",
                onDismiss = onFinish,
            )
        }

        is FlowResult.Error -> {
            // Show error screen
            ResultScreen(
                title = "Verification Error",
                message = result.message,
                onDismiss = onFinish,
            )
        }
    }
}

@Composable
private fun ResultScreen(title: String, message: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }
}
