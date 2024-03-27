package com.yjy.data.model.dto

import com.google.gson.annotations.SerializedName
import com.yjy.domain.model.Joke

data class JokeDto(@SerializedName("value") val content: String) {
    fun toJoke(): Joke = Joke(content)
}