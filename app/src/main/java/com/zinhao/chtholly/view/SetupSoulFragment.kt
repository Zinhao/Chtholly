package com.zinhao.chtholly.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.zinhao.chtholly.databinding.FragmentSetupSoulBinding
import com.zinhao.chtholly.databinding.FragmentSetupTtsBinding
import com.zinhao.chtholly.entity.AICharacter
import com.zinhao.chtholly.viewmodel.SetupViewModel

class SetupSoulFragment : Fragment() {
    private var _binding: FragmentSetupSoulBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SetupViewModel

    private val soulPresets = listOf(
        AICharacter("系统默认", "只是一个无感情的机器人，执行任何给出的指令", ),
        AICharacter("糖宝", "可爱风格聊天机器人", ),
        AICharacter("冰", "说话凌厉绝不拖泥带水，冷酷风格的白色头发仿生人", ),
        AICharacter("丛雨", "丛雨，说语间带有浓厚古人腔调，常以本座自称，有小孩的一面也有大人的一面。", ),
        AICharacter("自定义", "无限可能", ),
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupSoulBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[SetupViewModel::class.java]
        setupSoulDropdown()
    }

    fun setupSoulDropdown(){
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            soulPresets.map { it.name }
        )
        binding.presetSoulDropdown.setAdapter(adapter)

        binding.presetSoulDropdown.setOnItemClickListener { _, _, position, _ ->
            val preset = soulPresets[position]
            if(position == soulPresets.size-1){
                binding.soulInputDescLayout.visibility = View.VISIBLE
                binding.etSoulDesc.setText(preset.desc)
            }else{
                binding.soulInputDescLayout.visibility = View.GONE
                if (preset.desc.isNotEmpty()) {
                    binding.tvSoulDesc.text = preset.desc
                    viewModel.updateBotDescription(preset.desc)
                }
            }

        }
    }

}