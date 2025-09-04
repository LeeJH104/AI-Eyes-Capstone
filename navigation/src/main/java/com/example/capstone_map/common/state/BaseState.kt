package com.example.capstone_map.common.state

interface BaseState<VM> {
    fun handle(viewModel: VM) {}
    fun onPrimaryInput(viewModel: VM) {}
    fun onSecondaryInput(viewModel: VM) {}
    fun onTertiaryInput(viewModel: VM) {}
}
