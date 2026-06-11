package com.lautarovculic.intentions.ui.screens.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lautarovculic.intentions.core.model.InstalledAppRecord
import com.lautarovculic.intentions.ui.components.Pill
import com.lautarovculic.intentions.ui.rememberAppContainer
import com.lautarovculic.intentions.ui.theme.AccentCyan
import com.lautarovculic.intentions.ui.theme.AccentPurple
import com.lautarovculic.intentions.ui.theme.AccentRed
import com.lautarovculic.intentions.ui.theme.AccentYellow
import com.lautarovculic.intentions.ui.theme.MonoSmall

@Composable
fun AppsScreen(onOpenApp: (String) -> Unit) {
    val container = rememberAppContainer()
    val vm: AppsViewModel = viewModel(factory = AppsViewModel.factory(container))
    val ui by vm.ui.collectAsStateWithLifecycle()
    val f = ui.filter

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = f.query,
            onValueChange = { q -> vm.updateFilter { it.copy(query = q) } },
            label = { Text("Filter by label or package") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        )
        FlowRow(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(f.includeSystem, { vm.updateFilter { it.copy(includeSystem = !it.includeSystem) } }, label = { Text("System") })
            FilterChip(f.exportedOnly, { vm.updateFilter { it.copy(exportedOnly = !it.exportedOnly) } }, label = { Text("Exported") })
            FilterChip(f.deepLinksOnly, { vm.updateFilter { it.copy(deepLinksOnly = !it.deepLinksOnly) } }, label = { Text("Deep links") })
            FilterChip(f.debuggableOnly, { vm.updateFilter { it.copy(debuggableOnly = !it.debuggableOnly) } }, label = { Text("Debuggable") })
        }
        Text(
            "${ui.visible.size} / ${ui.all.size} packages",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
        if (ui.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(Modifier.fillMaxSize()) {
            items(ui.visible, key = { it.packageName }) { app ->
                AppRow(app, onClick = { onOpenApp(app.packageName) })
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun AppRow(app: InstalledAppRecord, onClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(app.label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (app.isDebuggable) Pill("debug", AccentRed, Modifier.padding(start = 4.dp))
                if (app.isSystem) Pill("system", AccentYellow, Modifier.padding(start = 4.dp))
            }
            Text(app.packageName, style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("uid ${app.uid}", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("tSDK ${app.targetSdk}", style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (app.totalExportedComponents > 0) Pill("${app.totalExportedComponents} exported", AccentCyan)
                if (app.hasAppLinks) Pill("app links", AccentPurple)
            }
        }
    }
}
