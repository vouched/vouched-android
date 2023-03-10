package id.vouched.android.kt.example

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import id.vouched.android.kt.example.databinding.FragmentWelcomeMenuBinding

class WelcomeMenuFragment : Fragment() {

    private var apiKey = BuildConfig.API_KEY

    private var groupId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFragmentResultListener(ApiKeyFormDialogFragment.REQUEST_API_KEY_DATA) { _, data ->
            data.getString(ApiKeyFormDialogFragment.KEY_API_KEY)?.let {
                apiKey = it
            }
            data.getString(ApiKeyFormDialogFragment.KEY_GROUP_ID)?.let {
                groupId = it
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentWelcomeMenuBinding.inflate(inflater, container, false).apply {
        identityVerificationFlowButton.setOnClickListener {
            val direction = WelcomeMenuFragmentDirections.actionWelcomeMenuFragmentToWelcomeFormFragment(
                apiKey,
                groupId
            )
            findNavController().navigate(direction)
        }
        reverificationFlowButton.setOnClickListener {
            val direction = WelcomeMenuFragmentDirections.actionWelcomeMenuFragmentToJobIdFormDialogFragment(
                apiKey,
                groupId
            )
            findNavController().navigate(direction)
        }
    }.root

    override fun onResume() {
        super.onResume()
        checkApiKey()
    }

    private fun checkApiKey() {
        if (apiKey.isBlank()) {
            findNavController().navigate(R.id.action_welcomeMenuFragment_to_apiKeyFormDialogFragment)
        }
    }
}
