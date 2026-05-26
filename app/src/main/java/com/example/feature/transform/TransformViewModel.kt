package com.example.feature.transform

import androidx.lifecycle.ViewModel
import com.example.core.domain.Transformer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TransformViewModel : ViewModel() {

    private val _inputText = MutableStateFlow("Copy-paste any snippet, UPPERCASE.it_or MINIFY it!")
    val inputText = _inputText.asStateFlow()

    private val _outputText = MutableStateFlow("")
    val outputText = _outputText.asStateFlow()

    private val _selectedTransformer = MutableStateFlow<Transformer>(Transformer.UpperCase)
    val selectedTransformer = _selectedTransformer.asStateFlow()

    private val _regexReplacePattern = MutableStateFlow("""[._]""")
    val regexReplacePattern = _regexReplacePattern.asStateFlow()

    private val _regexReplaceValue = MutableStateFlow(" ")
    val regexReplaceValue = _regexReplaceValue.asStateFlow()

    init {
        runTransform()
    }

    fun setInputText(text: String) {
        _inputText.value = text
        runTransform()
    }

    fun selectTransformer(transformer: Transformer) {
        _selectedTransformer.value = transformer
        runTransform()
    }

    fun updateRegexReplaceParams(pattern: String, replacement: String) {
        _regexReplacePattern.value = pattern
        _regexReplaceValue.value = replacement
        runTransform()
    }

    fun runTransform() {
        val input = _inputText.value
        val transformer = _selectedTransformer.value
        
        if (input.isEmpty()) {
            _outputText.value = ""
            return
        }

        val result = if (transformer is Transformer.RegexReplace) {
            val customReplacer = Transformer.RegexReplace(_regexReplacePattern.value, _regexReplaceValue.value)
            customReplacer.apply(input)
        } else {
            transformer.apply(input)
        }
        _outputText.value = result
    }
}
