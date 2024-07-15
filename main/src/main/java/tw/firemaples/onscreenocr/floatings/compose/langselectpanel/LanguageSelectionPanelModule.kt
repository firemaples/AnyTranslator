package tw.firemaples.onscreenocr.floatings.compose.langselectpanel

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface LanguageSelectionPanelModule {
    @Binds
    fun bindResultViewModel(
        languageSelectionPanelViewModelImpl: LanguageSelectionPanelViewModelImpl
    ): LanguageSelectionPanelViewModel
}
