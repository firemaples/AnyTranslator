package tw.firemaples.onscreenocr.floatings.compose.langselectpanel

import android.content.Context
import android.view.WindowManager
import androidx.compose.runtime.Composable
import dagger.hilt.android.qualifiers.ApplicationContext
import tw.firemaples.onscreenocr.floatings.compose.base.ComposeFloatingView
import tw.firemaples.onscreenocr.utils.Logger
import javax.inject.Inject

class LanguageSelectionPanel @Inject constructor(
    @ApplicationContext context: Context,
    private val viewModel: LanguageSelectionPanelViewModel,
) : ComposeFloatingView(context) {
//    override val layoutId: Int
//        get() = R.layout.floating_translation_select_panel

    override val layoutWidth: Int
        get() = WindowManager.LayoutParams.MATCH_PARENT

    override val layoutHeight: Int
        get() = WindowManager.LayoutParams.MATCH_PARENT

    private val logger: Logger by lazy { Logger(this::class) }

    @Composable
    override fun RootContent() {
        LanguageSelectionPanelContent(viewModel)
    }

//    private val viewModel: TranslationSelectPanelViewModel by lazy {
//        TranslationSelectPanelViewModel(viewScope)
//    }
//
//    private val binding: FloatingTranslationSelectPanelBinding =
//        FloatingTranslationSelectPanelBinding.bind(rootLayout)
//
//    private val translationProviderMenuView: MenuView by lazy {
//        MenuView(context, checkable = true).apply {
//            setAnchor(binding.tvTranslationProvider)
//            onItemSelected = { view, key ->
//                logger.debug("onItemSelected: $key")
//
//                view.detachFromScreen()
//                viewModel.onTranslationProviderSelected(key)
//            }
//        }
//    }
//
//    private val ocrLangListLayoutManager by lazy {
//        LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
//    }
//    private val translationLangListLayoutManager by lazy {
//        LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
//    }
//    private lateinit var ocrLangListAdapter: LangListAdapter<OCRLangItem>
//    private lateinit var translationLangListAdapter: LangListAdapter<TranslateLangItem>
//
//    init {
//        setViews()
//        loadData()
//    }
//
//    private fun setViews() {
//        binding.viewOutside.clickOnce { detachFromScreen() }
//        binding.btClose.clickOnce { detachFromScreen() }
//
//        with(binding.rvOcrLang) {
//            layoutManager = ocrLangListLayoutManager
//            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
//            ocrLangListAdapter = LangListAdapter(
//                context = context,
//                diffCallback = object : DiffUtil.ItemCallback<OCRLangItem>() {
//                    override fun areItemsTheSame(
//                        oldItem: OCRLangItem, newItem: OCRLangItem
//                    ): Boolean =
//                        oldItem.code == newItem.code && oldItem.recognizer == newItem.recognizer
//
//                    override fun areContentsTheSame(
//                        oldItem: OCRLangItem, newItem: OCRLangItem
//                    ): Boolean = oldItem == newItem
//                },
//                onItemClicked = {
//                    logger.debug("on OCR lang checked, $it")
//                    viewModel.onOCRLangSelected(it)
//                },
//                onLongClicked = {
//                    logger.debug("on OCR lang long clicked: $it")
//                    viewModel.onOCRLangLongClicked(it.code)
//                }
//            )
//            adapter = ocrLangListAdapter
//        }
//
//        with(binding.rvTranslationLang) {
//            layoutManager = translationLangListLayoutManager
//            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
//            translationLangListAdapter = LangListAdapter(
//                context = context,
//                diffCallback = object : DiffUtil.ItemCallback<TranslateLangItem>() {
//                    override fun areItemsTheSame(
//                        oldItem: TranslateLangItem, newItem: TranslateLangItem
//                    ): Boolean = oldItem.code == newItem.code
//
//                    override fun areContentsTheSame(
//                        oldItem: TranslateLangItem,
//                        newItem: TranslateLangItem
//                    ): Boolean = oldItem == newItem
//                },
//                onItemClicked = {
//                    logger.debug("on translation lang checked, $it")
//
//                    viewModel.onTranslationLangChecked(it.code)
//                },
//                onLongClicked = {
//                    logger.debug("on translation lang long clicked: $it")
//                    viewModel.onTranslationLangLongClicked(it.code)
//                })
//            adapter = translationLangListAdapter
//        }
//
//        binding.tvTranslationProvider.clickOnce {
//            viewModel.onTranslationProviderClicked()
//        }
//
//        viewModel.ocrLanguageList.observe(lifecycleOwner) {
//            val (list, scrollToPosition) = it
//            logger.debug("on ocrLanguageList changed, scrollToPosition: $scrollToPosition, list: $list")
//
//            ocrLangListAdapter.submitList(list) {
//                if (scrollToPosition) {
//                    val indices = list.mapIndexedNotNull { index, item ->
//                        if (item.selected) index else null
//                    }
//                    val first = ocrLangListLayoutManager.findFirstCompletelyVisibleItemPosition()
//                    val last = ocrLangListLayoutManager.findLastCompletelyVisibleItemPosition()
//                    if (!indices.any { index -> index in first..last }) {
//                        binding.rvOcrLang.scrollToPosition(
//                            list.indexOfFirst { item -> item.selected }
//                                .coerceAtLeast(0))
//                    }
//                }
//            }
//        }
//
//        viewModel.selectedTranslationProviderName.observe(lifecycleOwner) {
//            binding.tvTranslationProvider.text = it
//        }
//
//        viewModel.translationLangList.observe(lifecycleOwner) {
//            val (list, scrollToPosition) = it
//            translationLangListAdapter.submitList(list) {
//                if (scrollToPosition) {
//                    val indices = list.mapIndexedNotNull { index, item ->
//                        if (item.selected) index else null
//                    }
//                    val first =
//                        translationLangListLayoutManager.findFirstCompletelyVisibleItemPosition()
//                    val last =
//                        translationLangListLayoutManager.findLastCompletelyVisibleItemPosition()
//                    if (!indices.any { index -> index in first..last }) {
//                        binding.rvTranslationLang.scrollToPosition(
//                            list.indexOfFirst { item -> item.selected }
//                                .coerceAtLeast(0)
//                        )
//                    }
//                }
//            }
//        }
//
//        viewModel.displayTranslationHint.observe(lifecycleOwner) {
//            binding.tvTranslationLangHint.setTextOrGone(it)
//        }
//
//        viewModel.displayTranslateProviders.observe(lifecycleOwner) {
//            translationProviderMenuView.apply {
//                updateData(
//                    itemList = it.associate { it.key to it.displayName },
//                    selectedKey = it.firstOrNull { it.selected }?.key
//                )
//            }.attachToScreen()
//        }
//    }
//
//    override fun onBackButtonPressed(): Boolean {
//        detachFromScreen()
//        return true
//    }
//
//    private fun loadData() {
//        viewModel.load()
//    }
//
//    private class LangListAdapter<T : LangItem>(
//        private val context: Context,
//        diffCallback: DiffUtil.ItemCallback<T>,
//        private val onItemClicked: (lang: T) -> Unit,
//        private val onLongClicked: (lang: T) -> Unit,
//    ) :
//        ListAdapter<T, LangListAdapter.ViewHolder>(diffCallback) {
//        class ViewHolder(val binding: ItemLangListBinding) : RecyclerView.ViewHolder(binding.root)
//
//        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
//            ViewHolder(
//                ItemLangListBinding.inflate(context.getThemedLayoutInflater(), parent, false)
//            )
//
//        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//            val item = getItem(position)
//
//            with(holder.binding.lang) {
//                text = item.displayName
//                isChecked = item.selected
//                val drawableDownload =
//                    if (item.showDownloadIcon)
//                        ContextCompat.getDrawable(context, R.drawable.ic_download)
//                    else null
//                val drawableFavorite =
//                    if (item.favorite)
//                        ContextCompat.getDrawable(context, R.drawable.ic_heart)
//                    else null
//
//                setCompoundDrawablesRelativeWithIntrinsicBounds(
//                    drawableFavorite,
//                    null,
//                    drawableDownload,
//                    null
//                )
//                compoundDrawablePadding = UIUtils.dpToPx(1f)
//                setTextColor(
//                    ContextCompat.getColor(
//                        context,
//                        if (item.unrecommended) R.color.alert else R.color.foreground,
//                    )
//                )
//            }
//
//            holder.itemView.clickOnce { onItemClicked.invoke(item) }
//            holder.itemView.setOnLongClickListener {
//                onLongClicked.invoke(item)
//                true
//            }
//        }
//    }
}
