package com.zinhao.chtholly.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.zinhao.chtholly.databinding.FragmentSetupTtsBinding
import com.zinhao.chtholly.viewmodel.SetupViewModel

class SetupTtsFragment : Fragment() {

    private var _binding: FragmentSetupTtsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SetupViewModel

    // 预设 TTS 服务器
    private val ttsPresets = listOf(
        TtsPreset("系统默认", "", ""),
        TtsPreset("Edge TTS", "http://localhost:5000", "zh-CN-XiaoxiaoNeural"),
        TtsPreset("GPT-SoVITS", "http://localhost:9880", ""),
        TtsPreset("Bert-VITS2", "http://localhost:5000", ""),
        TtsPreset("自定义", "", "")
    )

    // 预设语音列表
    private val voicePresets = listOf(
        "zh-CN-XiaoxiaoNeural",  // 晓晓 女声
    )

    data class TtsPreset(
        val name: String,
        val url: String,
        val defaultVoice: String
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupTtsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[SetupViewModel::class.java]

        setupPresetDropdown()
        setupVoiceDropdown()
        setupInputListeners()
        setupObservers()
        setupTestButton()
    }

    private fun setupPresetDropdown() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            ttsPresets.map { it.name }
        )
        binding.presetTtsDropdown.setAdapter(adapter)

        binding.presetTtsDropdown.setOnItemClickListener { _, _, position, _ ->
            val preset = ttsPresets[position]

            // 应用预设
            if (preset.url.isNotEmpty()) {
                binding.etTtsUrl.setText(preset.url)
                viewModel.updateTtsServerUrl(preset.url)
            }

            if (preset.defaultVoice.isNotEmpty()) {
                binding.voiceDropdown.setText(preset.defaultVoice, false)
                viewModel.updateTtsVoiceId(preset.defaultVoice)
            }

            // 显示/隐藏高级选项
            binding.advancedOptions.isVisible = (preset.name == "自定义" || preset.name == "GPT-SoVITS")
        }
    }

    private fun setupVoiceDropdown() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            voicePresets
        )
        binding.voiceDropdown.setAdapter(adapter)

        binding.voiceDropdown.setOnItemClickListener { _, _, position, _ ->
            viewModel.updateTtsVoiceId(voicePresets[position])
        }
    }

    private fun setupInputListeners() {
        // TTS URL 输入
        binding.etTtsUrl.addTextChangedListener { text ->
            viewModel.updateTtsServerUrl(text.toString())
        }

        // 自定义语音 ID 输入
        binding.etCustomVoiceId.addTextChangedListener { text ->
            viewModel.updateTtsVoiceId(text.toString())
        }

        // 语速调节
        binding.sliderSpeed.addOnChangeListener { _, value, _ ->
            binding.tvSpeedValue.text = "${value}x"
        }

        // 音调调节
        binding.sliderPitch.addOnChangeListener { _, value, _ ->
            binding.tvPitchValue.text = "${value}x"
        }

        // 使用自定义 Voice ID
        binding.switchCustomVoice.setOnCheckedChangeListener { _, isChecked ->
            binding.voiceDropdownLayout.isVisible = !isChecked
            binding.customVoiceLayout.isVisible = isChecked

            if (isChecked) {
                viewModel.updateTtsVoiceId(binding.etCustomVoiceId.text.toString())
            } else {
                viewModel.updateTtsVoiceId(binding.voiceDropdown.text.toString())
            }
        }
    }

    private fun setupObservers() {
        viewModel.ttsServerUrl.observe(viewLifecycleOwner) { url ->
            if (binding.etTtsUrl.text.toString() != url) {
                binding.etTtsUrl.setText(url)
            }
        }

        viewModel.ttsVoiceId.observe(viewLifecycleOwner) { voiceId ->
            if (voicePresets.contains(voiceId)) {
                binding.voiceDropdown.setText(voiceId, false)
            } else if (voiceId.isNotEmpty()) {
                binding.switchCustomVoice.isChecked = true
                binding.etCustomVoiceId.setText(voiceId)
            }
        }
    }

    private fun setupTestButton() {
        binding.btnTestTts.setOnClickListener {
            testTtsConnection()
        }
    }

    private fun testTtsConnection() {
        val url = viewModel.ttsServerUrl.value ?: ""
        if (url.isEmpty()) {
            binding.tvTestResult.text = "请先输入 TTS 服务器地址"
            binding.tvTestResult.setTextColor(requireContext().getColor(android.R.color.holo_orange_dark))
            return
        }

        binding.btnTestTts.isEnabled = false
        binding.btnTestTts.text = "测试中..."
        binding.tvTestResult.text = "正在连接 TTS 服务器..."

        //todo  模拟测试（实际应发起网络请求）
        binding.root.postDelayed({
            binding.btnTestTts.isEnabled = true
            binding.btnTestTts.text = "测试连接"

            // 实际逻辑：根据测试结果更新 UI
            val success = true // 替换为实际测试结果
            if (success) {
                binding.tvTestResult.text = "✓ 连接成功"
                binding.tvTestResult.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
            } else {
                binding.tvTestResult.text = "✗ 连接失败，请检查地址"
                binding.tvTestResult.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
            }
        }, 1500)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}