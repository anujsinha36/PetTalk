package com.example.pettalk.presentation.viewmodels

import androidx.lifecycle.ViewModel
import com.example.pettalk.data.PetType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Holds the pet picked on the selection screen. */
@HiltViewModel
class PetSelectionViewModel @Inject constructor() : ViewModel() {

    private val _selectedPet = MutableStateFlow(PetType.CAT)
    val selectedPet: StateFlow<PetType> = _selectedPet.asStateFlow()

    fun selectPet(petType: PetType) {
        _selectedPet.update { petType }
    }
}
