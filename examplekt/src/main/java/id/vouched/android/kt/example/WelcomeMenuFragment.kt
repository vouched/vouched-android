package id.vouched.android.kt.example

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import id.vouched.android.kt.example.databinding.FragmentWelcomeMenuBinding

class WelcomeMenuFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentWelcomeMenuBinding.inflate(inflater, container, false).apply {
        identityVerificationFlowButton.setOnClickListener {
            findNavController().navigate(R.id.action_welcomeMenuFragment_to_welcomeFormFragment)
        }
        reverificationFlowButton.setOnClickListener {
            findNavController().navigate(R.id.action_welcomeMenuFragment_to_jobIdFormDialogFragment)
        }
    }.root
}
