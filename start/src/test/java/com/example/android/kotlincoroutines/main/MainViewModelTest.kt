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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.android.kotlincoroutines.fakes.MainNetworkFake
import com.example.android.kotlincoroutines.fakes.TitleDaoFake
import com.example.android.kotlincoroutines.main.utils.MainCoroutineScopeRule
import com.example.android.kotlincoroutines.main.utils.getValueForTest
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * A rule is a way to run code before and after the execution of a test in JUnit. Two rules are used to allow us to test MainViewModel in an off-device test:
 * InstantTaskExecutorRule is a JUnit rule that configures LiveData to execute each task synchronously
 * MainCoroutineScopeRule is a custom rule in this codebase that configures Dispatchers.Main to use a TestCoroutineDispatcher from kotlinx-coroutines-test. This allows tests to advance a virtual-clock for testing, and allows code to use Dispatchers.Main in unit tests.
 * In the setup method, a new instance of MainViewModel is created using testing fakes – these are fake implementations of the network and database provided in the starter code to help write tests without using the real network or database.
 */
class MainViewModelTest {
  @get:Rule
  val coroutineScope = MainCoroutineScopeRule()
  @get:Rule
  val instantTaskExecutorRule = InstantTaskExecutorRule()

  lateinit var viewModel: MainViewModel

  @Before
  fun setup() {
    viewModel = MainViewModel(
        TitleRepository(
            MainNetworkFake("OK"),
            TitleDaoFake("initial")
        ))
  }

  @Test
  fun whenMainClicked_updatesTaps() {
    viewModel.onMainViewClicked()
    Truth.assertThat(viewModel.taps.getValueForTest()).isEqualTo("0 taps")

    coroutineScope.advanceTimeBy(2_000)
    Truth.assertThat(viewModel.taps.getValueForTest()).isEqualTo("1 taps")
  }
}