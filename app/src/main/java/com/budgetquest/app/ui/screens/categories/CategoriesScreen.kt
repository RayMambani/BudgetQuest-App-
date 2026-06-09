package com.budgetquest.app.ui.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.budgetquest.app.data.local.dao.CategorySpending
import com.budgetquest.app.data.local.entity.CategoryEntity
import com.budgetquest.app.ui.theme.*
import com.budgetquest.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val spending by viewModel.categorySpending.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        containerColor = CyberBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("CATEGORIES ARCHIVE", color = CyberGold, fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = CyberBackground)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp, top = 16.dp)
        ) {
            items(categories) { category ->
                val categorySpent = spending.find { it.categoryId == category.id }?.total ?: 0.0
                CategoryCard(
                    category = category, 
                    spent = categorySpent,
                    onEdit = { categoryToEdit = category },
                    onDelete = { categoryToDelete = category }
                )
            }

            item {
                Button(
                    onClick = {
                        showAddDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NeonCyan.copy(alpha = 0.5f)))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ADD NEW CATEGORY", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showAddDialog) {
        CategoryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, limit, icon ->
                viewModel.addCategory(name, limit, icon)
                showAddDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Category ARCHIVED: $name")
                }
            }
        )
    }

    if (categoryToEdit != null) {
        CategoryDialog(
            initialCategory = categoryToEdit,
            onDismiss = { categoryToEdit = null },
            onConfirm = { name, limit, icon ->
                viewModel.updateCategory(categoryToEdit!!.copy(name = name, monthlyLimit = limit, icon = icon))
                categoryToEdit = null
                scope.launch {
                    snackbarHostState.showSnackbar("Category UPDATED: $name")
                }
            }
        )
    }

    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            containerColor = CyberSurface,
            title = { Text("DELETION PROTOCOL", color = ThresholdMax, fontWeight = FontWeight.Black) },
            text = { Text("Are you sure you want to delete ${categoryToDelete?.name}? This cannot be undone.", color = Color.White) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategory(categoryToDelete!!)
                        categoryToDelete = null
                        scope.launch {
                            snackbarHostState.showSnackbar("Category DELETED")
                        }
                    }
                ) {
                    Text("DELETE", color = ThresholdMax, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("CANCEL", color = Color.White.copy(alpha = 0.6f))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDialog(
    initialCategory: CategoryEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String) -> Unit
) {
    var name by remember { mutableStateOf(initialCategory?.name ?: "") }
    var limit by remember { mutableStateOf(initialCategory?.monthlyLimit?.toString() ?: "") }
    var selectedIcon by remember { mutableStateOf(initialCategory?.icon ?: "shopping_cart") }
    
    val isEditing = initialCategory != null
    
    val icons = listOf(
        "shopping_cart", "directions_car", "movie", "receipt_long", "account_balance_wallet", "category"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = { Text(if (isEditing) "EDIT CATEGORY DATA" else "NEW CATEGORY PROTOCOL", color = CyberGold, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name", color = Color.White.copy(alpha = 0.6f)) },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = limit,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) limit = it },
                    label = { Text("Monthly Limit (R)", color = Color.White.copy(alpha = 0.6f)) },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Select Icon Identifier", color = CyberGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    icons.forEach { iconName ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (selectedIcon == iconName) NeonCyan.copy(alpha = 0.2f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selectedIcon == iconName) NeonCyan else Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedIcon = iconName },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(iconName),
                                contentDescription = null,
                                tint = if (selectedIcon == iconName) NeonCyan else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    if (name.isNotBlank() && limit.toDoubleOrNull() != null) {
                        onConfirm(name, limit.toDouble(), selectedIcon)
                    }
                },
                enabled = name.isNotBlank() && limit.toDoubleOrNull() != null
            ) {
                Text(if (isEditing) "UPDATE" else "ADD", color = NeonCyan, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.White.copy(alpha = 0.6f))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun CategoryCard(
    category: CategoryEntity, 
    spent: Double,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = (spent / category.monthlyLimit).toFloat().coerceIn(0f, 1f)
    val percentage = (progress * 100).toInt()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(CyberBackground, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(category.icon),
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = category.name.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
                
                Text(
                    text = "R ${String.format("%,.0f", spent)}",
                    color = CyberGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = NeonCyan.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ThresholdMax.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                color = when {
                    progress > 0.9f -> ThresholdMax
                    progress > 0.7f -> NeonPurple
                    else -> NeonGreen
                },
                trackColor = Color.White.copy(alpha = 0.05f),
                strokeCap = StrokeCap.Round
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Limit: R ${String.format("%,.0f", category.monthlyLimit)}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "$percentage%",
                    color = if (progress > 0.9f) ThresholdMax else NeonCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

fun getCategoryIcon(iconName: String): ImageVector {
    return when (iconName) {
        "shopping_cart" -> Icons.Default.ShoppingCart
        "directions_car" -> Icons.Default.DirectionsCar
        "movie" -> Icons.Default.Movie
        "receipt_long" -> Icons.AutoMirrored.Filled.ReceiptLong
        "account_balance_wallet" -> Icons.Default.AccountBalanceWallet
        else -> Icons.Default.Category
    }
}
