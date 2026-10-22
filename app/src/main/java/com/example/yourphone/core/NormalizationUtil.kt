package com.example.yourphone.core

object NormalizationUtil {
    fun normalize(number: String?): String {
        if (number.isNullOrEmpty()) return ""
        // Keep only digits and '+'
        return number.replace(Regex("[^0-9+]"), "")
    }

    fun numbersMatch(number1: String?, number2: String?): Boolean {
        val clean1 = normalize(number1)
        val clean2 = normalize(number2)
        if (clean1.isEmpty() || clean2.isEmpty()) return false
        
        // Strip leading plus for digit comparison
        val digits1 = clean1.replace("+", "")
        val digits2 = clean2.replace("+", "")
        
        if (digits1 == digits2) return true
        
        // Suffix match (compare last 10 digits)
        val len1 = digits1.length
        val len2 = digits2.length
        if (len1 >= 10 && len2 >= 10) {
            return digits1.substring(len1 - 10) == digits2.substring(len2 - 10)
        }
        return false
    }
}
