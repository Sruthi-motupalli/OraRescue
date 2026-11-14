package com.ora.rescue

import org.junit.Assert.assertEquals
import org.junit.Test

class QuizScoringTest {
    @Test fun awardsStarsAtDocumentedMasteryThresholds() {
        assertEquals(3, QuizScoring.stars(9, 10))
        assertEquals(2, QuizScoring.stars(7, 10))
        assertEquals(1, QuizScoring.stars(5, 10))
        assertEquals(0, QuizScoring.stars(4, 10))
        assertEquals(0, QuizScoring.stars(0, 0))
    }

    @Test fun machineLearningTemplateProducesOrderedChaptersAndReviewedQuestions() {
        val chapters = CourseTemplates.chaptersFor("MACHINE-LEARNING.zip")
        assertEquals(10, chapters.size)
        assertEquals("Machine Learning Foundations", chapters.first())
        val questions = CourseTemplates.questionsFor("Classification")
        assertEquals(4, questions.size)
        questions.forEach { question ->
            assertEquals(4, question.options.size)
            assert(question.correctIndex in question.options.indices)
            assert(question.explanation.isNotBlank())
        }
    }
}
