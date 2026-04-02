package com.zinhao.chtholly.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.zinhao.chtholly.databinding.FragmentSetupServerBinding
import com.zinhao.chtholly.viewmodel.SetupViewModel

class SetupServerFragment : Fragment() {

    private var _binding: FragmentSetupServerBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SetupViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupServerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[SetupViewModel::class.java]

        setupPresetDropdown()
        setupInputListeners()
        setupObservers()
    }

    private fun setupPresetDropdown() {
        val presets = listOf("OpenAI", "Gemini", "自定义")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, presets)
        binding.presetDropdown.setAdapter(adapter)
        binding.presetDropdown.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> viewModel.applyPresetConfig(SetupViewModel.ServerPreset.OpenAI)
                1 -> viewModel.applyPresetConfig(SetupViewModel.ServerPreset.Gemini)
                2 -> viewModel.applyPresetConfig(SetupViewModel.ServerPreset.Custom)
            }
        }
        viewModel.applyPresetConfig(SetupViewModel.ServerPreset.Custom)
    }

    private fun setupInputListeners() {
        binding.etBaseUrl.addTextChangedListener { text ->
            viewModel.updateBaseUrl(text.toString())
        }

        binding.etApiKey.addTextChangedListener { text ->
            viewModel.updateApiKey(text.toString())
        }
    }

    private fun setupObservers() {
        viewModel.baseUrl.observe(viewLifecycleOwner) { url ->
            if (binding.etBaseUrl.text.toString() != url) {
                binding.etBaseUrl.setText(url)
            }
        }

        viewModel.apiKey.observe(viewLifecycleOwner) { key ->
            if (binding.etApiKey.text.toString() != key) {
                binding.etApiKey.setText(key)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}