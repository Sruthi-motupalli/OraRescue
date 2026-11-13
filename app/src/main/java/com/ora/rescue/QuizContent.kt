package com.ora.rescue

data class QuestionSeed(
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

object CourseTemplates {
    val machineLearningChapters = listOf(
        "Machine Learning Foundations",
        "Types of Machine Learning",
        "Python for Machine Learning",
        "ML Pipeline",
        "Data Preprocessing",
        "Feature Engineering",
        "Linear Regression",
        "Classification",
        "Evaluation Metrics",
        "Artificial Neural Networks"
    )

    fun chaptersFor(fileName: String): List<String> {
        val normalized = fileName.lowercase()
        return if (normalized.contains("machine") || normalized.contains("learning") || normalized.contains("prg4206")) {
            machineLearningChapters
        } else {
            listOf(fileName.substringBeforeLast('.').ifBlank { "Imported chapter" })
        }
    }

    fun questionsFor(chapter: String): List<QuestionSeed> = when (chapter) {
        "Machine Learning Foundations" -> listOf(
            q("What best describes machine learning?", "Rules typed for every case", "Learning patterns from data", "Only storing data", "Drawing charts", 1, "Machine learning uses data to learn patterns that support predictions or decisions."),
            q("Which is an everyday machine-learning application?", "A fixed light switch", "Email spam filtering", "A paper notebook", "A basic calculator", 1, "Spam filters learn patterns that distinguish unwanted messages."),
            q("In a prediction problem, a feature is…", "An input used by the model", "The final exam date", "Always the answer", "A database password", 0, "Features are measurable inputs used to make a prediction."),
            q("Why do we evaluate a model on unseen data?", "To increase file size", "To test generalisation", "To remove every feature", "To avoid training", 1, "Unseen data shows whether the model can generalise beyond its training examples.")
        )
        "Types of Machine Learning" -> listOf(
            q("Predicting customer churn from labelled examples is…", "Supervised classification", "Unsupervised clustering", "Reinforcement learning", "Data cleaning", 0, "The outcome is labelled and the result is a category."),
            q("Grouping customers without known labels is…", "Regression", "Clustering", "Classification", "Backpropagation", 1, "Clustering is an unsupervised method that discovers groups."),
            q("Predicting a continuous house price is usually…", "Classification", "Regression", "Clustering", "Association only", 1, "Regression predicts numeric continuous values."),
            q("A model learning through rewards and penalties uses…", "Reinforcement learning", "Linear regression", "Static rules", "Data visualisation", 0, "Reinforcement learning improves behaviour using feedback signals.")
        )
        "Data Preprocessing" -> listOf(
            q("What should be investigated before filling missing values?", "Why values are missing", "The chart colour", "The file icon", "The model logo", 0, "The cause and pattern of missingness influence the correct treatment."),
            q("Standardisation commonly transforms a feature to…", "Mean 0 and standard deviation 1", "Only positive values", "A text label", "Exactly two rows", 0, "Standardisation centres a feature and scales it by its standard deviation."),
            q("One-hot encoding is used mainly for…", "Categorical variables", "Deleting duplicates", "Video compression", "Splitting dates", 0, "One-hot encoding represents categories using indicator columns."),
            q("Why fit a scaler only on training data?", "To prevent data leakage", "To create more labels", "To remove the target", "To reduce screen brightness", 0, "Using test information during preprocessing would leak knowledge into training.")
        )
        "Linear Regression" -> listOf(
            q("In advertising spend → sales, the dependent variable is…", "Advertising spend", "Sales", "The row number", "The company name", 1, "Sales is the outcome the model is trying to predict."),
            q("Simple linear regression uses…", "One predictor", "No outcome", "Only categories", "A confusion matrix", 0, "Simple linear regression models an outcome from one independent variable."),
            q("What does the slope represent?", "Expected change in Y for a one-unit change in X", "Number of rows", "The model filename", "Always the accuracy", 0, "The slope quantifies the expected change in the outcome per unit of the predictor."),
            q("Which target suits regression?", "Loan default: yes/no", "Monthly sales amount", "Email: spam/not spam", "Disease class", 1, "Monthly sales is a continuous numeric outcome.")
        )
        "Classification" -> listOf(
            q("Which problem is classification?", "Predicting temperature", "Predicting spam or not spam", "Estimating house price", "Forecasting revenue", 1, "Classification predicts a discrete class label."),
            q("A false positive means…", "Predicted positive, actually negative", "Predicted negative, actually positive", "Correctly predicted positive", "Correctly predicted negative", 0, "A false positive is a positive prediction for a negative case."),
            q("Which model can create a separating boundary between classes?", "Support Vector Machine", "Mean imputation", "Min-max scaler", "CSV reader", 0, "An SVM finds a decision boundary with a large margin."),
            q("For a balanced test set, accuracy measures…", "Correct predictions divided by all predictions", "Only true positives", "False negatives only", "Training time", 0, "Accuracy is the proportion of all predictions that are correct.")
        )
        "Evaluation Metrics" -> listOf(
            q("When missing a disease case is costly, prioritise…", "Recall", "File size", "Precision only", "Training rows", 0, "Recall measures how many actual positive cases were found."),
            q("Precision is TP divided by…", "TP + FP", "TP + FN", "TN + FP", "All rows only", 0, "Precision measures how often positive predictions are correct."),
            q("The F1-score balances…", "Precision and recall", "Rows and columns", "Training and storage", "Bias and filename", 0, "F1 is the harmonic mean of precision and recall."),
            q("A confusion matrix compares…", "Actual and predicted classes", "Two file formats", "Only training time", "Feature names only", 0, "It counts correct and incorrect predictions for each class.")
        )
        "Artificial Neural Networks" -> listOf(
            q("What does an artificial neuron calculate first?", "A weighted sum plus bias", "A confusion matrix", "A file path", "A database migration", 0, "A neuron combines its inputs with weights and bias before activation."),
            q("Why are nonlinear activation functions important?", "They let networks learn complex relationships", "They rename columns", "They guarantee perfect accuracy", "They remove all data", 0, "Without nonlinearity, stacked layers collapse to a linear transformation."),
            q("Which is an activation function?", "ReLU", "CSV", "SQL", "Room", 0, "ReLU is a commonly used nonlinear activation function."),
            q("Why might a simpler model beat an ANN on a small dataset?", "It can generalise with less data", "ANNs cannot use numbers", "Simple models need no input", "ANNs only draw images", 0, "Large neural networks can overfit when training data is limited.")
        )
        else -> emptyList()
    }

    private fun q(prompt: String, a: String, b: String, c: String, d: String, answer: Int, explanation: String) =
        QuestionSeed(prompt, listOf(a, b, c, d), answer, explanation)
}

object QuizScoring {
    fun stars(score: Int, total: Int): Int {
        if (total <= 0) return 0
        val ratio = score.toFloat() / total
        return when {
            ratio >= .9f -> 3
            ratio >= .7f -> 2
            ratio >= .5f -> 1
            else -> 0
        }
    }

    fun masteryLabel(score: Int, total: Int): String = when (stars(score, total)) {
        3 -> "Mastered"
        2 -> "Strong"
        1 -> "Keep practising"
        else -> "Review this chapter"
    }
}
