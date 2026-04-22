package com.scaevo.ui.permissions

import androidx.lifecycle.ViewModel
import com.scaevo.data.usage.UsageStatsHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    val helper: UsageStatsHelper
) : ViewModel()
