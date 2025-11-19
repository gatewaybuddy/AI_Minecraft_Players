package com.aiplayer.communication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for IntentClassifier.
 *
 * Tests cover:
 * - Intent classification (TASK_REQUEST, STATUS_QUERY, QUESTION, CASUAL_CHAT)
 * - Task request extraction with different action types
 * - Item normalization
 * - Edge cases and null handling
 */
class IntentClassifierTest {

    private IntentClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new IntentClassifier();
    }

    @Test
    void testTaskRequestIntentClassification() {
        // Test various task request patterns
        Intent gather = classifier.classify("gather 64 oak logs");
        assertEquals(Intent.Type.TASK_REQUEST, gather.getType(),
            "Should classify 'gather' as task request");

        Intent mine = classifier.classify("mine some diamonds");
        assertEquals(Intent.Type.TASK_REQUEST, mine.getType(),
            "Should classify 'mine' as task request");

        Intent build = classifier.classify("build a house");
        assertEquals(Intent.Type.TASK_REQUEST, build.getType(),
            "Should classify 'build' as task request");

        Intent find = classifier.classify("find some food");
        assertEquals(Intent.Type.TASK_REQUEST, find.getType(),
            "Should classify 'find' as task request");

        Intent collect = classifier.classify("collect wood please");
        assertEquals(Intent.Type.TASK_REQUEST, collect.getType(),
            "Should classify 'collect' as task request");
    }

    @Test
    void testStatusQueryIntentClassification() {
        // Test status query patterns
        Intent status1 = classifier.classify("what are you doing?");
        assertEquals(Intent.Type.STATUS_QUERY, status1.getType(),
            "Should classify 'what are you doing' as status query");

        Intent status2 = classifier.classify("how's it going?");
        assertEquals(Intent.Type.STATUS_QUERY, status2.getType(),
            "Should classify 'how's it going' as status query");

        Intent status3 = classifier.classify("what's your status?");
        assertEquals(Intent.Type.STATUS_QUERY, status3.getType(),
            "Should classify 'what's your status' as status query");
    }

    @Test
    void testQuestionIntentClassification() {
        // Test question patterns
        Intent question1 = classifier.classify("do you have diamonds?");
        assertEquals(Intent.Type.QUESTION, question1.getType(),
            "Should classify 'do you have' as question");

        Intent question2 = classifier.classify("can you help me?");
        assertEquals(Intent.Type.QUESTION, question2.getType(),
            "Should classify 'can you' as question");

        Intent question3 = classifier.classify("where is the village?");
        assertEquals(Intent.Type.QUESTION, question3.getType(),
            "Should classify 'where is' as question");

        Intent question4 = classifier.classify("how do I craft a pickaxe?");
        assertEquals(Intent.Type.QUESTION, question4.getType(),
            "Should classify 'how do I' as question");
    }

    @Test
    void testCasualChatIntentClassification() {
        // Test casual chat patterns
        Intent chat1 = classifier.classify("hello there");
        assertEquals(Intent.Type.CASUAL_CHAT, chat1.getType(),
            "Should classify 'hello' as casual chat");

        Intent chat2 = classifier.classify("thanks for your help");
        assertEquals(Intent.Type.CASUAL_CHAT, chat2.getType(),
            "Should classify 'thanks' as casual chat");

        Intent chat3 = classifier.classify("good job!");
        assertEquals(Intent.Type.CASUAL_CHAT, chat3.getType(),
            "Should classify 'good job' as casual chat");

        Intent chat4 = classifier.classify("nice weather today");
        assertEquals(Intent.Type.CASUAL_CHAT, chat4.getType(),
            "Should classify random statement as casual chat");
    }

    @Test
    void testTaskRequestExtraction() {
        // Test extracting task details from gather request
        Intent gatherIntent = classifier.classify("gather 64 oak logs");
        TaskRequest gatherTask = classifier.extractTaskRequest("gather 64 oak logs", gatherIntent);

        assertNotNull(gatherTask, "Should extract task request");
        assertEquals(TaskRequest.ActionType.GATHER, gatherTask.getActionType(),
            "Should identify GATHER action");
        assertEquals(64, gatherTask.getQuantity(), "Should extract quantity 64");
        assertNotNull(gatherTask.getTargetItem(), "Should have target item");
        assertTrue(gatherTask.getTargetItem().contains("oak"),
            "Target item should contain 'oak'");

        // Test extracting from mine request
        Intent mineIntent = classifier.classify("mine 10 iron ore");
        TaskRequest mineTask = classifier.extractTaskRequest("mine 10 iron ore", mineIntent);

        assertNotNull(mineTask, "Should extract mine request");
        assertEquals(TaskRequest.ActionType.MINE, mineTask.getActionType(),
            "Should identify MINE action");
        assertEquals(10, mineTask.getQuantity(), "Should extract quantity 10");
    }

    @Test
    void testTaskRequestWithoutQuantity() {
        // Test task without explicit quantity
        Intent intent = classifier.classify("gather some wood");
        TaskRequest task = classifier.extractTaskRequest("gather some wood", intent);

        assertNotNull(task, "Should extract task even without quantity");
        assertEquals(TaskRequest.ActionType.GATHER, task.getActionType(),
            "Should identify GATHER action");
        assertTrue(task.getQuantity() > 0, "Should have default quantity");
    }

    @Test
    void testItemNormalization() {
        // Test that "logs" gets normalized to "oak_log" or similar
        Intent intent = classifier.classify("gather logs");
        TaskRequest task = classifier.extractTaskRequest("gather logs", intent);

        assertNotNull(task, "Should extract task");
        assertNotNull(task.getTargetItem(), "Should have normalized item");
        // The actual normalization depends on implementation
        // Just verify we get some item name back
        assertFalse(task.getTargetItem().isEmpty(), "Item name should not be empty");
    }

    @Test
    void testNullAndEmptyInput() {
        // Test null message
        Intent nullIntent = classifier.classify(null);
        assertNotNull(nullIntent, "Should return intent for null message");
        assertEquals(Intent.Type.CASUAL_CHAT, nullIntent.getType(),
            "Null message should default to casual chat");

        // Test empty message
        Intent emptyIntent = classifier.classify("");
        assertNotNull(emptyIntent, "Should return intent for empty message");
        assertEquals(Intent.Type.CASUAL_CHAT, emptyIntent.getType(),
            "Empty message should default to casual chat");

        // Test whitespace only
        Intent whitespaceIntent = classifier.classify("   ");
        assertNotNull(whitespaceIntent, "Should return intent for whitespace");
        assertEquals(Intent.Type.CASUAL_CHAT, whitespaceIntent.getType(),
            "Whitespace message should default to casual chat");
    }

    @Test
    void testCaseInsensitivity() {
        // Test that classification works regardless of case
        Intent upper = classifier.classify("GATHER WOOD");
        Intent lower = classifier.classify("gather wood");
        Intent mixed = classifier.classify("GaTh

Er WoOd");

        assertEquals(Intent.Type.TASK_REQUEST, upper.getType(),
            "Uppercase should be classified as task request");
        assertEquals(Intent.Type.TASK_REQUEST, lower.getType(),
            "Lowercase should be classified as task request");
        assertEquals(Intent.Type.TASK_REQUEST, mixed.getType(),
            "Mixed case should be classified as task request");
    }

    @Test
    void testComplexTaskRequests() {
        // Test more complex task formulations
        Intent polite = classifier.classify("could you please gather 32 oak planks for me?");
        assertEquals(Intent.Type.TASK_REQUEST, polite.getType(),
            "Should handle polite task requests");

        TaskRequest politeTask = classifier.extractTaskRequest(
            "could you please gather 32 oak planks for me?", polite);
        assertNotNull(politeTask, "Should extract from polite request");
        assertEquals(32, politeTask.getQuantity(), "Should extract quantity from polite request");

        Intent imperative = classifier.classify("go mine diamonds now!");
        assertEquals(Intent.Type.TASK_REQUEST, imperative.getType(),
            "Should handle imperative task requests");
    }

    @Test
    void testAmbiguousMessages() {
        // Test messages that could be multiple types
        // The classifier should make a reasonable choice

        Intent question = classifier.classify("can you gather wood?");
        // Could be either QUESTION or TASK_REQUEST
        // Either is reasonable, but task request is more useful
        assertTrue(question.getType() == Intent.Type.TASK_REQUEST ||
                  question.getType() == Intent.Type.QUESTION,
            "Ambiguous message should be classified as either task or question");
    }
}
