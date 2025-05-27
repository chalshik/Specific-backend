package com.Specific.Specific.Services;

import com.Specific.Specific.Models.Question;

import java.util.ArrayList;
import java.util.List;

public class QuestionBank {
    public static List<Question> getQuestions() {
        List<Question> questions = new ArrayList<>();

        questions.add(new Question(
                "What is the capital of France?",
                0,
                new String[]{"Paris", "London", "Berlin", "Madrid"}
        ));

        questions.add(new Question(
                "Which planet is known as the Red Planet?",
                2,
                new String[]{"Earth", "Venus", "Mars", "Jupiter"}
        ));

        questions.add(new Question(
                "What is the largest ocean on Earth?",
                3,
                new String[]{"Atlantic Ocean", "Indian Ocean", "Arctic Ocean", "Pacific Ocean"}
        ));

        questions.add(new Question(
                "Who wrote 'Romeo and Juliet'?",
                0,
                new String[]{"William Shakespeare", "Mark Twain", "Charles Dickens", "Jane Austen"}
        ));

        questions.add(new Question(
                "What is the boiling point of water at sea level?",
                1,
                new String[]{"90°C", "100°C", "110°C", "120°C"}
        ));

        questions.add(new Question(
                "Which element has the chemical symbol 'O'?",
                1,
                new String[]{"Gold", "Oxygen", "Hydrogen", "Carbon"}
        ));

        questions.add(new Question(
                "What is the smallest prime number?",
                1,
                new String[]{"1", "2", "3", "0"}
        ));

        questions.add(new Question(
                "Who painted the Mona Lisa?",
                2,
                new String[]{"Vincent van Gogh", "Pablo Picasso", "Leonardo da Vinci", "Claude Monet"}
        ));

        questions.add(new Question(
                "Which country hosted the 2016 Summer Olympics?",
                1,
                new String[]{"China", "Brazil", "UK", "Russia"}
        ));

        questions.add(new Question(
                "What gas do plants absorb from the atmosphere?",
                2,
                new String[]{"Oxygen", "Nitrogen", "Carbon Dioxide", "Helium"}
        ));

        questions.add(new Question(
                "What is the hardest natural substance on Earth?",
                2,
                new String[]{"Gold", "Iron", "Diamond", "Silver"}
        ));

        questions.add(new Question(
                "What is the currency of Japan?",
                2,
                new String[]{"Dollar", "Euro", "Yen", "Won"}
        ));

        questions.add(new Question(
                "How many continents are there on Earth?",
                2,
                new String[]{"5", "6", "7", "8"}
        ));

        questions.add(new Question(
                "Who discovered penicillin?",
                1,
                new String[]{"Marie Curie", "Alexander Fleming", "Isaac Newton", "Albert Einstein"}
        ));

        questions.add(new Question(
                "What is the main ingredient in sushi?",
                0,
                new String[]{"Rice", "Potato", "Bread", "Noodles"}
        ));

        questions.add(new Question(
                "Which planet is closest to the Sun?",
                2,
                new String[]{"Venus", "Earth", "Mercury", "Mars"}
        ));

        questions.add(new Question(
                "What language is primarily spoken in Brazil?",
                1,
                new String[]{"Spanish", "Portuguese", "French", "English"}
        ));

        questions.add(new Question(
                "In which year did World War II end?",
                1,
                new String[]{"1939", "1945", "1950", "1960"}
        ));

        questions.add(new Question(
                "Which organ pumps blood throughout the human body?",
                2,
                new String[]{"Liver", "Lungs", "Heart", "Kidneys"}
        ));

        questions.add(new Question(
                "What is the chemical formula for water?",
                1,
                new String[]{"CO2", "H2O", "O2", "NaCl"}
        ));

        return questions;
    }
}
