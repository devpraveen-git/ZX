package com.example.callscribe.recording

import com.example.callscribe.data.model.RecordRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingRuleEvaluatorTest {

    private val knownContacts = setOf("+1112223333", "+19998887777")
    private val evaluator = RecordingRuleEvaluator { number ->
        knownContacts.contains(number)
    }

    @Test
    fun ruleAll_recordsAnyNumber() {
        assertTrue(evaluator.shouldRecord(RecordRule.ALL, "+1112223333", emptySet()))
        assertTrue(evaluator.shouldRecord(RecordRule.ALL, "+1000000000", emptySet()))
    }

    @Test
    fun ruleUnknownOnly_recordsOnlyUnknown() {
        // Known contact -> should NOT record
        assertFalse(evaluator.shouldRecord(RecordRule.UNKNOWN_ONLY, "+1112223333", emptySet()))

        // Unknown contact -> SHOULD record
        assertTrue(evaluator.shouldRecord(RecordRule.UNKNOWN_ONLY, "+1000000000", emptySet()))
    }

    @Test
    fun ruleSelectedContacts_recordsOnlyMatching() {
        val selected = setOf("+1112223333")

        // Matches selected -> SHOULD record
        assertTrue(evaluator.shouldRecord(RecordRule.SELECTED_CONTACTS, "+1112223333", selected))
        // Format variation check (dashes/spaces)
        assertTrue(evaluator.shouldRecord(RecordRule.SELECTED_CONTACTS, "+1 (112) 223-333", selected))

        // Not in selected -> should NOT record
        assertFalse(evaluator.shouldRecord(RecordRule.SELECTED_CONTACTS, "+19998887777", selected))
    }

    @Test
    fun ruleManual_neverAutoRecords() {
        assertFalse(evaluator.shouldRecord(RecordRule.MANUAL, "+1112223333", emptySet()))
        assertFalse(evaluator.shouldRecord(RecordRule.MANUAL, "+1000000000", emptySet()))
    }
}
