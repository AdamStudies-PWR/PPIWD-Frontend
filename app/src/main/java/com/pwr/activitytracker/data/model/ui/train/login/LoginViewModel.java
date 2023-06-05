package com.pwr.activitytracker.data.model.ui.train.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.util.Patterns;

import com.pwr.activitytracker.data.LoginRepository;
import com.pwr.activitytracker.data.Result;
import com.pwr.activitytracker.data.model.LoggedInUser;
import com.pwr.activitytracker.R;

public class LoginViewModel extends ViewModel
{

    private MutableLiveData<LoginFormState> loginFormState = new MutableLiveData<>();
    private MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();
    private LoginRepository loginRepository;

    LoginViewModel(LoginRepository loginRepository)
    {
        this.loginRepository = loginRepository;
    }

    LiveData<LoginFormState> getLoginFormState()
    {
        return loginFormState;
    }

    LiveData<LoginResult> getLoginResult()
    {
        return loginResult;
    }

    public void login(String username, String password)
    {
        // can be launched in a separate asynchronous job
        Result<LoggedInUser> result = loginRepository.login(username, password);

        if (result instanceof Result.Success)
        {
            LoggedInUser data = ((Result.Success<LoggedInUser>) result).getData();
            loginResult.setValue(new LoginResult(new LoggedInUserView(data.getDisplayName())));
        } else
        {
            loginResult.setValue(new LoginResult(R.string.login_failed));
        }
    }

    public void register(String username, String password)
    {
        // can be launched in a separate asynchronous job
        Result<LoggedInUser> result = loginRepository.register(username, password);

        if (result instanceof Result.Success)
        {
            LoggedInUser data = ((Result.Success<LoggedInUser>) result).getData();
            loginResult.setValue(new LoginResult(new LoggedInUserView(data.getDisplayName())));
        } else
        {
            loginResult.setValue(new LoginResult(R.string.login_failed));
        }
    }

    public void loginDataChanged(String username, String password)
    {
        if (!isUserNameValid(username))
        {
            loginFormState.setValue(new LoginFormState(R.string.invalid_username, null));
        } else if (!isPasswordValid(password))
        {
            loginFormState.setValue(new LoginFormState(null, R.string.invalid_password));
        } else
        {
            loginFormState.setValue(new LoginFormState(true));
        }
    }

    // A placeholder username validation check
    private boolean isUserNameValid(String username)
    {
        if (username == null)
        {
            return false;
        }
        if (username.contains("@"))
        {
            return Patterns.EMAIL_ADDRESS.matcher(username).matches();
        } else
        {
            return !username.trim().isEmpty();
        }
    }

    // A placeholder password validation check
    private boolean isPasswordValid(String password)
    {
        return password != null && password.trim().length() > 8;
    }
}