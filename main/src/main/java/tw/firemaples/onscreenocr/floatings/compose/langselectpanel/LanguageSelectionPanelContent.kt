package tw.firemaples.onscreenocr.floatings.compose.langselectpanel

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import tw.firemaples.onscreenocr.R
import tw.firemaples.onscreenocr.theme.AppTheme

@Composable
fun LanguageSelectionPanelContent(viewModel: LanguageSelectionPanelViewModel) {
    val state by viewModel.state.collectAsState()

    Row(
        modifier =
            Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 16.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .width(150.dp)
                    .height(300.dp),
        ) {
            OCRLanguageListContent(
                ocrLanguageList = state.ocrLanguageList,
                onLanguageClicked = viewModel::onOCRLanguageClicked,
            )
        }

        Spacer(modifier = Modifier.size(4.dp))

        Column(
            modifier =
                Modifier
                    .width(150.dp)
                    .height(300.dp),
        ) {
            Column(
                modifier =
                    Modifier.weight(1f),
            ) {
                TranslationLanguageContent(
                    translationProviderList = state.translationProviderList,
                    translationLanguageList = state.translationLanguageList,
                    onProviderClicked = viewModel::onTranslationProvideClicked,
                    onLanguageClicked = viewModel::onTranslationLanguageClicked,
                )
            }

            TextButton(
                modifier = Modifier.align(Alignment.End),
                onClick = { /*TODO*/ },
            ) {
                Text(text = stringResource(id = R.string.text_close))
            }
        }
    }
}

@Composable
private fun OCRLanguageListContent(
    ocrLanguageList: List<LanguageItem>,
    onLanguageClicked: (LanguageItem) -> Unit,
) {
    Title(text = R.string.text_ocr_language)

    Spacer(modifier = Modifier.size(2.dp))

    LazyColumn {
        items(ocrLanguageList) { item ->
            LanguageItem(
                text = item.languageName,
                selected = item.selected,
                onClicked = {},
            )

            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TranslationLanguageContent(
    translationProviderList: List<TranslationProvider>,
    translationLanguageList: List<LanguageItem>,
    onProviderClicked: (TranslationProvider) -> Unit,
    onLanguageClicked: (LanguageItem) -> Unit,
) {
    Title(text = R.string.text_translation)

    Spacer(modifier = Modifier.size(2.dp))

    var providerExpanded by remember { mutableStateOf(false) }
    val selectProviderName = translationProviderList.firstOrNull { it.selected }?.displayName

    ExposedDropdownMenuBox(
        expanded = providerExpanded,
        onExpandedChange = { providerExpanded = it },
    ) {
        TextField(
            modifier = Modifier.menuAnchor(),
            value = selectProviderName.orEmpty(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(text = stringResource(id = R.string.text_field_provider)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = providerExpanded,
            onDismissRequest = { providerExpanded = false },
        ) {
            translationProviderList.forEach { provider ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = provider.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    onClick = { onProviderClicked.invoke(provider) },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }

    Spacer(modifier = Modifier.size(2.dp))

    LazyColumn {
        items(translationLanguageList) { item ->
            LanguageItem(
                text = item.languageName,
                selected = item.selected,
                onClicked = {
                    onLanguageClicked.invoke(item)
                },
            )
        }
    }
}

@Composable
private fun Title(
    @StringRes text: Int,
) {
    Text(
        text = stringResource(id = text),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
    )

    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun LanguageItem(
    text: String,
    selected: Boolean,
    onClicked: () -> Unit,
) {
    val background =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        }
    val textColor =
        if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    Text(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(color = background)
                .padding(horizontal = 4.dp, vertical = 1.dp)
                .clickable(onClick = onClicked),
        text = text,
        color = textColor,
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LanguageSelectionPanelContentPreview() {
    val languageList =
        listOf(
            LanguageItem(
                key = "en",
                languageName = "English",
                selected = true,
            ),
            LanguageItem(
                key = "zh",
                languageName = "Chinese",
                selected = false,
            ),
        )
    val translationProviderList =
        listOf(
            TranslationProvider(
                key = "p1",
                displayName = "Provider1",
                selected = true,
            ),
            TranslationProvider(
                key = "p2",
                displayName = "Provider2",
                selected = false,
            ),
        )
    val state =
        LanguageSelectionPanelState(
            ocrLanguageList = languageList,
            translationProviderList = translationProviderList,
            translationLanguageList = languageList,
        )
    val viewModel =
        object : LanguageSelectionPanelViewModel {
            override val state = MutableStateFlow(state)

            override fun onOCRLanguageClicked(item: LanguageItem) = Unit

            override fun onTranslationProvideClicked(translationProvider: TranslationProvider) = Unit

            override fun onTranslationLanguageClicked(languageItem: LanguageItem) = Unit
        }

    AppTheme {
        LanguageSelectionPanelContent(viewModel = viewModel)
    }
}
