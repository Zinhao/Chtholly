package com.zinhao.chtholly.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.databinding.FragmentSetupAdminBinding
import com.zinhao.chtholly.databinding.FragmentSetupSoulBinding
import com.zinhao.chtholly.databinding.FragmentSetupSuccessBinding
import com.zinhao.chtholly.viewmodel.SetupViewModel

class SetupAdminFragment: Fragment() {
    private var _binding: FragmentSetupAdminBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SetupViewModel
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetupAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[SetupViewModel::class.java]
        binding.etAdmin.setText(BotApp.getInstance().adminName)
        binding.etAdmin.addTextChangedListener(
            afterTextChanged = {
                viewModel.updateAdminName(it?.trim().toString())
            }
        )
    }
}