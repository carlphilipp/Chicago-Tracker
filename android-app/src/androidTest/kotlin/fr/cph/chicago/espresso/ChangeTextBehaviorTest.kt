package fr.cph.chicago.espresso

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import fr.cph.chicago.R
import fr.cph.chicago.core.activity.BaseActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@LargeTest
class ChangeTextBehaviorKtTest {

    @get:Rule
    var activityScenarioRule = activityScenarioRule<BaseActivity>()

    private lateinit var idlingResource: IdlingResource


/*    @Before
    fun init() {
        activityScenarioRule.scenario.onActivity { baseActivity ->
            idlingResource = LoginIdlingResource(baseActivity)
            IdlingRegistry.getInstance().register(idlingResource)
        }
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(idlingResource)
    }*/

    @Test
    fun changeText_sameActivity() {

        activityScenarioRule.scenario.onActivity { baseActivity ->
            idlingResource = LoginIdlingResource(baseActivity)
            IdlingRegistry.getInstance().register(idlingResource)
        }

        // Make sure Espresso does not time out
        /*val waitingTime = 2000L
        IdlingPolicies.setMasterPolicyTimeout(waitingTime * 2, TimeUnit.MILLISECONDS);
        IdlingPolicies.setIdlingResourceTimeout(waitingTime * 2, TimeUnit.MILLISECONDS);*/

        // Now we wait
        //val idlingResource = ElapsedTimeIdlingResource(waitingTime)
        //Espresso.registerIdlingResources(idlingResource)


        //assert(true)

        //onView(withId(R.id.no_fav)).check(matches(withText("Welcome to Chicago Commutes!")))

        //onView(isRoot()).perform(waitId(R.id.toolbar, 1000L))

        //onView(isRoot()).perform(waitId(R.id.toolbar, TimeUnit.SECONDS.toMillis(1)))
        //onView(withId(R.id.no_fav)).check(matches(withText("Welcome to Chicago Commutes!")))

        //onView(isRoot()).perform(waitFor(2000))
        //onView(withId(R.id.no_fav)).check(matches(withText("Welcome to Chicago Commutes!")))
        onView(withId(R.id.main_drawer)).perform(click())

        IdlingRegistry.getInstance().unregister(idlingResource)

        // Clean up
        //Espresso.unregisterIdlingResources(idlingResource)
    }

/*    @Test
    fun changeText_newActivity() {
        // Type text and then press the button.

        assert(true)
    }*/

    companion object {

        val STRING_TO_BE_TYPED = "Espresso"
    }
}
