package com.inventorymanager.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

@Composable
fun InventoryManagerApp(
    viewModel: InventoryViewModel,
    onLockApp: () -> Unit,
    onClearExportMessage: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(uiState.backupMessage) {
        uiState.backupMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearExportMessage()
        }
    }

    if (showExportPasswordDialog) {
        PasswordDialog(
            title = "Export Backup",
            message = "Set a password to encrypt your backup. You will need this password to restore your data on any device.",
            onConfirm = { password ->
                showExportPasswordDialog = false
                viewModel.exportEncryptedBackup(password)
            },
            onDismiss = { showExportPasswordDialog = false }
        )
    }

    pendingImportUri?.let { uri ->
        PasswordDialog(
            title = "Import Backup",
            message = "Enter the password used to encrypt this backup file.",
            onConfirm = { password ->
                pendingImportUri = null
                viewModel.importEncryptedBackup(uri, password)
            },
            onDismiss = { pendingImportUri = null }
        )
    }

    if (uiState.editor.isOpen) {
        InventoryEditorScreen(
            state = uiState.editor,
            locations = uiState.locations,
            allContainers = uiState.allContainers,
            currencies = uiState.currencies,
            allTags = uiState.tags,
            onClose = viewModel::closeEditor,
            onUpdate = viewModel::updateEditor,
            onAddGalleryPhotos = viewModel::addGalleryPhotos,
            onAddCameraPhoto = viewModel::addCameraPhoto,
            onRemovePhoto = viewModel::removePhoto,
            onDelete = viewModel::deleteEditorItem,
            onSave = viewModel::saveEditor,
        )
    } else {
        InventoryListScreen(
            uiState = uiState,
            onQueryChanged = viewModel::onQueryChanged,
            onImageSearch = viewModel::onImageSearch,
            onClearImageSearch = viewModel::clearImageSearch,
            onCreateItem = viewModel::startCreating,
            onEditItem = viewModel::startEditing,
            onLockApp = onLockApp,
            onExportBackup = { showExportPasswordDialog = true },
            onImportBackup = { uri -> pendingImportUri = uri },
            snackbarHostState = snackbarHostState,
        )
    }
}

@Composable
private fun PasswordDialog(
    title: String,
    message: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(message, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(image, contentDescription = null)
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password) },
                enabled = password.isNotBlank()
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryListScreen(
    uiState: InventoryUiState,
    onQueryChanged: (String) -> Unit,
    onImageSearch: (Uri) -> Unit,
    onClearImageSearch: () -> Unit,
    onCreateItem: () -> Unit,
    onEditItem: (com.inventorymanager.app.data.local.entity.InventoryItemWithPhotos) -> Unit,
    onLockApp: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: (Uri) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var pendingSearchImageUri by remember { mutableStateOf<Uri?>(null) }
    var searchMenuExpanded by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(onImportBackup)
    }

    val imageSearchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let(onImageSearch)
    }

    val cameraSearchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) {
            pendingSearchImageUri?.let(onImageSearch)
        }
        pendingSearchImageUri = null
    }

    val cameraSearchPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            pendingSearchImageUri = createCaptureUri(context)
            pendingSearchImageUri?.let(cameraSearchLauncher::launch)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Camera permission is required to search by photo")
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Inventory Manager",
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                NavigationDrawerItem(
                    label = { Text(if (uiState.isImportingBackup) "Importing..." else "Import Backup") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        importLauncher.launch(arrayOf("*/*"))
                    },
                    icon = { Icon(Icons.Outlined.Upload, contentDescription = null) },
                    badge = { if (uiState.isImportingBackup) Text("⌛") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text(if (uiState.isExportingBackup) "Exporting..." else "Export Backup") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onExportBackup()
                    },
                    icon = { Icon(Icons.Outlined.Download, contentDescription = null) },
                    badge = { if (uiState.isExportingBackup) Text("⌛") },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Inventory Manager") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Outlined.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = onLockApp) {
                            Icon(Icons.Outlined.Lock, contentDescription = "Lock app")
                        }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onCreateItem) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add item")
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            ) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = onQueryChanged,
                    singleLine = true,
                    enabled = uiState.items.isNotEmpty() || uiState.query.isNotEmpty() || uiState.isImageSearchActive,
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.isImageSearchActive) {
                            IconButton(onClick = onClearImageSearch) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear image search")
                            }
                        } else {
                            Box {
                                IconButton(onClick = { searchMenuExpanded = true }) {
                                    Icon(Icons.Outlined.PhotoCamera, contentDescription = "Search by image")
                                }
                                DropdownMenu(
                                    expanded = searchMenuExpanded,
                                    onDismissRequest = { searchMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Gallery") },
                                        onClick = {
                                            searchMenuExpanded = false
                                            imageSearchLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                        },
                                        leadingIcon = { Icon(Icons.Outlined.PhotoLibrary, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Camera") },
                                        onClick = {
                                            searchMenuExpanded = false
                                            val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                            if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                                                pendingSearchImageUri = createCaptureUri(context)
                                                pendingSearchImageUri?.let(cameraSearchLauncher::launch)
                                            } else {
                                                cameraSearchPermissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        },
                                        leadingIcon = { Icon(Icons.Outlined.PhotoCamera, contentDescription = null) }
                                    )
                                }
                            }
                        }
                    },
                    placeholder = { 
                        Text(
                            text = if (uiState.isImageSearchActive) "Image search active" else "Search by name, location, tag, or image",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (uiState.items.isEmpty()) {
                        item {
                            if (uiState.query.isEmpty()) {
                                EmptyInventoryState()
                            } else {
                                NoResultsState(query = uiState.query)
                            }
                        }
                    } else {
                        itemsIndexed(uiState.items) { index, (item, score) ->
                            InventoryCard(item = item, score = score, onClick = { onEditItem(item) })
                            if (index < uiState.items.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoResultsState(query: String) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Nothing found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "No items match \"$query\". Try a different search term.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InventoryCard(
    item: com.inventorymanager.app.data.local.entity.InventoryItemWithPhotos,
    score: Float? = null,
    onClick: () -> Unit,
) {
    val imagePath = item.item.primaryImagePath.ifBlank {
        item.photos.firstOrNull()?.filePath.orEmpty()
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                            ),
                        ),
                    ),
            ) {
                if (imagePath.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(File(imagePath))
                            .crossfade(true)
                            .build(),
                        contentDescription = item.item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            item.item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        Text(
                            item.item.uid,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    if (score != null) {
                        val percentage = (score * 100).coerceIn(0f, 100f).toInt()
                        Text(
                            text = "$percentage%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Place,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${item.item.locationName} • ${item.item.containerName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                    )
                }
                if (item.item.categoryTagsCsv.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item.item.categoryTagsCsv.split(",")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .forEach { tag ->
                                TagBadge(tag)
                            }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyInventoryState() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("No items yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Add your first photo-first asset to start building the catalog.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun InventoryEditorScreen(
    state: InventoryEditorState,
    locations: List<String>,
    allContainers: List<Pair<String, String>>,
    currencies: List<String>,
    allTags: List<String>,
    onClose: () -> Unit,
    onUpdate: ((InventoryEditorState) -> InventoryEditorState) -> Unit,
    onAddGalleryPhotos: (List<String>) -> Unit,
    onAddCameraPhoto: (String) -> Unit,
    onRemovePhoto: (Int) -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var locationExpanded by remember { mutableStateOf(false) }
    var containerExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var zoomedPhoto by remember { mutableStateOf<InventoryEditorPhoto?>(null) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd-MM-yyyy") }

    var locationTextFieldValue by remember {
        mutableStateOf(TextFieldValue(state.locationName, TextRange(state.locationName.length)))
    }
    var containerTextFieldValue by remember {
        mutableStateOf(TextFieldValue(state.containerName, TextRange(state.containerName.length)))
    }
    var currencyTextFieldValue by remember {
        mutableStateOf(TextFieldValue(state.currencyCode, TextRange(state.currencyCode.length)))
    }

    LaunchedEffect(state.locationName) {
        if (state.locationName != locationTextFieldValue.text) {
            locationTextFieldValue = locationTextFieldValue.copy(
                text = state.locationName,
                selection = TextRange(state.locationName.length)
            )
        }
    }
    LaunchedEffect(state.containerName) {
        if (state.containerName != containerTextFieldValue.text) {
            containerTextFieldValue = containerTextFieldValue.copy(
                text = state.containerName,
                selection = TextRange(state.containerName.length)
            )
        }
    }
    LaunchedEffect(state.currencyCode) {
        if (state.currencyCode != currencyTextFieldValue.text) {
            currencyTextFieldValue = currencyTextFieldValue.copy(
                text = state.currencyCode,
                selection = TextRange(state.currencyCode.length)
            )
        }
    }

    val initialDate = remember(state.purchaseDateText) {
        runCatching {
            LocalDate.parse(state.purchaseDateText, dateFormatter)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            onUpdate { it.copy(purchaseDateText = date.format(dateFormatter)) }
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10),
    ) { uris ->
        onAddGalleryPhotos(uris.map { it.toString() })
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) {
            pendingCameraUri?.let { onAddCameraPhoto(it.toString()) }
        }
        pendingCameraUri = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            pendingCameraUri = createCaptureUri(context)
            pendingCameraUri?.let(cameraLauncher::launch)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Camera permission is required to take photos")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (state.itemId == 0L) "Add Item" else "Edit Item") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.itemId != 0L) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete item")
                        }
                    }
                    TextButton(onClick = onSave, enabled = !state.isSaving && state.name.isNotBlank()) {
                        Text(if (state.isSaving) "Saving" else "Save")
                    }
                },
            )
        },
    ) { padding ->
        if (showDeleteConfirmation) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete Item") },
                text = { Text("Are you sure you want to delete this item? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmation = false
                            onDelete()
                        },
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showAddTagDialog) {
            var newTagText by remember { mutableStateOf("") }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showAddTagDialog = false },
                title = { Text("Add Tag") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = newTagText,
                            onValueChange = { newTagText = it },
                            label = { Text("New tag name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        
                        val currentTagsList = state.tagsText.split(",")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .toSet()
                        
                        val availableTags = allTags.filter { it !in currentTagsList }
                        
                        if (availableTags.isNotEmpty()) {
                            Text("Suggestions", style = MaterialTheme.typography.labelMedium)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                availableTags.forEach { tag ->
                                    AssistChip(
                                        onClick = {
                                            val current = state.tagsText.split(",")
                                                .map { it.trim() }
                                                .filter { it.isNotBlank() }
                                                .toMutableList()
                                            if (tag !in current) {
                                                current.add(tag)
                                                onUpdate { it.copy(tagsText = current.joinToString(", ")) }
                                            }
                                            showAddTagDialog = false
                                        },
                                        label = { Text(tag) }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newTagText.isNotBlank()) {
                                val current = state.tagsText.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                    .toMutableList()
                                if (newTagText.trim() !in current) {
                                    current.add(newTagText.trim())
                                    onUpdate { it.copy(tagsText = current.joinToString(", ")) }
                                }
                            }
                            showAddTagDialog = false
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddTagDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(state.photos) { index, photo ->
                    EditorPhotoPreview(
                        photo = photo,
                        onRemove = { onRemovePhoto(index) },
                        onClick = { zoomedPhoto = photo }
                    )
                }
                item {
                    PhotoActionCard(
                        label = "Gallery",
                        icon = Icons.Outlined.PhotoLibrary,
                        onClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                    )
                }
                item {
                    PhotoActionCard(
                        label = "Camera",
                        icon = Icons.Outlined.PhotoCamera,
                        onClick = {
                            val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                            if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                                pendingCameraUri = createCaptureUri(context)
                                pendingCameraUri?.let(cameraLauncher::launch)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                    )
                }
            }

            Text(
                text = "ID: ${state.uid}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            OutlinedTextField(
                value = state.name,
                onValueChange = { value -> onUpdate { current -> current.copy(name = value) } },
                label = { Text("Item name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = if (state.name != state.initialName && state.itemId != 0L) 
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                    else MaterialTheme.colorScheme.surface
                )
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = { value -> onUpdate { it.copy(description = value) } },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = if (state.description != state.initialDescription && state.itemId != 0L) 
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                    else MaterialTheme.colorScheme.surface
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = state.estimatedValueText,
                    onValueChange = { value -> onUpdate { it.copy(estimatedValueText = value) } },
                    label = { Text("Estimated value") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = if (state.estimatedValueText != state.initialEstimatedValueText && state.itemId != 0L) 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                        else MaterialTheme.colorScheme.surface
                    )
                )
                ExposedDropdownMenuBox(
                    expanded = currencyExpanded,
                    onExpandedChange = { currencyExpanded = !currencyExpanded },
                    modifier = Modifier.width(120.dp)
                ) {
                    OutlinedTextField(
                        value = currencyTextFieldValue,
                        onValueChange = { value ->
                            currencyTextFieldValue = value
                            onUpdate { it.copy(currencyCode = value.text) }
                            currencyExpanded = true
                        },
                        label = { Text("Currency") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                            unfocusedContainerColor = if (state.currencyCode != state.initialCurrencyCode && state.itemId != 0L) 
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                            else MaterialTheme.colorScheme.surface
                        ),
                    )

                    val filteredCurrencies = currencies.filter {
                        it.contains(state.currencyCode, ignoreCase = true)
                    }

                    if (filteredCurrencies.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = currencyExpanded,
                            onDismissRequest = { currencyExpanded = false },
                        ) {
                            filteredCurrencies.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        currencyTextFieldValue = TextFieldValue(selectionOption, TextRange(selectionOption.length))
                                        onUpdate { it.copy(currencyCode = selectionOption) }
                                        currencyExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                )
                            }
                        }
                    }
                }
            }
            val dateInteractionSource = remember { MutableInteractionSource() }
            val isDatePressed by dateInteractionSource.collectIsPressedAsState()
            if (isDatePressed) {
                showDatePicker = true
            }

            OutlinedTextField(
                value = state.purchaseDateText,
                onValueChange = { },
                label = { Text("Purchase date") },
                readOnly = true,
                interactionSource = dateInteractionSource,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Icon(Icons.Outlined.DateRange, contentDescription = "Select date")
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = if (state.purchaseDateText != state.initialPurchaseDateText && state.itemId != 0L) 
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                    else MaterialTheme.colorScheme.surface
                )
            )

            ExposedDropdownMenuBox(
                expanded = locationExpanded,
                onExpandedChange = { locationExpanded = !locationExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = locationTextFieldValue,
                    onValueChange = { value ->
                        locationTextFieldValue = value
                        onUpdate { it.copy(locationName = value.text) }
                        locationExpanded = true
                    },
                    label = { Text("Location") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                        unfocusedContainerColor = if (state.locationName != state.initialLocationName && state.itemId != 0L) 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                        else MaterialTheme.colorScheme.surface
                    ),
                )

                val filteredLocations = locations.filter {
                    it.contains(state.locationName, ignoreCase = true)
                }

                if (filteredLocations.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = locationExpanded,
                        onDismissRequest = { locationExpanded = false },
                    ) {
                        filteredLocations.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    locationTextFieldValue = TextFieldValue(selectionOption, TextRange(selectionOption.length))
                                    onUpdate { it.copy(locationName = selectionOption) }
                                    locationExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = containerExpanded,
                onExpandedChange = { containerExpanded = !containerExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = containerTextFieldValue,
                    onValueChange = { value ->
                        containerTextFieldValue = value
                        onUpdate { it.copy(containerName = value.text) }
                        containerExpanded = true
                    },
                    label = { Text("Container") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = containerExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                        unfocusedContainerColor = if (state.containerName != state.initialContainerName && state.itemId != 0L) 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                        else MaterialTheme.colorScheme.surface
                    ),
                )

                val filteredContainers = allContainers
                    .filter { (loc, cont) ->
                        loc.equals(state.locationName, ignoreCase = true) &&
                        cont.contains(state.containerName, ignoreCase = true)
                    }
                    .map { it.second }
                    .distinct()

                if (filteredContainers.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = containerExpanded,
                        onDismissRequest = { containerExpanded = false },
                    ) {
                        filteredContainers.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    containerTextFieldValue = TextFieldValue(selectionOption, TextRange(selectionOption.length))
                                    onUpdate { it.copy(containerName = selectionOption) }
                                    containerExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tags", style = MaterialTheme.typography.titleSmall)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { showAddTagDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = "Add Tag",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.tagsText.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .forEach { tag ->
                        TagBadge(
                            tag = tag,
                            onRemove = {
                                val current = state.tagsText.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                    .toMutableList()
                                current.remove(tag)
                                onUpdate { it.copy(tagsText = current.joinToString(", ")) }
                            }
                        )
                    }
            }
        }
    }

    if (zoomedPhoto != null) {
        Dialog(
            onDismissRequest = { zoomedPhoto = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black)
                    .clickable { zoomedPhoto = null },
                contentAlignment = Alignment.Center
            ) {
                val requestData = when (val photo = zoomedPhoto!!) {
                    is InventoryEditorPhoto.Existing -> File(photo.path)
                    is InventoryEditorPhoto.Pending -> photo.uri.toUri()
                }
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(requestData)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { zoomedPhoto = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun TagBadge(tag: String, onRemove: (() -> Unit)? = null) {
    val color = remember(tag) {
        val hash = tag.lowercase().trim().hashCode()
        androidx.compose.ui.graphics.Color.hsl(
            hue = (hash.absoluteValue % 360).toFloat(),
            saturation = 0.5f,
            lightness = 0.6f
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = if (onRemove != null) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = tag.trim(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
        )
        if (onRemove != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Remove tag",
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onRemove() },
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun EditorPhotoPreview(
    photo: InventoryEditorPhoto,
    onRemove: () -> Unit,
    onClick: () -> Unit,
) {
    val requestData = when (photo) {
        is InventoryEditorPhoto.Existing -> File(photo.path)
        is InventoryEditorPhoto.Pending -> photo.uri.toUri()
    }

    Box {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .height(120.dp)
                .aspectRatio(1f)
                .clickable(onClick = onClick),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(requestData)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "Remove")
        }
    }
}

@Composable
private fun PhotoActionCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .height(120.dp)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = label)
            Spacer(modifier = Modifier.height(8.dp))
            Text(label)
        }
    }
}

private fun createCaptureUri(context: Context): Uri {
    val file = File.createTempFile("capture_", ".jpg", context.cacheDir)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
}
