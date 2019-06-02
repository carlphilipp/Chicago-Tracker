package fr.cph.chicago.espresso

import androidx.test.espresso.IdlingResource
import fr.cph.chicago.core.activity.BaseActivity

class LoginIdlingResource constructor(private val baseActivity: BaseActivity) : IdlingResource {

    private var resourceCallback: IdlingResource.ResourceCallback? = null

    override fun getName(): String {
        return LoginIdlingResource::class.java.name
    }

    override fun isIdleNow(): Boolean {
        if (!baseActivity.inProgress) {
            resourceCallback?.onTransitionToIdle()
        }
        return !baseActivity.inProgress
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.resourceCallback = callback
    }
}
