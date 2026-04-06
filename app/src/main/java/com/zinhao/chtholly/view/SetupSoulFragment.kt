package com.zinhao.chtholly.view

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.databinding.FragmentSetupSoulBinding
import com.zinhao.chtholly.entity.AICharacter
import com.zinhao.chtholly.viewmodel.SetupViewModel

class SetupSoulFragment : Fragment() {
    private var _binding: FragmentSetupSoulBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SetupViewModel

    private var soulPresets: MutableList<AICharacter> = mutableListOf()

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

        setupObserve()

        binding.etAtName.setText(BotApp.getInstance().botName)
        binding.etAtName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.updateBotName(s.toString())
            }
        })

        binding.tvSoulDesc.text = BotApp.getInstance().aiSoul
        binding.etSoulDesc.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.updateBotDescription(s.toString())
            }
        })
    }

    fun setupObserve(){
        viewModel.botSoulList.observe(viewLifecycleOwner) {
            soulPresets.addAll(it)
            soulPresets.add(0, AICharacter("custom",""))
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                soulPresets.map { character -> character.name }
            )
            binding.presetSoulDropdown.setAdapter(adapter)
            binding.presetSoulDropdown.setOnItemClickListener { _, _, position, _ ->
                val preset = soulPresets[position]
                if(position == 0){
                    binding.soulInputDescLayout.visibility = View.VISIBLE
                    binding.etSoulDesc.setText(preset.desc)
                }else{
                    binding.soulInputDescLayout.visibility = View.GONE
                    if (preset.desc.isNotEmpty()) {
                        binding.tvSoulDesc.text = preset.desc
                        binding.etAtName.setText(preset.name)
                        viewModel.updateBotDescription(preset.desc)
                    }
                }
            }
        }
    }

}