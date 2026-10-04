package com.example.callscribe.recording

import com.example.callscribe.data.model.RecordRule

class RecordingRuleEvaluator(
    private val isKnownContactProvider: (String) -> Boolean
) {
    fun shouldRecord(
        rule: RecordRule,
        phoneNumber: String,
        selectedContacts: Set<String>
    ): Boolean {
        return when (rule) {
            RecordRule.ALL -> true

            RecordRule.UNKNOWN_ONLY -> {
                val isKnown = isKnownContactProvider(phoneNumber)
                !isKnown
            }

            RecordRule.SELECTED_CONTACTS -> {
                val cleanNumber = normalizeNumber(phoneNumber)
                selectedContacts.any { normalizeNumber(it) == cleanNumber }
            }

            RecordRule.MANUAL -> false
        }
    }

    private fun normalizeNumber(number: String): String {
        return number.replace(Regex("[^0-9]"), "")
    }
}
