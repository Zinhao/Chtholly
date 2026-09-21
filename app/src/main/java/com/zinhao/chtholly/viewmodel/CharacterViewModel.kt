package com.zinhao.chtholly.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.entity.AICharacter
import com.zinhao.chtholly.utils.AsyncHelper

class CharacterViewModel(application: Application) : AndroidViewModel(application) {

    private val _characterList = MutableLiveData<List<AICharacter>>()
    val characterList: LiveData<List<AICharacter>> = _characterList

    fun loadCharacters() {
        BotApp.getInstance().loadAICharacter { result ->
            _characterList.postValue(result)
        }
    }

    fun addCharacter(character: AICharacter) {
        BotApp.getInstance().insert(character, Runnable {
            loadCharacters()
        })
    }

    fun updateCharacter(character: AICharacter) {
        AsyncHelper.doAsyncPart {
            BotApp.getInstance().characterDao.update(character)
            loadCharacters()
        }
    }

    fun deleteCharacter(character: AICharacter) {
        BotApp.getInstance().delete(character)
        _characterList.value?.let { current ->
            _characterList.postValue(current.filter { it.id != character.id })
        }
    }

    fun switchCharacter(character: AICharacter) {
        BotApp.getInstance().switchAISoul(character)
    }
}
