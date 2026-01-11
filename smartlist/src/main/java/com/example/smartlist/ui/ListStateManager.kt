package com.example.smartlist.ui

import androidx.annotation.VisibleForTesting
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.smartlist.R

/**
 * Small helper that centralizes permitted state transitions and metadata for cloned lists.
 * Provides display metadata (label resource, description resource, color, icon) so the UI
 * can render consistent pills and legends.
 */
object ListStateManager {
    const val PRECHECK = "PRECHECK"
    const val WORKING = "WORKING"
    const val CLOSED = "CLOSED"
    const val ARCHIVED = "ARCHIVED"

    data class StateInfo(
        val id: String,
        val labelRes: Int,
        val descRes: Int,
        val color: Color,
        val icon: ImageVector
    )

    private val infoMap: Map<String, StateInfo> = mapOf(
        PRECHECK to StateInfo(PRECHECK, R.string.state_precheck, R.string.legend_precheck_desc, Color(0xFF90A4AE), Icons.Filled.HourglassEmpty),
        WORKING to StateInfo(WORKING, R.string.state_working, R.string.legend_working_desc, Color(0xFF4CAF50), Icons.Filled.Work),
        CLOSED to StateInfo(CLOSED, R.string.state_closed, R.string.legend_closed_desc, Color(0xFFFFA726), Icons.Filled.CheckCircle),
        ARCHIVED to StateInfo(ARCHIVED, R.string.state_archived, R.string.legend_archived_desc, Color(0xFF616161), Icons.Filled.Archive)
    )

    // Allowed transitions map (from -> list of allowed targets)
    private val transitions = mapOf(
        PRECHECK to listOf(WORKING, ARCHIVED),
        WORKING to listOf(CLOSED, ARCHIVED),
        CLOSED to listOf(WORKING, ARCHIVED),
        ARCHIVED to listOf(PRECHECK)
    )

    fun allowedTargets(current: String): List<String> = transitions[current] ?: emptyList()

    fun isTransitionAllowed(current: String, target: String): Boolean {
        return allowedTargets(current).contains(target)
    }

    fun requiresConfirmation(current: String, target: String): Boolean {
        // Archiving is destructive/hidden — require confirmation
        return target == ARCHIVED
    }

    fun defaultUnarchiveTarget(): String = PRECHECK

    fun isEditable(state: String): Boolean {
        return when (state) {
            PRECHECK -> true // add/rename/delete allowed; marking not allowed
            WORKING -> true // add/rename/mark allowed; delete not allowed
            CLOSED -> false
            ARCHIVED -> false
            else -> true
        }
    }

    fun getStateInfo(state: String): StateInfo {
        return infoMap[state] ?: infoMap[PRECHECK]!!
    }

    @VisibleForTesting
    fun allStateInfos(): List<StateInfo> = infoMap.values.toList()
}
