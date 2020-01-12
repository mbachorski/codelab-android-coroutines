/*
 * Copyright (C) 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.kotlincoroutines.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.kotlincoroutines.util.singleArgViewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * MainViewModel designed to store and manage UI-related data in a lifecycle conscious way. This
 * allows data to survive configuration changes such as screen rotations. In addition, background
 * work such as fetching network results can continue through configuration changes and deliver
 * results after the new Fragment or Activity is available.
 *
 * @param repository the data source this ViewModel will fetch results from.
 */
class MainViewModel(private val repository: TitleRepository) : ViewModel() {

  companion object {
    /**
     * Factory for creating [MainViewModel]
     *
     * @param arg the repository to pass to [MainViewModel]
     */
    val FACTORY = singleArgViewModelFactory(::MainViewModel)
  }

  /**
   * Request a snackbar to display a string.
   *
   * This variable is private because we don't want to expose MutableLiveData
   *
   * MutableLiveData allows anyone to set a value, and MainViewModel is the only
   * class that should be setting values.
   */
  private val _snackBar = MutableLiveData<String?>()

  /**
   * Request a snackbar to display a string.
   */
  val snackbar: LiveData<String?>
    get() = _snackBar

  /**
   * Update title text via this LiveData
   */
  val title = repository.title

  private val _spinner = MutableLiveData<Boolean>(false)

  /**
   * Show a loading spinner if true
   */
  val spinner: LiveData<Boolean>
    get() = _spinner

  /**
   * Count of taps on the screen
   */
  private var tapCount = 0

  /**
   * LiveData with formatted tap count.
   */
  private val _taps = MutableLiveData<String>("$tapCount taps")

  /**
   * Public view of tap live data.
   */
  val taps: LiveData<String>
    get() = _taps

  /**
   * Respond to onClick events by refreshing the title.
   *
   * The loading spinner will display until a result is returned, and errors will trigger
   * a snackbar.
   */
  fun onMainViewClicked() {
    refreshTitle()
    updateTaps()
  }

  /**
   * Wait one second then update the tap count.
   */
  private fun updateTaps() {
    tapCount++
    viewModelScope.launch {
      delay(2_000)
      _taps.postValue("$tapCount taps")
    }
  }

  /**
   * Called immediately after the UI shows the snackbar.
   */
  fun onSnackbarShown() {
    _snackBar.value = null
  }

  /**
   * Refresh the title, showing a loading spinner while it refreshes and errors via snackbar.
   * Note that the onCompleted callback is not passed the title. Since we write all titles to the Room database, the UI updates to the current title by observing a LiveData that's updated by Room.
   *In the update to coroutines, we'll keep the exact same behavior. It's a good pattern to use an observable data source like a Room database to automatically keep the UI up to date.
   */
  private fun refreshTitle() {
    launchDataLoad {
      repository.refreshTitle()
    }
  }

  private fun launchDataLoad(lambdaBlock: suspend () -> Unit): Job {
    // When creating a coroutine from a non-coroutine, start with launch.
    //That way, if they throw an uncaught exception it'll automatically be propagated to uncaught exception handlers (which by default crash the app). A coroutine started with async won't throw an exception to its caller until you call await. However, you can only call await from inside a coroutine, since it is a suspend function.
    //Once inside a coroutine, you can use launch or async to start child coroutines. Use launch for when you don't have a result to return, and async when you do.
    return viewModelScope.launch {
      try {
        _spinner.value = true
        lambdaBlock()
      } catch (error: TitleRefreshError) {
        _snackBar.value = error.message
      } finally {
        _spinner.value = false
      }
    }
  }
}
