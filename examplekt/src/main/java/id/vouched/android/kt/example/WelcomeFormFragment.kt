package id.vouched.android.kt.example

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import id.vouched.android.kt.example.databinding.FragmentWelcomeFormBinding
import id.vouched.android.kt.example.utils.EventWrapperObserver

class WelcomeFormFragment : Fragment() {

    private val viewModel: WelcomeFormViewModel by viewModels()

    private lateinit var binding: FragmentWelcomeFormBinding

    private val navigationArgs: WelcomeFormFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentWelcomeFormBinding.inflate(
        inflater,
        container,
        false
    ).apply {
        binding = this
        viewModel = this@WelcomeFormFragment.viewModel
        lifecycleOwner = this@WelcomeFormFragment.viewLifecycleOwner
        startObservers()
    }.root

    private fun startObservers() {
        viewModel.event.observe(
            viewLifecycleOwner,
            EventWrapperObserver { event ->
                when (event) {
                    is WelcomeFormViewModel.Event.NavToNext -> {
                        val action = WelcomeFormFragmentDirections.actionWelcomeFragmentToDocumentScanningFragment(
                            event.firstName,
                            event.lastName,
                            navigationArgs.apiKey,
                            navigationArgs.groupId
                        ).apply {
                            allowCameraFlash = event.allowCameraFlash
                            allowIdConfirmation = event.allowIdConfirmation
                            allowOrientationCheck = event.allowOrientationCheck
                            timeoutMilliseconds = event.timeoutMilliseconds
                            geoLocationEnabled = event.allowGeolocation
                        }
                        findNavController().navigate(action)
                    }
                }
            }
        )
    }
}
