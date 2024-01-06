package tw.firemaples.onscreenocr.floatings.compose.mainbar

import android.content.Context
import android.graphics.Point
import androidx.compose.runtime.Composable
import dagger.hilt.android.qualifiers.ApplicationContext
import tw.firemaples.onscreenocr.databinding.FloatingMainBarBinding
import tw.firemaples.onscreenocr.floatings.ViewHolderService
import tw.firemaples.onscreenocr.floatings.compose.base.ComposeMovableFloatingView
import tw.firemaples.onscreenocr.floatings.compose.base.collectOnLifecycleResumed
import tw.firemaples.onscreenocr.floatings.history.VersionHistoryView
import tw.firemaples.onscreenocr.floatings.manager.NavState
import tw.firemaples.onscreenocr.floatings.manager.StateNavigator
import tw.firemaples.onscreenocr.floatings.menu.MenuView
import tw.firemaples.onscreenocr.floatings.readme.ReadmeView
import tw.firemaples.onscreenocr.floatings.translationSelectPanel.TranslationSelectPanel
import tw.firemaples.onscreenocr.log.FirebaseEvent
import tw.firemaples.onscreenocr.pages.setting.SettingActivity
import tw.firemaples.onscreenocr.pages.setting.SettingManager
import tw.firemaples.onscreenocr.utils.Utils
import tw.firemaples.onscreenocr.utils.clickOnce
import javax.inject.Inject

class MainBarFloatingView @Inject constructor(
    @ApplicationContext context: Context,
    private val stateNavigator: StateNavigator,
    private val viewModel: MainBarViewModel,
) : ComposeMovableFloatingView(context) {

//    override val layoutId: Int
//        get() = R.layout.floating_main_bar

    override val initialPosition: Point
        get() = viewModel.getInitialPosition()

    @Composable
    override fun RootContent() {
        viewModel.action.collectOnLifecycleResumed { action ->
            when (action) {
                MainBarAction.RescheduleFadeOut ->
                    rescheduleFadeOut()

                MainBarAction.OpenLanguageSelectionPanel -> {
                    rescheduleFadeOut()
                    // TODO wait to be refactored
                    TranslationSelectPanel(context).attachToScreen()
                }

                is MainBarAction.OpenBrowser ->
                    // TODO wait to be refactored
                    Utils.openBrowser(action.url)

                MainBarAction.OpenReadme ->
                    // TODO wait to be refactored
                    ReadmeView(context).attachToScreen()

                MainBarAction.OpenSettings ->
                    // TODO wait to be refactored
                    SettingActivity.start(context)

                MainBarAction.OpenVersionHistory ->
                    // TODO wait to be refactored
                    VersionHistoryView(context).attachToScreen()

                MainBarAction.HideMainBar ->
                    // TODO wait to be refactored
                    ViewHolderService.hideViews(context)

                MainBarAction.ExitApp ->
                    // TODO wait to be refactored
                    ViewHolderService.exit(context)
            }
        }

        MainBarContent(
            viewModel = viewModel,
            onDragStart = onDragStart,
            onDragEnd = {
                onDragEnd.invoke()
                viewModel.onDragEnd(params.x, params.y)
            },
            onDragCancel = onDragCancel,
            onDrag = onDrag,
        )
    }

    override val enableDeviceDirectionTracker: Boolean
        get() = true

    override val moveToEdgeAfterMoved: Boolean
        get() = true

    override val fadeOutAfterMoved: Boolean
        get() = !arrayOf(NavState.ScreenCircling, NavState.ScreenCircled)
            .contains(stateNavigator.currentNavState.value)
                && !menuView.attached
                && SettingManager.enableFadingOutWhileIdle
    override val fadeOutDelay: Long
        get() = SettingManager.timeoutToFadeOut
    override val fadeOutDestinationAlpha: Float
        get() = SettingManager.opaquePercentageToFadeOut

//    private val binding: FloatingMainBarBinding = FloatingMainBarBinding.bind(rootLayout)

    private val menuView: MenuView by lazy {
        MenuView(context, false).apply {
//            setAnchor(binding.btMenu)

            onAttached = { rescheduleFadeOut() }
            onDetached = { rescheduleFadeOut() }
            onItemSelected = { view, key ->
                view.detachFromScreen()
                viewModel.onMenuItemClicked(key)
                rescheduleFadeOut()
            }
        }
    }

    init {
//        binding.setViews()
//        setDragView(binding.btMenu)
    }

    private fun FloatingMainBarBinding.setViews() {
        btLangSelector.clickOnce {
            rescheduleFadeOut()
            TranslationSelectPanel(context).attachToScreen()
        }

        btSelect.clickOnce {
            viewModel.onSelectClicked()
        }

        btTranslate.clickOnce {
            FirebaseEvent.logClickTranslationStartButton()
            viewModel.onTranslateClicked()
        }

        btClose.clickOnce {
            viewModel.onCloseClicked()
        }

        btMenu.clickOnce {
            viewModel.onMenuButtonClicked()
        }

//        viewModel.languageText.observe(lifecycleOwner) {
//            tvLang.text = it
//            moveToEdgeIfEnabled()
//        }
//
//        viewModel.displayTranslatorIcon.observe(lifecycleOwner) {
//            if (it == null) {
//                ivGoogleTranslator.setImageDrawable(null)
//                ivGoogleTranslator.hide()
//            } else {
//                ivGoogleTranslator.setImageResource(it)
//                ivGoogleTranslator.show()
//            }
//            moveToEdgeIfEnabled()
//        }
//
//        viewModel.displaySelectButton.observe(lifecycleOwner) {
//            btSelect.showOrHide(it)
//            moveToEdgeIfEnabled()
//        }
//
//        viewModel.displayTranslateButton.observe(lifecycleOwner) {
//            btTranslate.showOrHide(it)
//            moveToEdgeIfEnabled()
//        }
//
//        viewModel.displayCloseButton.observe(lifecycleOwner) {
//            btClose.showOrHide(it)
//            moveToEdgeIfEnabled()
//        }
//
//        viewModel.displayMenuItems.observe(lifecycleOwner) {
//            with(menuView) {
//                updateData(it)
//                attachToScreen()
//            }
//        }
//
//        viewModel.rescheduleFadeOut.observe(lifecycleOwner) {
//            rescheduleFadeOut()
//        }
//
//        viewModel.showSettingPage.observe(lifecycleOwner) {
//            SettingActivity.start(context)
//        }
//
//        viewModel.openBrowser.observe(lifecycleOwner) {
//            Utils.openBrowser(it)
//        }
//
//        viewModel.showVersionHistory.observe(lifecycleOwner) {
//            VersionHistoryView(context).attachToScreen()
//        }
//
//        viewModel.showReadme.observe(lifecycleOwner) {
//            ReadmeView(context).attachToScreen()
//        }
    }

    override fun attachToScreen() {
        super.attachToScreen()
        viewModel.onAttachedToScreen()
    }
}
