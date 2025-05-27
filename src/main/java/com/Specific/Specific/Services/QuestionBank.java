package com.Specific.Specific.Services;

import com.Specific.Specific.Models.Question;

import java.util.ArrayList;
import java.util.List;

public class QuestionBank {
    public static List<Question> getQuestions() {
        List<Question> questions = new ArrayList<>();
        questions.add(new Question(
                "What does the word 'resilient' most nearly mean?",
                2,
                new String[]{"Angry", "Fragile", "Strong after difficulty", "Lazy"}
        ));

        questions.add(new Question(
                "Choose the word closest in meaning to 'meticulous'.",
                0,
                new String[]{"Careful", "Careless", "Quick", "Loud"}
        ));

        questions.add(new Question(
                "What is the best synonym for 'abundant'?",
                1,
                new String[]{"Tiny", "Plentiful", "Rare", "Insufficient"}
        ));

        questions.add(new Question(
                "The word 'reluctant' most nearly means:",
                3,
                new String[]{"Happy", "Sure", "Fast", "Unwilling"}
        ));

        questions.add(new Question(
                "What is the closest meaning of 'inevitable'?",
                2,
                new String[]{"Avoidable", "Confusing", "Certain to happen", "Unfair"}
        ));

        questions.add(new Question(
                "What does 'evaluate' mean?",
                1,
                new String[]{"Ignore", "Judge or assess", "Compare", "Change"}
        ));

        questions.add(new Question(
                "Choose the correct meaning of 'sufficient'.",
                0,
                new String[]{"Enough", "Extra", "Low", "Complex"}
        ));

        questions.add(new Question(
                "The word 'controversial' means:",
                3,
                new String[]{"Normal", "Funny", "Safe", "Causing disagreement"}
        ));

        questions.add(new Question(
                "What is the meaning of 'dedicated'?",
                1,
                new String[]{"Uncertain", "Devoted or committed", "Tired", "Late"}
        ));

        questions.add(new Question(
                "The word 'enhance' most nearly means:",
                0,
                new String[]{"Improve", "Destroy", "Hide", "Avoid"}
        ));


        return questions;
    }
}
