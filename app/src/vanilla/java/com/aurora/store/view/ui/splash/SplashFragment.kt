/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.store.view.ui.splash

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.lifecycle.lifecycleScope
import com.aurora.store.R
import com.aurora.store.data.model.AuthState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.random.Random

@AndroidEntryPoint
class SplashFragment : BaseFlavouredSplashFragment() {

    private data class BallState(
        var x: Float = 0f,
        var y: Float = 0f,
        var velocityX: Float = 1f,
        var velocityY: Float = 1f
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide "Log in using" text
        view.findViewById<View>(R.id.txt_action)?.visibility = View.GONE

        // Hide status text initially to avoid showing "null"
        view.findViewById<View>(R.id.txt_status)?.visibility = View.GONE

        // Add animations to the UI
        animateSplashScreen(view)

        // Immediately trigger anonymous login
        viewLifecycleOwner.lifecycleScope.launch {
            // Set device spoof before any login attempt
            setReloadedBerylliumSpoof()

            // Check current state and trigger login if needed
            val currentState = viewModel.authState.value
            if (currentState == AuthState.Unavailable || currentState == AuthState.SignedOut) {
                viewModel.buildAnonymousAuthData()
            }

            // Continue monitoring state for any future changes
            viewModel.authState.collect { state ->
                if (state == AuthState.Unavailable || state == AuthState.SignedOut) {
                    if (viewModel.authState.value != AuthState.Fetching) {
                        setReloadedBerylliumSpoof()
                        viewModel.buildAnonymousAuthData()
                    }
                }
            }
        }
    }

    private fun animateSplashScreen(view: View) {
        // Animate logo with subtle pulse and glow
        val logo = view.findViewById<View>(R.id.img_icon)
        val title = view.findViewById<View>(R.id.txt_title)

        // Fade in logo and title
        logo.alpha = 0f
        title.alpha = 0f
        ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f).apply {
            duration = 800
            start()
        }
        ObjectAnimator.ofFloat(title, "alpha", 0f, 1f).apply {
            duration = 1000
            startDelay = 200
            start()
        }

        ObjectAnimator.ofFloat(logo, "scaleX", 1f, 1.08f, 1f).apply {
            duration = 2500
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        ObjectAnimator.ofFloat(logo, "scaleY", 1f, 1.08f, 1f).apply {
            duration = 2500
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        ObjectAnimator.ofFloat(logo, "rotation", -2f, 2f, -2f).apply {
            duration = 4000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

        // Subtle ambient floating bubbles
        val circles = listOf(
            view.findViewById<View>(R.id.circle1),
            view.findViewById<View>(R.id.circle2),
            view.findViewById<View>(R.id.circle3),
            view.findViewById<View>(R.id.circle4),
            view.findViewById<View>(R.id.circle5),
            view.findViewById<View>(R.id.circle6)
        )

        view.post {
            val screenWidth = view.width.toFloat()
            val screenHeight = view.height.toFloat()

            // Very slow speeds for calm ambient effect
            val speeds = listOf(0.4f, 0.5f, 0.6f, 0.45f, 0.55f, 0.5f)
            circles.forEachIndexed { index, circle ->
                circle?.let { bounceBall(it, screenWidth, screenHeight, speeds[index]) }
            }
        }
    }

    private fun bounceBall(ball: View, screenWidth: Float, screenHeight: Float, speed: Float) {
        val ballSize = ball.width.toFloat()
        val baseAlpha = ball.alpha // Remember original alpha
        val state = BallState(
            x = Random.nextFloat() * (screenWidth - ballSize),
            y = Random.nextFloat() * (screenHeight - ballSize),
            velocityX = (if (Random.nextBoolean()) speed else -speed),
            velocityY = (if (Random.nextBoolean()) speed else -speed)
        )

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = Long.MAX_VALUE
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE

            addUpdateListener {
                // Update position
                state.x += state.velocityX
                state.y += state.velocityY

                // Bounce off edges smoothly
                if (state.x <= 0 || state.x >= screenWidth - ballSize) {
                    state.velocityX = -state.velocityX
                    state.x = state.x.coerceIn(0f, screenWidth - ballSize)
                }

                if (state.y <= 0 || state.y >= screenHeight - ballSize) {
                    state.velocityY = -state.velocityY
                    state.y = state.y.coerceIn(0f, screenHeight - ballSize)
                }

                // Apply position
                ball.translationX = state.x
                ball.translationY = state.y
            }

            start()
        }

        // Very slow rotation for subtle movement
        ObjectAnimator.ofFloat(ball, "rotation", 0f, 360f).apply {
            duration = 60000 // 1 minute for full rotation
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }

        // Gentle breathing effect on alpha
        ObjectAnimator.ofFloat(ball, "alpha", baseAlpha, baseAlpha * 1.3f, baseAlpha).apply {
            duration = 8000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }
}
