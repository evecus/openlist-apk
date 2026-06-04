package com.openlist.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.openlist.app.OpenListApp
import com.openlist.app.data.api.RetrofitClient
import com.openlist.app.data.local.ServerEntity
import com.openlist.app.data.repository.OpenListRepository
import com.openlist.app.data.repository.Result
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SetupViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as OpenListApp
    private val db = app.database
    private val prefs = app.preferences

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    private val _serverCheckState = MutableLiveData<ServerCheckState>()
    val serverCheckState: LiveData<ServerCheckState> = _serverCheckState

    fun checkExistingSession() {
        viewModelScope.launch {
            val token = prefs.activeServerToken.first()
            val url = prefs.activeServerUrl.first()
            if (token.isNotEmpty() && url.isNotBlank()) {
                _loginState.value = LoginState.AlreadyLoggedIn(url, token)
            } else {
                // Clear any partial/corrupt state before showing setup
                prefs.clearActiveServer()
                _loginState.value = LoginState.NeedsSetup
            }
        }
    }

    fun checkServerReachable(url: String) {
        viewModelScope.launch {
            _serverCheckState.value = ServerCheckState.Checking
            try {
                val repo = OpenListRepository(url, "")
                val result = repo.getPublicSettings()
                when (result) {
                    is Result.Success -> _serverCheckState.value = ServerCheckState.Reachable(
                        result.data["site_title"] ?: "OpenList"
                    )
                    is Result.Error -> _serverCheckState.value = ServerCheckState.Error(result.message)
                    else -> {}
                }
            } catch (e: Exception) {
                _serverCheckState.value = ServerCheckState.Error(e.message ?: "Cannot reach server")
            }
        }
    }

    fun login(serverUrl: String, serverName: String, username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val repo = OpenListRepository(serverUrl, "")
                when (val result = repo.login(username, password)) {
                    is Result.Success -> {
                        val token = result.data
                        // Save server to DB
                        val existing = db.serverDao().getAllServers().first()
                            .find { RetrofitClient.normalizeUrl(it.url) == RetrofitClient.normalizeUrl(serverUrl) }
                        val serverId: Long
                        if (existing != null) {
                            db.serverDao().updateServer(existing.copy(token = token, username = username, isActive = true, lastUsed = System.currentTimeMillis()))
                            serverId = existing.id
                        } else {
                            serverId = db.serverDao().insertServer(
                                ServerEntity(name = serverName, url = serverUrl, token = token, username = username, isActive = true)
                            )
                        }
                        db.serverDao().clearActiveServer()
                        db.serverDao().setActiveServer(serverId)
                        prefs.setActiveServer(serverId, serverUrl, token, serverName)
                        _loginState.value = LoginState.Success(serverUrl, token)
                    }
                    is Result.Error -> _loginState.value = LoginState.Error(result.message)
                    else -> {}
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun loginAsGuest(serverUrl: String, serverName: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val serverId = db.serverDao().insertServer(
                    ServerEntity(name = serverName, url = serverUrl, token = "", isActive = true)
                )
                db.serverDao().clearActiveServer()
                db.serverDao().setActiveServer(serverId)
                prefs.setActiveServer(serverId, serverUrl, "", serverName)
                _loginState.value = LoginState.Success(serverUrl, "")
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Failed")
            }
        }
    }
}

sealed class LoginState {
    object NeedsSetup : LoginState()
    object Loading : LoginState()
    data class AlreadyLoggedIn(val url: String, val token: String) : LoginState()
    data class Success(val url: String, val token: String) : LoginState()
    data class Error(val message: String) : LoginState()
}

sealed class ServerCheckState {
    object Checking : ServerCheckState()
    data class Reachable(val siteTitle: String) : ServerCheckState()
    data class Error(val message: String) : ServerCheckState()
}
