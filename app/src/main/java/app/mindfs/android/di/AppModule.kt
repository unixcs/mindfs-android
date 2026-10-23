package app.mindfs.android.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import app.mindfs.android.core.data.NodeRepository
import app.mindfs.android.core.datastore.AppSettingsStore
import app.mindfs.android.core.datastore.NodeStore
import app.mindfs.android.core.network.MindfsJson
import app.mindfs.android.core.security.KeystoreSecretStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json

private val Context.mindfsDataStore: DataStore<Preferences> by
preferencesDataStore(name = "mindfs_android")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = MindfsJson

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.mindfsDataStore

    @Provides
    @Singleton
    fun provideSecretStore(@ApplicationContext context: Context): KeystoreSecretStore =
        KeystoreSecretStore(context)

    @Provides
    @Singleton
    fun provideNodeStore(dataStore: DataStore<Preferences>): NodeStore = NodeStore(dataStore)

    @Provides
    @Singleton
    fun provideAppSettingsStore(dataStore: DataStore<Preferences>): AppSettingsStore =
        AppSettingsStore(dataStore)

    @Provides
    @Singleton
    fun provideNodeRepository(nodeStore: NodeStore): NodeRepository = NodeRepository(nodeStore)
}
