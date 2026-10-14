package app.mrm.phonix.di.factory.fragment

import app.mrm.phonix.ui.settings.SettingsFragment

interface FragmentFactory {
    fun getSettingsFragment(): SettingsFragment
}