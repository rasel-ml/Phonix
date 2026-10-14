package app.mrm.phonix.di.factory.fragment

import app.mrm.phonix.ui.settings.SettingsFragment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FragmentFactoryImpl @Inject constructor() : FragmentFactory {
    override fun getSettingsFragment() = SettingsFragment()
}