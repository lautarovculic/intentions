package com.lautarovculic.intentions.ui.screens.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lautarovculic.intentions.core.intent.IntentExporter
import com.lautarovculic.intentions.core.model.ComponentRecord
import com.lautarovculic.intentions.core.model.ComponentType
import com.lautarovculic.intentions.core.model.DispatchType
import com.lautarovculic.intentions.core.model.IntentSpec
import com.lautarovculic.intentions.core.model.TargetingMode
import com.lautarovculic.intentions.ui.components.KeyValueRow
import com.lautarovculic.intentions.ui.components.Pill
import com.lautarovculic.intentions.ui.components.SectionCard
import com.lautarovculic.intentions.ui.components.copyToClipboard
import com.lautarovculic.intentions.ui.navigation.Routes
import com.lautarovculic.intentions.ui.rememberAppContainer
import com.lautarovculic.intentions.ui.theme.AccentCyan
import com.lautarovculic.intentions.ui.theme.AccentGreen
import com.lautarovculic.intentions.ui.theme.AccentPurple
import com.lautarovculic.intentions.ui.theme.AccentRed
import com.lautarovculic.intentions.ui.theme.MonoSmall

@Composable
fun ComponentsScreen(packageName: String, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val container = rememberAppContainer()
    val vm: ComponentsViewModel = viewModel(
        key = "components-$packageName",
        factory = ComponentsViewModel.factory(container, packageName),
    )
    val ui by vm.ui.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text(packageName, style = MonoSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
        FlowRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(ui.exportedOnly, { vm.toggleExportedOnly() }, label = { Text("Exported only") })
            FilterChip(ui.rootEnriched, { vm.load(enrich = !ui.rootEnriched) }, label = { Text("Enrich deep links (root)") })
        }
        if (ui.loading) LinearProgressIndicator(Modifier.fillMaxWidth())

        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text(
                    "${ui.visible.size} components" + if (ui.rootEnriched) " · ${ui.rootDeepLinks.size} dumpsys filters" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            items(ui.visible, key = { it.type.name + it.className }) { comp ->
                ComponentCard(
                    comp = comp,
                    onBuilder = {
                        container.sendToBuilder(comp.toIntentSpec())
                        onNavigate(Routes.BUILDER)
                    },
                    onProvider = {
                        container.sendToProvider(comp.toProviderSpec())
                        onNavigate(Routes.PROVIDER)
                    },
                    onCopyAdb = { copyToClipboard(ctx, "adb command", IntentExporter.adb(comp.toIntentSpec())) },
                    onCopyComponent = { copyToClipboard(ctx, "component", comp.shortString) },
                )
            }
            if (ui.rootEnriched && ui.rootDeepLinks.isNotEmpty()) {
                item {
                    SectionCard("Deep links (root dumpsys)", trailing = { Pill("root", AccentPurple) }) {
                        ui.rootDeepLinks.forEach { filter ->
                            KeyValueRow("schemes", filter.schemes.joinToString(", ").ifEmpty { null })
                            KeyValueRow("hosts", filter.hosts.joinToString(", ").ifEmpty { null })
                            KeyValueRow("actions", filter.actions.joinToString(", ").ifEmpty { null })
                        }
                    }
                }
            }
            item { androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp)) }
        }
    }
}

@Composable
private fun ComponentCard(
    comp: ComponentRecord,
    onBuilder: () -> Unit,
    onProvider: () -> Unit,
    onCopyAdb: () -> Unit,
    onCopyComponent: () -> Unit,
) {
    SectionCard(comp.type.label, trailing = {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (comp.exported) Pill("exported", if (comp.isUnprotectedExported) AccentRed else AccentCyan)
            else Pill("internal", AccentGreen)
            if (!comp.enabled) Pill("disabled", AccentRed)
        }
    }) {
        Text(comp.shortClassName, style = MonoSmall, color = MaterialTheme.colorScheme.onSurface)
        KeyValueRow("Permission", comp.permission)
        if (comp.type == ComponentType.PROVIDER) {
            KeyValueRow("Authority", comp.authority, copyable = true)
            KeyValueRow("Read perm", comp.readPermission)
            KeyValueRow("Write perm", comp.writePermission)
            KeyValueRow("grantUri", comp.grantUriPermissions.toString())
        }
        comp.processName?.let { KeyValueRow("Process", it) }
        if (comp.deepLinks.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                comp.deepLinks.take(8).forEach { Pill(it.exampleUri(), AccentPurple) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (comp.type == ComponentType.PROVIDER) {
                TextButton(onClick = onProvider) { Text("→ Provider Lab") }
            } else {
                TextButton(onClick = onBuilder) { Text("→ Builder") }
            }
            TextButton(onClick = onCopyAdb) { Text("adb") }
            TextButton(onClick = onCopyComponent) { Text("copy") }
        }
    }
}

private fun ComponentRecord.toIntentSpec(): IntentSpec {
    val dispatch = when (type) {
        ComponentType.ACTIVITY -> DispatchType.START_ACTIVITY
        ComponentType.SERVICE -> DispatchType.START_SERVICE
        ComponentType.RECEIVER -> DispatchType.SEND_BROADCAST
        ComponentType.PROVIDER -> DispatchType.CONTENT_QUERY
    }
    val firstDeepLink = deepLinks.firstOrNull()?.exampleUri()
    return IntentSpec(
        dispatchType = dispatch,
        targeting = TargetingMode.EXPLICIT_COMPONENT,
        componentPackage = packageName,
        componentClass = className,
        action = if (firstDeepLink != null) "android.intent.action.VIEW" else null,
        dataUri = firstDeepLink,
        categories = if (firstDeepLink != null)
            listOf("android.intent.category.BROWSABLE", "android.intent.category.DEFAULT") else emptyList(),
    )
}

private fun ComponentRecord.toProviderSpec(): IntentSpec = IntentSpec(
    dispatchType = DispatchType.CONTENT_QUERY,
    targeting = TargetingMode.IMPLICIT,
    dataUri = "content://${authority ?: packageName}/",
)
