package app.mrm.phonix.lib.di.module

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import app.mrm.phonix.lib.interactor.dialog.DialogsInteractor
import app.mrm.phonix.lib.interactor.dialog.DialogsInteractorImpl
import app.mrm.phonix.lib.interactor.prompt.PromptsInteractor
import app.mrm.phonix.lib.interactor.prompt.PromptsInteractorImpl
import app.mrm.phonix.lib.interactor.screen.ScreensInteractor
import app.mrm.phonix.lib.interactor.screen.ScreensInteractorImpl
import app.mrm.phonix.lib.ui.base.BaseActivity
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext

@InstallIn(ActivityComponent::class)
@Module(includes = [ActivityModule.BindsModule::class])
class ActivityModule {
    @Provides
    fun provideBaseActivity(@ActivityContext context: Context): BaseActivity<*> =
        context as BaseActivity<*>

    @Provides
    fun provideFragmentManager(@ActivityContext context: Context): FragmentManager =
        (context as AppCompatActivity).supportFragmentManager

    @Module
    @InstallIn(ActivityComponent::class)
    interface BindsModule {
        @Binds
        fun bindDialogsInteractor(dialogsInteractorImpl: DialogsInteractorImpl): DialogsInteractor

        @Binds
        fun bindScreensInteractor(screensInteractorImpl: ScreensInteractorImpl): ScreensInteractor

        @Binds
        fun bindPromptsInteractor(promptsInteractorImpl: PromptsInteractorImpl): PromptsInteractor
    }
}