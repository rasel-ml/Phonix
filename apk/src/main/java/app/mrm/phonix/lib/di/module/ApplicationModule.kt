package app.mrm.phonix.lib.di.module

import android.app.KeyguardManager
import android.app.UiModeManager
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.media.AudioManager
import android.os.PowerManager
import android.os.Vibrator
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.view.inputmethod.InputMethodManager
import androidx.core.app.NotificationManagerCompat
import app.mrm.phonix.lib.data.repository.calls.CallsRepository
import app.mrm.phonix.lib.data.repository.calls.CallsRepositoryImpl
import app.mrm.phonix.lib.data.repository.contacts.ContactsRepository
import app.mrm.phonix.lib.data.repository.contacts.ContactsRepositoryImpl
import app.mrm.phonix.lib.data.repository.phones.PhonesRepository
import app.mrm.phonix.lib.data.repository.phones.PhonesRepositoryImpl
import app.mrm.phonix.lib.data.repository.rawcontacts.RawContactsRepository
import app.mrm.phonix.lib.data.repository.rawcontacts.RawContactsRepositoryImpl
import app.mrm.phonix.lib.data.repository.recents.RecentsRepository
import app.mrm.phonix.lib.data.repository.recents.RecentsRepositoryImpl
import app.mrm.phonix.lib.di.factory.contentresolver.ContentResolverFactory
import app.mrm.phonix.lib.di.factory.contentresolver.ContentResolverFactoryImpl
import app.mrm.phonix.lib.di.factory.fragment.FragmentFactory
import app.mrm.phonix.lib.di.factory.fragment.FragmentFactoryImpl
import app.mrm.phonix.lib.interactor.animation.AnimationsInteractor
import app.mrm.phonix.lib.interactor.animation.AnimationsInteractorImpl
import app.mrm.phonix.lib.interactor.audio.AudiosInteractor
import app.mrm.phonix.lib.interactor.audio.AudiosInteractorImpl
import app.mrm.phonix.lib.interactor.blocked.BlockedInteractor
import app.mrm.phonix.lib.interactor.blocked.BlockedInteractorImpl
import app.mrm.phonix.lib.interactor.callaudio.CallAudiosInteractor
import app.mrm.phonix.lib.interactor.callaudio.CallAudiosInteractorImpl
import app.mrm.phonix.lib.interactor.calls.CallsInteractor
import app.mrm.phonix.lib.interactor.calls.CallsInteractorImpl
import app.mrm.phonix.lib.interactor.color.ColorsInteractor
import app.mrm.phonix.lib.interactor.color.ColorsInteractorImpl
import app.mrm.phonix.lib.interactor.contacts.ContactsInteractor
import app.mrm.phonix.lib.interactor.contacts.ContactsInteractorImpl
import app.mrm.phonix.lib.interactor.drawable.DrawablesInteractor
import app.mrm.phonix.lib.interactor.drawable.DrawablesInteractorImpl
import app.mrm.phonix.lib.interactor.navigation.NavigationsInteractor
import app.mrm.phonix.lib.interactor.navigation.NavigationsInteractorImpl
import app.mrm.phonix.lib.interactor.permission.PermissionsInteractor
import app.mrm.phonix.lib.interactor.permission.PermissionsInteractorImpl
import app.mrm.phonix.lib.interactor.phoneaccounts.PhonesInteractor
import app.mrm.phonix.lib.interactor.phoneaccounts.PhonesInteractorImpl
import app.mrm.phonix.lib.interactor.preferences.PreferencesInteractor
import app.mrm.phonix.lib.interactor.preferences.PreferencesInteractorImpl
import app.mrm.phonix.lib.interactor.proximity.ProximitiesInteractor
import app.mrm.phonix.lib.interactor.proximity.ProximitiesInteractorImpl
import app.mrm.phonix.lib.interactor.rawcontacts.RawContactsInteractor
import app.mrm.phonix.lib.interactor.rawcontacts.RawContactsInteractorImpl
import app.mrm.phonix.lib.interactor.recents.RecentsInteractor
import app.mrm.phonix.lib.interactor.recents.RecentsInteractorImpl
import app.mrm.phonix.lib.interactor.sim.SimsInteractor
import app.mrm.phonix.lib.interactor.sim.SimsInteractorImpl
import app.mrm.phonix.lib.interactor.string.StringsInteractor
import app.mrm.phonix.lib.interactor.string.StringsInteractorImpl
import app.mrm.phonix.lib.interactor.telecom.TelecomInteractor
import app.mrm.phonix.lib.interactor.telecom.TelecomInteractorImpl
import app.mrm.phonix.lib.interactor.theme.ThemesInteractor
import app.mrm.phonix.lib.interactor.theme.ThemesInteractorImpl
import app.mrm.phonix.lib.util.PreferencesManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.reactivex.disposables.CompositeDisposable

@InstallIn(SingletonComponent::class)
@Module(includes = [ApplicationModule.BindsModule::class])
class ApplicationModule {
    @Provides
    fun provideDisposables(): CompositeDisposable = CompositeDisposable()

    @Provides
    fun provideVibrator(@ApplicationContext context: Context): Vibrator =
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    @Provides
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver =
        context.contentResolver
    //region manager

    @Provides
    fun provideUiManager(@ApplicationContext context: Context): UiModeManager =
        context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager

    @Provides
    fun providePowerManager(@ApplicationContext context: Context): PowerManager =
        context.getSystemService(Context.POWER_SERVICE) as PowerManager

    @Provides
    fun provideAudioManager(@ApplicationContext context: Context): AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @Provides
    fun provideTelecomManager(@ApplicationContext context: Context): TelecomManager =
        context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

    @Provides
    fun provideKeyguardManager(@ApplicationContext context: Context): KeyguardManager =
        context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

    @Provides
    fun provideTelephonyManager(@ApplicationContext context: Context): TelephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    @Provides
    fun provideClipboardManager(@ApplicationContext context: Context): ClipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    @Provides
    fun provideInputMethodManager(@ApplicationContext context: Context): InputMethodManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    @Provides
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager =
        PreferencesManager.getInstance(context)

    @Provides
    fun provideSubscriptionManager(@ApplicationContext context: Context): SubscriptionManager =
        context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager

    @Provides
    fun provideNotificationManager(@ApplicationContext context: Context): NotificationManagerCompat =
        NotificationManagerCompat.from(context)

    //endregion

    @Module
    @InstallIn(SingletonComponent::class)
    interface BindsModule {
        //region repository

        @Binds
        fun bindCallsRepository(callsRepositoryImpl: CallsRepositoryImpl): CallsRepository

        @Binds
        fun bindPhonesRepository(phonesRepositoryImpl: PhonesRepositoryImpl): PhonesRepository

        @Binds
        fun bindRecentsRepository(recentsRepositoryImpl: RecentsRepositoryImpl): RecentsRepository

        @Binds
        fun bindContactsRepository(contactsRepositoryImpl: ContactsRepositoryImpl): ContactsRepository

        @Binds
        fun bindRawContactsRepoistory(rawContactsRepositoryImpl: RawContactsRepositoryImpl): RawContactsRepository

        //endregion

        //region factory

        @Binds
        fun bindFragmentFactory(fragmentFactoryImpl: FragmentFactoryImpl): FragmentFactory

        @Binds
        fun bindContentResolverFactory(contentResolverFactoryImpl: ContentResolverFactoryImpl): ContentResolverFactory

        //endregion

        //region interactor

        @Binds
        fun bindSimsInteractor(simsInteractorImpl: SimsInteractorImpl): SimsInteractor

        @Binds
        fun bindThemesInteractor(themesInteractor: ThemesInteractorImpl): ThemesInteractor

        @Binds
        fun bindCallsInteractor(callsInteractorImpl: CallsInteractorImpl): CallsInteractor

        @Binds
        fun bindColorsInteractor(colorsInteractorImpl: ColorsInteractorImpl): ColorsInteractor

        @Binds
        fun bindAudiosInteractor(audiosInteractorImpl: AudiosInteractorImpl): AudiosInteractor

        @Binds
        fun bindPhonesInteractor(phonesInteractorImpl: PhonesInteractorImpl): PhonesInteractor

        @Binds
        fun bindStringsInteractor(stringsInteractorImpl: StringsInteractorImpl): StringsInteractor

        @Binds
        fun bindBlockedInteractor(blockedInteractorImpl: BlockedInteractorImpl): BlockedInteractor

        @Binds
        fun bindTelecomInteractor(telecomInteractorImpl: TelecomInteractorImpl): TelecomInteractor

        @Binds
        fun bindRecentsInteractor(recentsInteractorImpl: RecentsInteractorImpl): RecentsInteractor

        @Binds
        fun bindContactsInteractor(contactsInteractorImpl: ContactsInteractorImpl): ContactsInteractor

        @Binds
        fun bindDrawablesInteractor(drawablesInteractorImpl: DrawablesInteractorImpl): DrawablesInteractor

        @Binds
        fun bindAnimationsInteractor(animationsInteractorImpl: AnimationsInteractorImpl): AnimationsInteractor

        @Binds
        fun bindCallAudiosInteractor(callAudiosInteractorImpl: CallAudiosInteractorImpl): CallAudiosInteractor

        @Binds
        fun bindRawContactsInteractor(rawContactsInteractorImpl: RawContactsInteractorImpl): RawContactsInteractor

        @Binds
        fun bindProximitiesInteractor(proximitiesInteractorImpl: ProximitiesInteractorImpl): ProximitiesInteractor

        @Binds
        fun bindPermissionsInteractor(permissionsInteractorImpl: PermissionsInteractorImpl): PermissionsInteractor

        @Binds
        fun bindPreferencesInteractor(preferencesInteractorImpl: PreferencesInteractorImpl): PreferencesInteractor

        @Binds
        fun bindNavigationsInteractor(navigationsInteractorImpl: NavigationsInteractorImpl): NavigationsInteractor

        //endregion
    }
}