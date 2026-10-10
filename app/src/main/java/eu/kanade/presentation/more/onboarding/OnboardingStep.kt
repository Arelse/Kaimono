package eu.kanade.presentation.more.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

internal interface OnboardingStep {

    val isComplete: Boolean

    /** Icon shown in the step's header circle (steps 2+) or hero circle (step 1). */
    val icon: ImageVector

    /** Step heading, e.g. "Storage Setup". Step 1 (ThemeStep) uses its own "Welcome!" heading
     * instead and doesn't read this. */
    val title: String

    @Composable
    fun Content()
}
