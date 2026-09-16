package id.vouched.sample

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.vouched.sdk.VouchedSDK
import id.vouched.sdk.VouchedSession
import id.vouched.sdk.models.FlowResult
import id.vouched.sdk.ui.flow.VouchedFlow
import id.vouched.sdk.ui.flow.customComposeScreen

/**
 * Example 1: Simple Flow
 *
 * The most basic implementation using the new Flow Builder API.
 * This replaces the old FlowOrchestrator approach.
 */
@Composable
fun SimpleFlowExample(session: VouchedSession, onComplete: (FlowResult) -> Unit) {
    val appConfig = session.appConfig

    val flowBuilder =
        remember(appConfig) {
            VouchedSDK
                .createFlow(appConfig)
                .onFlowComplete { result ->
                    Log.d("SimpleFlow", "Flow completed: $result")
                    onComplete(result)
                }
        }

    VouchedFlow(
        session = session,
        flowBuilder = flowBuilder,
    )
}

/**
 * Example 2: Flow with Custom Welcome Screen
 *
 * Shows how to inject a custom screen at the beginning of the flow.
 * The custom screen is shown before the InitializingStage.
 */
@Composable
fun FlowWithCustomInstructions(session: VouchedSession, onComplete: (FlowResult) -> Unit) {
    val appConfig = session.appConfig
    val flowBuilder =
        remember(appConfig) {
            VouchedSDK
                .createFlow(appConfig)
                .withTrackingConsent(true)
                .withIdStageInstructionsScreen(
                    provider =
                    customComposeScreen { onContinue ->
                        IdInstructionScreen(
                            onContinue = {
                                onContinue()
                            },
                        )
                    },
                ).withFaceStageInstructionsScreen(
                    provider =
                    customComposeScreen { onContinue ->
                        FaceInstructionScreen(
                            onContinue = {
                                onContinue()
                            },
                        )
                    },
                ).onFlowComplete { result ->
                    onComplete(result)
                }
        }

    VouchedFlow(
        session = session,
        flowBuilder = flowBuilder,
    )
}

@Composable
private fun IdInstructionScreen(onContinue: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Please have your ID ready",
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onContinue) {
                    Text("Start ID Capture")
                }
            }
        }
    }
}

@Composable
private fun FaceInstructionScreen(onContinue: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Please get ready for your selfie",
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onContinue) {
                    Text("Start selfie capture")
                }
            }
        }
    }
}
