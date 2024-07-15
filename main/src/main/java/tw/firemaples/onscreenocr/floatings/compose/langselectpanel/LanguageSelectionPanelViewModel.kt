package tw.firemaples.onscreenocr.floatings.compose.langselectpanel

import android.content.Context
import androidx.compose.runtime.Immutable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import tw.firemaples.onscreenocr.di.MainImmediateCoroutineScope
import tw.firemaples.onscreenocr.floatings.manager.StateNavigator
import javax.inject.Inject

interface LanguageSelectionPanelViewModel {
    val state: StateFlow<LanguageSelectionPanelState>

    fun onOCRLanguageClicked(item: LanguageItem)

    fun onTranslationProvideClicked(translationProvider: TranslationProvider)

    fun onTranslationLanguageClicked(languageItem: LanguageItem)
}

@Immutable
data class LanguageSelectionPanelState(
    val ocrLanguageList: List<LanguageItem> = listOf(),
    val translationProviderList: List<TranslationProvider> = listOf(),
    val translationLanguageList: List<LanguageItem> = listOf(),
)

data class LanguageItem(
    val key: String,
    val languageName: String,
    val selected: Boolean,
)

data class TranslationProvider(
    val key: String,
    val displayName: String,
    val selected: Boolean,
)

class LanguageSelectionPanelViewModelImpl @Inject constructor(
        @ApplicationContext
        private val context: Context,
        @MainImmediateCoroutineScope
        private val scope: CoroutineScope,
        private val stateNavigator: StateNavigator,
    ) : LanguageSelectionPanelViewModel {
        override val state = MutableStateFlow(LanguageSelectionPanelState())

        init {

        }



        override fun onOCRLanguageClicked(item: LanguageItem) {
            TODO("Not yet implemented")
        }

        override fun onTranslationProvideClicked(translationProvider: TranslationProvider) {
            TODO("Not yet implemented")
        }

        override fun onTranslationLanguageClicked(languageItem: LanguageItem) {
            TODO("Not yet implemented")
        }
    }
