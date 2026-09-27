package com.editog.novaagent

import android.app.Application
import com.editog.novaagent.automation.AppLauncher
import com.editog.novaagent.automation.ContactsHelper
import com.editog.novaagent.automation.TelephonyHelper
import com.editog.novaagent.automation.VoiceManager
import com.editog.novaagent.data.api.AgentBrainOrchestrator
import com.editog.novaagent.data.repository.ApiConfigRepository
import com.editog.novaagent.data.repository.ChatRepository
import com.editog.novaagent.data.repository.SettingsRepository
import com.editog.novaagent.data.repository.VoicemailRepository

class NovaApplication : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var apiConfigRepository: ApiConfigRepository
        private set
    lateinit var chatRepository: ChatRepository
        private set
    lateinit var voicemailRepository: VoicemailRepository
        private set

    lateinit var appLauncher: AppLauncher
        private set
    lateinit var contactsHelper: ContactsHelper
        private set
    lateinit var telephonyHelper: TelephonyHelper
        private set
    lateinit var voiceManager: VoiceManager
        private set
    lateinit var brainOrchestrator: AgentBrainOrchestrator
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        settingsRepository = SettingsRepository(this)
        apiConfigRepository = ApiConfigRepository(this)
        chatRepository = ChatRepository(this)
        voicemailRepository = VoicemailRepository(this)

        appLauncher = AppLauncher(this)
        contactsHelper = ContactsHelper(this)
        telephonyHelper = TelephonyHelper(this)
        voiceManager = VoiceManager(this)
        brainOrchestrator = AgentBrainOrchestrator()
    }

    companion object {
        lateinit var instance: NovaApplication
            private set
    }
}
