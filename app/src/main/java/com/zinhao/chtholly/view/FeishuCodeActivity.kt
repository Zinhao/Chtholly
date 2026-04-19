package com.zinhao.chtholly.view

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.MainViewModel
import com.zinhao.chtholly.NekoChatService
import com.zinhao.chtholly.databinding.ActivityFeishuCodeBinding
import com.zinhao.chtholly.databinding.ActivityMainBinding

class FeishuCodeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeishuCodeBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeishuCodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btMain.setOnClickListener {
            if(NekoChatService.getInstance()==null){
                Toast.makeText(it.context,"先启动无障碍服务",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val r = NekoChatService.getInstance().feiShuApi.inputWaitCode(binding.etCode.text.toString())
            if(r){
                finish()
            }else{
                Toast.makeText(it.context,"验证码无效",Toast.LENGTH_SHORT).show()
            }
        }

    }
}