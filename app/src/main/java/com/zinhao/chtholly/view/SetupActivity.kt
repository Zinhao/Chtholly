package com.zinhao.chtholly.view

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.zinhao.chtholly.R
import com.zinhao.chtholly.databinding.ActivitySetupBinding
import com.zinhao.chtholly.utils.HostConsts
import com.zinhao.chtholly.view.adapter.SetupPagerAdapter
import com.zinhao.chtholly.viewmodel.SetupViewModel

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private lateinit var viewModel: SetupViewModel
    private lateinit var pagerAdapter: SetupPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[SetupViewModel::class.java]

        setupViewPager()
        setupObservers()
        setupButtons()
    }

    private fun setupViewPager() {
        pagerAdapter = SetupPagerAdapter(this)
        binding.viewPager.apply {
            adapter = pagerAdapter
            isUserInputEnabled = false // 禁止滑动，只能通过按钮导航
            offscreenPageLimit = 3
        }

        // 监听页面变化
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.setCurrentStep(position)
                updateStepIndicator(position)
            }
        })
    }

    private fun setupObservers() {
        // 观察当前步骤，同步 ViewPager
        viewModel.currentStep.observe(this) { step ->
            if (binding.viewPager.currentItem != step) {
                binding.viewPager.currentItem = step
            }
            updateStepIndicator(step)
        }

        // 观察是否可以下一步
        viewModel.canGoNext.observe(this) { canGo ->
            binding.btnNext.isEnabled = canGo
            binding.btnNext.alpha = if (canGo) 1.0f else 0.5f
        }

        // 观察设置完成
        viewModel.setupComplete.observe(this) { complete ->
            if (complete) {
                startMainActivity()
            }
        }

        // 观察错误信息
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                // 显示错误提示
                viewModel.consumeError()
            }
        }
    }

    private fun setupButtons() {
        binding.btnNext.setOnClickListener {
            viewModel.goToNextStep()
        }

        binding.btnPrevious.setOnClickListener {
            viewModel.goToPreviousStep()
        }

        binding.btnSkip.setOnClickListener {
            // 可选：跳过向导使用默认配置
            finishSetupWithDefaults()
        }
    }

    private fun updateStepIndicator(currentStep: Int) {
        val stepTexts = listOf("服务器", "语音", "角色", "完成")
        binding.stepIndicator.text = "${currentStep + 1}/${stepTexts.size} ${stepTexts[currentStep]}"

        // 更新按钮文字
        binding.btnPrevious.isEnabled = currentStep > 0
        binding.btnNext.text = if (currentStep == 3) "进入应用" else "下一步"

        // 进度条
        binding.progressBar.progress = ((currentStep + 1) * 100 / 4)
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun finishSetupWithDefaults() {
        // 使用默认配置完成设置
        if(viewModel.currentStep.value == 0){
            viewModel.apply {
                updateBaseUrl(HostConsts.GEMINI_PROXY_API_HOST)
                updateApiKey("sk-default")
            }
        }else if(viewModel.currentStep.value == 1){
            viewModel.apply {
                updateTtsServerUrl(HostConsts.LOCAL_HOST)
            }
        }else if(viewModel.currentStep.value == 2){
            viewModel.apply {
                updateAdminName("主人")
                updateBotName("助手")
                updateBotDescription(getString(R.string.chara_default))
            }
        }
        viewModel.goToNextStep()
    }

    override fun onBackPressed() {
        val currentStep = viewModel.currentStep.value ?: 0
        if (currentStep > 0) {
            viewModel.goToPreviousStep()
        } else {
            super.onBackPressed() // 在第一步时退出应用
        }
    }
}