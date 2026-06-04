package com.openlist.app.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.snackbar.Snackbar
import com.openlist.app.R
import com.openlist.app.databinding.ActivitySetupBinding
import com.openlist.app.ui.viewmodel.LoginState
import com.openlist.app.ui.viewmodel.ServerCheckState
import com.openlist.app.ui.viewmodel.SetupViewModel

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private val viewModel: SetupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
        viewModel.checkExistingSession()
    }

    private fun setupUI() {
        // Animate logo on enter
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        binding.logoContainer.startAnimation(fadeIn)

        // Step 1: Server URL input with "Connect" button
        binding.btnConnect.setOnClickListener {
            val url = binding.etServerUrl.text.toString().trim()
            if (url.isEmpty()) {
                binding.tilServerUrl.error = getString(R.string.error_url_empty)
                return@setOnClickListener
            }
            binding.tilServerUrl.error = null
            viewModel.checkServerReachable(url)
        }

        // Step 2: Login form
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val url = binding.etServerUrl.text.toString().trim()
            val serverName = binding.etServerName.text.toString().trim()
                .ifEmpty { extractHostname(url) }

            if (username.isEmpty()) {
                binding.tilUsername.error = getString(R.string.error_username_empty)
                return@setOnClickListener
            }
            binding.tilUsername.error = null
            viewModel.login(url, serverName, username, password)
        }

        binding.btnGuestAccess.setOnClickListener {
            val url = binding.etServerUrl.text.toString().trim()
            val serverName = binding.etServerName.text.toString().trim()
                .ifEmpty { extractHostname(url) }
            viewModel.loginAsGuest(url, serverName)
        }

        binding.btnChangeServer.setOnClickListener {
            showStep1()
        }

        // Auto-remove errors on type
        binding.etServerUrl.doAfterTextChanged { binding.tilServerUrl.error = null }
        binding.etUsername.doAfterTextChanged { binding.tilUsername.error = null }
    }

    private fun observeViewModel() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginState.NeedsSetup -> showStep1()
                is LoginState.Loading -> showLoading(true)
                is LoginState.AlreadyLoggedIn -> navigateToMain()
                is LoginState.Success -> navigateToMain()
                is LoginState.Error -> {
                    showLoading(false)
                    showSnackbar(state.message)
                }
            }
        }

        viewModel.serverCheckState.observe(this) { state ->
            when (state) {
                is ServerCheckState.Checking -> {
                    binding.btnConnect.isEnabled = false
                    binding.serverCheckProgress.visibility = View.VISIBLE
                }
                is ServerCheckState.Reachable -> {
                    binding.btnConnect.isEnabled = true
                    binding.serverCheckProgress.visibility = View.GONE
                    showStep2(state.siteTitle)
                }
                is ServerCheckState.Error -> {
                    binding.btnConnect.isEnabled = true
                    binding.serverCheckProgress.visibility = View.GONE
                    binding.tilServerUrl.error = state.message
                }
            }
        }
    }

    private fun showStep1() {
        binding.cardStep2.visibility = View.GONE
        binding.cardStep1.visibility = View.VISIBLE
        binding.loadingContainer.visibility = View.GONE
    }

    private fun showStep2(siteTitle: String) {
        binding.cardStep1.visibility = View.VISIBLE
        binding.cardStep2.visibility = View.VISIBLE
        binding.tvSiteTitle.text = siteTitle
        binding.cardStep2.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up))
        binding.etUsername.requestFocus()
    }

    private fun showLoading(loading: Boolean) {
        binding.loadingContainer.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.btnGuestAccess.isEnabled = !loading
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun extractHostname(url: String): String {
        return try {
            java.net.URL(if (url.startsWith("http")) url else "http://$url").host
        } catch (e: Exception) {
            url
        }
    }
}
